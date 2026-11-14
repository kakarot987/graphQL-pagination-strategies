package com.portfolio.graphql.service;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.ActivityFilter;
import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.repository.ActivityItemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityItemRepository activityItemRepository;

    /**
     * Seek pagination using a descending, monotonically increasing ID.
     * Filters and cursor constraints are applied inside the database query.
     * The cursor represents insertion order, not the activity timestamp.
     */
    public ActivityConnection getActivityFeed(int first, String afterCursor, ActivityFilter filter) {
        if (first < 1 || first > 100) {
            throw new IllegalArgumentException("first must be between 1 and 100");
        }

        Long cursorId = afterCursor == null ? null : CursorUtil.decode(afterCursor, "activity");
        LocalDateTime from = parseTimestamp(filter == null ? null : filter.fromTimestamp(), "fromTimestamp");
        LocalDateTime to = parseTimestamp(filter == null ? null : filter.toTimestamp(), "toTimestamp");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("fromTimestamp must not be after toTimestamp");
        }

        Specification<Activity> filters = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.activityType() != null) {
                    predicates.add(cb.equal(root.get("activityType"), filter.activityType()));
                }
                if (filter.userId() != null) {
                    predicates.add(cb.equal(root.get("userId"), filter.userId()));
                }
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("timestamp"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Specification<Activity> seek = cursorId == null
            ? filters
            : filters.and((root, query, cb) -> cb.lessThan(root.<Long>get("id"), cursorId));

        // Retrieve one extra record to determine hasNextPage without slicing in memory.
        List<Activity> fetched = activityItemRepository.findAll(
            seek, PageRequest.of(0, first + 1, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();
        boolean hasNextPage = fetched.size() > first;
        List<Activity> items = hasNextPage ? fetched.subList(0, first) : fetched;

        List<ActivityEdge> edges = items.stream()
            .map(activity -> new ActivityEdge(
                activity,
                CursorUtil.encode("activity", activity.getId()),
                buildRelationshipNote(activity)
            ))
            .toList();

        ActivityPageInfo pageInfo = new ActivityPageInfo(
            hasNextPage,
            cursorId != null,
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor()
        );

        // totalCount is the size of the entire filtered set, independent of the cursor.
        long totalCount = activityItemRepository.count(filters);
        return new ActivityConnection(
            edges, items, pageInfo, totalCount, items.isEmpty(), buildFeedMeta()
        );
    }

    public Activity createActivity(Long userId,
                                   String username,
                                   Activity.ActivityType activityType,
                                   String description,
                                   String targetId,
                                   String targetName,
                                   String metadata) {
        Activity activity = Activity.builder()
            .userId(userId)
            .username(username)
            .activityType(activityType)
            .description(description)
            .targetId(targetId)
            .targetName(targetName)
            .timestamp(LocalDateTime.now())
            .metadata(metadata)
            .build();

        return activityItemRepository.save(activity);
    }

    private static LocalDateTime parseTimestamp(String value, String argument) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(argument + " must be an ISO-8601 local date-time", ex);
        }
    }

    private String buildRelationshipNote(Activity activity) {
        return switch (activity.getActivityType()) {
            case PURCHASE -> "Transaction completed";
            case REVIEW -> "Community contribution";
            case FOLLOW -> "Social connection established";
            case WISHLIST -> "Saved for later";
            case VIEW -> "Browsing session";
            case SHARE -> "Content amplification";
        };
    }

    private ActivityFeedMeta buildFeedMeta() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = activityItemRepository.countSince(oneHourAgo);
        String lastViewed = LocalDateTime.now().minusMinutes(15)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long total = activityItemRepository.count();
        return new ActivityFeedMeta(recentCount, lastViewed, total);
    }
}
