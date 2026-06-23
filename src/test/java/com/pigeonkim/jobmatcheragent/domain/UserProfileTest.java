package com.pigeonkim.jobmatcheragent.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 키워드 분리 단위 테스트 — 쉼표 구분, 공백 trim, 빈 값 제외.
 */
class UserProfileTest {

    @Test
    void splits_keywords_by_comma_and_trims() {
        UserProfile p = new UserProfile();
        p.setSearchKeywords(" Spring Boot ,Java 백엔드,  서버 개발자 ");

        assertThat(p.getSearchKeywordList())
                .containsExactly("Spring Boot", "Java 백엔드", "서버 개발자");
    }

    @Test
    void empty_or_null_keywords_return_empty_list() {
        UserProfile p = new UserProfile();
        assertThat(p.getSearchKeywordList()).isEmpty();   // null

        p.setSearchKeywords("   ");
        assertThat(p.getSearchKeywordList()).isEmpty();   // 공백만
    }

    @Test
    void drops_blank_entries_between_commas() {
        UserProfile p = new UserProfile();
        p.setSearchKeywords("Java,,  ,Spring");

        assertThat(p.getSearchKeywordList()).containsExactly("Java", "Spring");
    }
}
