package com.portfolio.graphql.repository;

import com.portfolio.graphql.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    long countByStatus(User.UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countCreatedSince(LocalDateTime since);

    @Query("SELECT AVG(u.totalSpent / NULLIF(u.orderCount, 0)) FROM User u WHERE u.orderCount > 0")
    Double findAverageOrderValue();
}
