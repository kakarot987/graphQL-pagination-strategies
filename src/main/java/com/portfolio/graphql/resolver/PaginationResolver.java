package com.portfolio.graphql.resolver;

import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.*;
import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.model.Product;
import com.portfolio.graphql.model.User;
import com.portfolio.graphql.service.ActivityService;
import com.portfolio.graphql.service.ProductService;
import com.portfolio.graphql.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaginationResolver {

    private final ProductService productService;
    private final ActivityService activityService;
    private final UserService userService;

    // ---- E-commerce: Offset-based ----

    @QueryMapping
    public ProductConnection products(
        @Argument Integer page,
        @Argument Integer size,
        @Argument ProductFilter filter,
        @Argument ProductSort sort
    ) {
        return productService.getProducts(
            page != null ? page : 0,
            size != null ? size : 10,
            filter,
            sort
        );
    }

    @QueryMapping
    public Product product(@Argument Long id) {
        return productService.getProduct(id).orElse(null);
    }

    // ---- Activity Feed: Cursor-based ----

    @QueryMapping
    public ActivityConnection activityFeed(
        @Argument Integer first,
        @Argument String after,
        @Argument ActivityFilter filter
    ) {
        return activityService.getActivityFeed(
            first != null ? first : 10,
            after,
            filter
        );
    }

    // ---- Admin Dashboard: Offset-based with metrics ----

    @QueryMapping
    public UserConnection adminUsers(
        @Argument Integer page,
        @Argument Integer size,
        @Argument UserFilter filter,
        @Argument UserSort sort
    ) {
        return userService.getAdminUsers(
            page != null ? page : 0,
            size != null ? size : 10,
            filter,
            sort
        );
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.getUser(id).orElse(null);
    }

    @QueryMapping
    public ActivityConnection activityFeedOffset(@Argument Integer page, @Argument Integer size) {
        return activityService.getActivityFeedOffset(page == null ? 0 : page, size == null ? 10 : size);
    }

    @QueryMapping
    public List<String> paginationStrategies() {
        return List.of(
            "CURSOR_BASED: Best for real-time feeds, infinite scroll, append-only data",
            "OFFSET_BASED: Best for search results, numbered pages, stable datasets"
        );
    }

    @MutationMapping
    public Activity createActivity(
        @Argument Long userId,
        @Argument String username,
        @Argument Activity.ActivityType activityType,
        @Argument String description,
        @Argument String targetId,
        @Argument String targetName,
        @Argument String metadata
    ) {
        return activityService.createActivity(userId, username, activityType, description, targetId, targetName, metadata);
    }
}
