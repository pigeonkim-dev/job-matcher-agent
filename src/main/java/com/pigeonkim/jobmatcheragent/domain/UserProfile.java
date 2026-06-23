package com.pigeonkim.jobmatcheragent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    private String jobTitle;  // 희망 직무

    // 크롤 검색어 겸 매칭 선호 신호 (쉼표로 구분). 예: "Spring Boot, Java 백엔드, 서버 개발자"
    // 이전의 preferredCategories를 대체 (검색 + 매칭 통합)
    @Column(columnDefinition = "TEXT")
    private String searchKeywords;

    @Column(columnDefinition = "TEXT")
    private String avoidKeywords;

    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 검색 키워드를 쉼표 기준으로 나눠 리스트로 반환한다. (크롤러가 키워드별로 검색)
     * 빈 값/공백은 제외하고, searchKeywords가 비어 있으면 빈 리스트를 준다.
     */
    public List<String> getSearchKeywordList() {
        if (searchKeywords == null || searchKeywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(searchKeywords.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());
    }
}