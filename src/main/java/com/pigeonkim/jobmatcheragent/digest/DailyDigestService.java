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
import java.util.function.Function;

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

    /**
     * score 내림차순 상위 N개. null score는 제외하고, 정렬도 null-safe(nullsLast)하게
     * 명시해 의도를 드러낸다. (eng-review: B3 방어적 강화) 테스트 가능하도록 분리.
     */
    static List<MatchResult> selectTop(List<MatchResult> all, int limit) {
        return all.stream()
                .filter(r -> r.getScore() != null)
                .sorted(Comparator.comparing(MatchResult::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    /**
     * Slack 리포트 메시지를 만든다. 네트워크와 분리해 테스트 가능하도록 추출.
     *
     * @param lookup jobPostingId → JobPosting 조회 (없으면 빈 JobPosting)
     */
    static String buildMessage(List<MatchResult> top5, Function<Long, JobPosting> lookup) {
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
                JobPosting p = lookup.apply(r.getJobPostingId());

                msg.append(nums[i]).append(" *").append(p.getCompany()).append("*")
                        .append(" · ").append(p.getTitle()).append("\n")
                        .append("   점수: *").append(r.getScore()).append("점*")
                        .append(" | 🏷️ ").append(r.getMatchedKeywords()).append("\n")
                        .append("   📋 ").append(r.getSummary()).append("\n")
                        .append("   🔗 <").append(p.getUrl()).append("|공고 보기>\n\n");
            }
        }
        return msg.toString();
    }

    public void send() {
        List<MatchResult> top5 = selectTop(matchResultRepository.findAll(), 5);
        String message = buildMessage(top5,
                id -> jobPostingRepository.findById(id).orElseGet(JobPosting::new));

        slackClient.post()
                .uri(webhookUrl)
                .bodyValue(Map.of("text", message))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Slack 일일 리포트 전송 완료");
    }
}