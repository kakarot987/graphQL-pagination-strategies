package com.portfolio.graphql.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(nullable = false)
    private String description;

    private String targetId;
    private String targetName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String metadata;

    public enum ActivityType {
        PURCHASE, REVIEW, WISHLIST, VIEW, SHARE, FOLLOW
    }
}
