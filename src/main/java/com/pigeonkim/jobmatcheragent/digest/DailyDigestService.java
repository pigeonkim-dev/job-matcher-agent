package com.pigeonkim.jobmatcheragent.digest;

import com.pigeonkim.jobmatcheragent.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DailyDigestService {

    private static final Logger log = LoggerFactory.getLogger(DailyDigestService.class);

    private final MatchResultRepository matchResultRepository;
    private final JobPostingRepository jobPostingRepository;
    private final WebClient slackClient;

    public DailyDigestService(
            MatchResultRepository matchResultRepository,
            JobPostingRepository jobPostingRepository,
            @Value("${slack.webhook-url}") String webhookUrl) {
        this.matchResultRepository = matchResultRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.slackClient = WebClient.builder().build();
        this.webhookUrl = webhookUrl;
    }

    private final String webhookUrl;

    public void send() {
        // score 내림차순. null은 이미 필터로 제외하지만, 정렬 자체도 null-safe하게
        // 명시(Comparator.nullsLast)해 의도를 드러낸다. (eng-review: B3 방어적 강화)
        List<MatchResult> top5 = matchResultRepository.findAll().stream()
                .filter(r -> r.getScore() != null)
                .sorted(Comparator.comparing(MatchResult::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        StringBuilder msg = new StringBuilder();
        msg.append("🤖 *일일 채용 매칭 리포트* — ")
                .append(LocalDate.now())
                .append("\n\n");

        if (top5.isEmpty()) {
            msg.append("분석된 공고가 없습니다.");
        } else {
            msg.append("📊 *오늘의 TOP ").append(top5.size()).append(" 매칭 공고*\n\n");
            String[] nums = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣"};

            for (int i = 0; i < top5.size(); i++) {
                MatchResult r = top5.get(i);
                JobPosting p = jobPostingRepository.findById(r.getJobPostingId())
                        .orElse(new JobPosting());

                msg.append(nums[i]).append(" *").append(p.getCompany()).append("*")
                        .append(" · ").append(p.getTitle()).append("\n")
                        .append("   점수: *").append(r.getScore()).append("점*")
                        .append(" | 🏷️ ").append(r.getMatchedKeywords()).append("\n")
                        .append("   📋 ").append(r.getSummary()).append("\n")
                        .append("   🔗 <").append(p.getUrl()).append("|공고 보기>\n\n");
            }
        }

        slackClient.post()
                .uri(webhookUrl)
                .bodyValue(Map.of("text", msg.toString()))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Slack 일일 리포트 전송 완료");
    }
}