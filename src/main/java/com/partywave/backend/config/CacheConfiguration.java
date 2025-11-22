package com.partywave.backend.config;

import java.time.Duration;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.*;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;
    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.Cache.Ehcache ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, com.partywave.backend.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, com.partywave.backend.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, com.partywave.backend.domain.User.class.getName());
            createCache(cm, com.partywave.backend.domain.Authority.class.getName());
            createCache(cm, com.partywave.backend.domain.User.class.getName() + ".authorities");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName());
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".images");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".refreshTokens");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".memberships");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".receivedAccesses");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".grantedAccesses");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".createdInvitations");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".messages");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".castVotes");
            createCache(cm, com.partywave.backend.domain.AppUser.class.getName() + ".receivedVotes");
            createCache(cm, com.partywave.backend.domain.AppUserStats.class.getName());
            createCache(cm, com.partywave.backend.domain.AppUserImage.class.getName());
            createCache(cm, com.partywave.backend.domain.UserToken.class.getName());
            createCache(cm, com.partywave.backend.domain.RefreshToken.class.getName());
            createCache(cm, com.partywave.backend.domain.Room.class.getName());
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".members");
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".accesses");
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".invitations");
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".messages");
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".votes");
            createCache(cm, com.partywave.backend.domain.Room.class.getName() + ".tags");
            createCache(cm, com.partywave.backend.domain.Tag.class.getName());
            createCache(cm, com.partywave.backend.domain.Tag.class.getName() + ".rooms");
            createCache(cm, com.partywave.backend.domain.RoomMember.class.getName());
            createCache(cm, com.partywave.backend.domain.RoomAccess.class.getName());
            createCache(cm, com.partywave.backend.domain.RoomInvitation.class.getName());
            createCache(cm, com.partywave.backend.domain.ChatMessage.class.getName());
            createCache(cm, com.partywave.backend.domain.Vote.class.getName());
            // jhipster-needle-ehcache-add-entry
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
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
