package com.portfolio.graphql.service;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.*;
import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.repository.ActivityItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityItemRepository activityItemRepository;

    /**
     * Cursor-based pagination — ideal for real-time activity feeds.
     *
     * Why cursor over offset here:
     * - New activities are constantly appended
     * - Offset pagination would cause skipped/duplicate items when new items arrive
     * - Cursor is stable: "give me items before ID=X" is deterministic
     *
     * Cursor strategy: cursor = base64("cursor:activity:ID")
     * We fetch items with ID < cursor_id, newest first
     */
    public ActivityConnection getActivityFeed(int first, String afterCursor, ActivityFilter filter) {
        log.info("Fetching activity feed: first={}, after={}", first, afterCursor);

        // Fetch one extra to determine hasNextPage
        PageRequest pageable = PageRequest.of(0, first + 1, Sort.by(Sort.Direction.DESC, "id"));

        Page<Activity> page;
        if (afterCursor != null && CursorUtil.isValid(afterCursor)) {
            Long cursorId = CursorUtil.decode(afterCursor);
            page = activityItemRepository.findByIdLessThanOrderByIdDesc(cursorId, pageable);
        } else {
            page = activityItemRepository.findAllByOrderByIdDesc(pageable);
        }

        List<Activity> items = page.getContent();
        boolean hasNextPage = items.size() > first;

        // Trim to requested size
        List<Activity> resultItems = hasNextPage ? items.subList(0, first) : items;

        List<ActivityEdge> edges = resultItems.stream()
            .map(a -> new ActivityEdge(
                a,
                CursorUtil.encode("activity", a.getId()),
                buildRelationshipNote(a)   // relationship metadata pattern
            ))
            .toList();

        ActivityPageInfo pageInfo = new ActivityPageInfo(
            hasNextPage,
            afterCursor != null,
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor()
        );

        // Feed-specific metadata
        ActivityFeedMeta feedMeta = buildFeedMeta();

        long totalCount = activityItemRepository.count();

        return new ActivityConnection(
            edges,
            resultItems,
            pageInfo,
            totalCount,
            resultItems.isEmpty(),
            feedMeta
        );
    }

    /**
     * Create and persist a new Activity. Used as a demo helper (mutation).
     */
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

    private String buildRelationshipNote(Activity activity) {
        // Demonstrates: relationship metadata pattern
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
