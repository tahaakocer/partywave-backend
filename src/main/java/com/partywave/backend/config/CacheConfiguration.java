package com.partywave.backend.config;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import org.hibernate.cache.jcache.ConfigSettings;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * Redis key prefix for all PartyWave keys.
     * Used in service layer for constructing Redis keys.
     * Example: partywave:room:{roomId}:playlist:item:{playlistItemId}
     */
    public static final String KEY_PREFIX = "partywave:";

    private GitProperties gitProperties;
    private BuildProperties buildProperties;

    /**
     * Creates RedissonClient bean for Redis connection.
     * Supports both single server and cluster modes.
     * Based on REDIS_ARCHITECTURE.md specifications.
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(JHipsterProperties jHipsterProperties) {
        URI redisUri = URI.create(jHipsterProperties.getCache().getRedis().getServer()[0]);

        Config config = new Config();
        // Fix Hibernate lazy initialization https://github.com/jhipster/generator-jhipster/issues/22889
        config.setCodec(new org.redisson.codec.SerializationCodec());

        if (jHipsterProperties.getCache().getRedis().isCluster()) {
            ClusterServersConfig clusterServersConfig = config
                .useClusterServers()
                .setMasterConnectionPoolSize(jHipsterProperties.getCache().getRedis().getConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(jHipsterProperties.getCache().getRedis().getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(jHipsterProperties.getCache().getRedis().getSubscriptionConnectionPoolSize())
                .addNodeAddress(jHipsterProperties.getCache().getRedis().getServer());

            if (redisUri.getUserInfo() != null) {
                clusterServersConfig.setPassword(redisUri.getUserInfo().substring(redisUri.getUserInfo().indexOf(':') + 1));
            }
        } else {
            SingleServerConfig singleServerConfig = config
                .useSingleServer()
                .setConnectionPoolSize(jHipsterProperties.getCache().getRedis().getConnectionPoolSize())
                .setConnectionMinimumIdleSize(jHipsterProperties.getCache().getRedis().getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(jHipsterProperties.getCache().getRedis().getSubscriptionConnectionPoolSize())
                .setAddress(jHipsterProperties.getCache().getRedis().getServer()[0]);

            if (redisUri.getUserInfo() != null) {
                singleServerConfig.setPassword(redisUri.getUserInfo().substring(redisUri.getUserInfo().indexOf(':') + 1));
            }
        }

        return Redisson.create(config);
    }

    /**
     * Creates RedisConnectionFactory bean using Redisson.
     * This factory is used by RedisTemplate and other Redis operations.
     * Based on REDIS_ARCHITECTURE.md specifications.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    /**
     * Creates RedisTemplate with 'partywave:' key prefix.
     * Used for all Redis operations in the application.
     *
     * Key structure: partywave:{key}
     * - Playlist items: partywave:room:{roomId}:playlist:item:{playlistItemId}
     * - Like/Dislike sets: partywave:room:{roomId}:playlist:item:{playlistItemId}:likes
     * - Room playlist: partywave:room:{roomId}:playlist
     * - Playback state: partywave:room:{roomId}:playback
     * - Online members: partywave:room:{roomId}:members:online
     *
     * Based on REDIS_ARCHITECTURE.md specifications.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Use StringRedisSerializer for keys
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Use GenericJackson2JsonRedisSerializer for values
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * JCache configuration for Hibernate second-level cache.
     * Uses Redisson with configured expiration time.
     */
    @Bean
    public javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration(
        JHipsterProperties jHipsterProperties,
        RedissonClient redissonClient
    ) {
        MutableConfiguration<Object, Object> jcacheConfig = new MutableConfiguration<>();
        jcacheConfig.setStatisticsEnabled(true);
        jcacheConfig.setExpiryPolicyFactory(
            CreatedExpiryPolicy.factoryOf(
                new javax.cache.expiry.Duration(TimeUnit.SECONDS, jHipsterProperties.getCache().getRedis().getExpiration())
            )
        );
        return RedissonConfiguration.fromInstance(redissonClient, jcacheConfig);
    }

    /**
     * Hibernate properties customizer to integrate JCache with Hibernate second-level cache.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    /**
     * JCache Manager customizer for Hibernate entity caching.
     * Only essential caches are created here.
     * Additional caches can be added as needed via jhipster-needle-redis-add-entry.
     */
    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer(javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration) {
        return cm -> {
            // User-related caches
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName(), jcacheConfiguration);
            createCache(cm, com.partywave.backend.domain.AppUserStats.class.getName(), jcacheConfiguration);
            createCache(cm, com.partywave.backend.domain.RefreshToken.class.getName(), jcacheConfiguration);

            // Room-related caches
            createCache(cm, com.partywave.backend.domain.Room.class.getName(), jcacheConfiguration);
            createCache(cm, com.partywave.backend.domain.RoomMember.class.getName(), jcacheConfiguration);
            // jhipster-needle-redis-add-entry
        };
    }

    private void createCache(
        javax.cache.CacheManager cm,
        String cacheName,
        javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration
    ) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
