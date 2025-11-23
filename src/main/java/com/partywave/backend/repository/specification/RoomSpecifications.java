package com.partywave.backend.repository.specification;

import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.Tag;
import jakarta.persistence.criteria.*;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for filtering Room entities.
 *
 * Provides type-safe, composable query predicates for room discovery and filtering.
 * Used in conjunction with JpaSpecificationExecutor in RoomRepository.
 *
 * Based on PROJECT_OVERVIEW.md section 2.3 - Room Discovery & Joining.
 */
public class RoomSpecifications {

    private RoomSpecifications() {
        // Private constructor to prevent instantiation (utility class)
    }

    /**
     * Specification to filter only public rooms.
     *
     * @return Specification that matches rooms where isPublic = true
     */
    public static Specification<Room> isPublic() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("isPublic"));
    }

    /**
     * Specification to filter rooms by any of the given tags (OR logic).
     *
     * Uses EXISTS subquery to avoid cartesian product issues with LEFT JOIN.
     * This ensures:
     * - Rooms without tags are excluded when tag filtering is applied
     * - No duplicate results from tag joins
     * - Proper pagination at database level
     *
     * @param tagNames List of normalized tag names (lowercase) to filter by
     * @return Specification that matches rooms having any of the specified tags,
     *         or always-true specification if tagNames is empty
     */
    public static Specification<Room> hasAnyTag(List<String> tagNames) {
        return (root, query, criteriaBuilder) -> {
            // If no tags specified, don't apply any filter (return always-true predicate)
            if (tagNames == null || tagNames.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            // Null check for query (should not happen in normal Criteria API usage, but satisfies static analysis)
            if (query == null) {
                return criteriaBuilder.conjunction();
            }

            // Use EXISTS subquery to check if room has any of the specified tags
            // This avoids LEFT JOIN issues and ensures correct pagination
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Room> subRoot = subquery.from(Room.class);
            Join<Room, Tag> tagJoin = subRoot.join("tags", JoinType.INNER);

            subquery
                .select(criteriaBuilder.literal(1L))
                .where(criteriaBuilder.equal(subRoot.get("id"), root.get("id")), criteriaBuilder.lower(tagJoin.get("name")).in(tagNames));

            return criteriaBuilder.exists(subquery);
        };
    }

    /**
     * Specification to search rooms by name or description (case-insensitive).
     *
     * Performs LIKE query with wildcards on both name and description fields.
     * Uses OR logic between the two fields.
     *
     * @param searchTerm Search term to match against name/description (will be lowercased)
     * @return Specification that matches rooms where name or description contains the search term,
     *         or always-true specification if searchTerm is empty/null
     */
    public static Specification<Room> searchByNameOrDescription(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            // If no search term, don't apply any filter
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }

            // Build LIKE pattern with wildcards
            String likePattern = "%" + searchTerm.toLowerCase() + "%";

            // Create OR predicate: name LIKE pattern OR description LIKE pattern
            Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern);
            Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);

            return criteriaBuilder.or(namePredicate, descriptionPredicate);
        };
    }

    /**
     * Convenience method to combine all public room discovery filters.
     *
     * Combines:
     * - isPublic = true
     * - Tag filtering (if provided)
     * - Search by name/description (if provided)
     *
     * @param tagNames List of normalized tag names to filter by (can be null/empty)
     * @param searchTerm Search term for name/description (can be null/empty)
     * @return Combined specification with all filters applied
     */
    public static Specification<Room> findPublicRooms(List<String> tagNames, String searchTerm) {
        return Specification.where(isPublic()).and(hasAnyTag(tagNames)).and(searchByNameOrDescription(searchTerm));
    }
}
