package com.portfolio.graphql.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private Integer orderCount;

    @Column(nullable = false)
    private Double totalSpent;

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING
    }
}
