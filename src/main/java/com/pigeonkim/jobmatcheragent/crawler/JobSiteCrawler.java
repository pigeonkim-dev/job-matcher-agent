package com.pigeonkim.jobmatcheragent.crawler;

import com.pigeonkim.jobmatcheragent.domain.JobPosting;
import java.util.List;

public interface JobSiteCrawler {
    List<String> searchJobUrls(String keyword);  // 검색 → URL 목록
    JobPosting parseJobPosting(String url);       // 개별 공고 파싱
}