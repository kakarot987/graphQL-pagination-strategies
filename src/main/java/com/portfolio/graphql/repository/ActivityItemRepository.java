package com.portfolio.graphql.repository;

import com.portfolio.graphql.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityItemRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    // Cursor-based: fetch items after a given ID (cursor = base64(id))
    Page<Activity> findByIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    Page<Activity> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.timestamp > :since")
    long countSince(LocalDateTime since);

    long countByActivityType(Activity.ActivityType activityType);
}
