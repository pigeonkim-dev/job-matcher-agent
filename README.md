# Job Matcher Agent

**12년차 백엔드 엔지니어의 정규직 재진입을 위한 AI 채용 매칭 에이전트**

## Why

11년의 C# 백엔드 경력 위에 Java/Spring 생태계로 도구를 확장하며, AI 시대의
백엔드 엔지니어로 다시 정렬하는 과정에서 만든 실용 도구입니다.

매일 채용 공고를 수집하여 이력서·경력·선호와 매칭 분석을 수행하고, 가장
적합한 공고를 일일 리포트로 전달합니다. 본인이 매일 실제로 사용하는
도구이자, Spring Boot + Claude API 통합 학습의 결과물입니다.

## Modules

- **UserProfile** — 이력서, 페이 기준, 선호/회피 카테고리 등 본인 분석 데이터
- **JobCrawler** — 원티드, 사람인, 잡알리오 등에서 공고 수집
- **MatchingEngine** — Claude API 기반 매칭 점수 + 우려사항 + 자소서 키워드 분석
- **DailyDigest** — 매일 아침 슬랙으로 일일 리포트 전송
- **FeedbackLoop** — 본인 피드백을 학습해 개인화 정확도 향상

## Tech Stack

- **Backend**: Spring Boot 4.0.6, Java 17, JPA/Hibernate
- **Database**: PostgreSQL 15
- **AI**: Anthropic Claude API (claude-sonnet-4-20250514)
- **Crawling**: Jsoup 1.17.2
- **Scheduling**: Spring Scheduler
- **Notification**: Slack Incoming Webhook

## Roadmap

| Phase | 목표 | 상태 |
|---|---|---|
| 1 | UserProfile 엔티티 + Claude API 연동 PoC | ✅ 완료 |
| 2 | 원티드 크롤링 + 매칭 엔진 MVP | ✅ 완료 |
| 3 | PostgreSQL 저장 + 슬랙 전송 + 자동 스케줄링 | ✅ 완료 |
| 4 | 피드백 루프 + README 정리 | ✅ 완료 |
| 5 | 매칭 정확도 개선 + 자소서 키워드 고도화 | 🔜 예정 |
| 6 | 문서화 + 데모 영상 + 회고 블로그 | ⬜ 예정 |

## Status

🟢 Phase 4 완료 — 2026.06  
일일 작업 기록은 Commits 참조.