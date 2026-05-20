package com.portfolio.graphql.service;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.*;
import com.portfolio.graphql.model.User;
import com.portfolio.graphql.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Offset-based pagination with admin-specific metadata.
     * Admin dashboards benefit from: total counts, jumping to any page, status breakdowns.
     */
    public UserConnection getAdminUsers(int page, int size, UserFilter filter, UserSort sort) {
        log.info("Fetching admin users: page={}, size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, buildSort(sort));
        Specification<User> spec = buildSpecification(filter);

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<User> users = userPage.getContent();

        List<UserEdge> edges = users.stream()
            .map(u -> new UserEdge(u, CursorUtil.encode("user", u.getId())))
            .toList();

        UserPageInfo pageInfo = new UserPageInfo(
            userPage.hasNext(),
            userPage.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor(),
            userPage.getTotalPages(),
            page
        );

        AdminUserMetrics metrics = buildAdminMetrics();

        return new UserConnection(
            edges,
            users,
            pageInfo,
            userPage.getTotalElements(),
            users.isEmpty(),
            metrics
        );
    }

    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }

    private Sort buildSort(UserSort sort) {
        if (sort == null) return Sort.by(Sort.Direction.ASC, "username");
        String field = switch (sort.field()) {
            case USERNAME -> "username";
            case CREATED_AT -> "createdAt";
            case LAST_LOGIN -> "lastLoginAt";
            case ORDER_COUNT -> "orderCount";
            case TOTAL_SPENT -> "totalSpent";
        };
        return sort.direction() == SortDirection.ASC
            ? Sort.by(Sort.Direction.ASC, field)
            : Sort.by(Sort.Direction.DESC, field);
    }

    private Specification<User> buildSpecification(UserFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.role() != null && !filter.role().isBlank()) {
                predicates.add(cb.equal(root.get("role"), filter.role()));
            }
            if (filter.searchTerm() != null && !filter.searchTerm().isBlank()) {
                String pattern = "%" + filter.searchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminUserMetrics buildAdminMetrics() {
        long active = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactive = userRepository.countByStatus(User.UserStatus.INACTIVE);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long newThisWeek = userRepository.countCreatedSince(weekAgo);
        Double avgOrderValue = userRepository.findAverageOrderValue();
        return new AdminUserMetrics(
            active,
            inactive,
            newThisWeek,
            avgOrderValue != null ? avgOrderValue : 0.0
        );
    }
}
