package com.pigeonkim.jobmatcheragent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String resumeContent;

    private Integer payFloor;

    private Integer payTarget;

    @Column(columnDefinition = "TEXT")
    private String preferredCategories;

    @Column(columnDefinition = "TEXT")
    private String avoidKeywords;
}