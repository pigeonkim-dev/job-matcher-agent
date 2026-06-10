# Job Matcher Agent

**12년차 백엔드 엔지니어의 정규직 재진입을 위한 AI 채용 매칭 에이전트**

## Why

11년의 C# 백엔드 경력 위에 Java/Spring 생태계로 도구를 확장하며, AI 시대의
백엔드 엔지니어로 다시 정렬하는 과정에서 만든 실용 도구입니다.

매일 채용 공고를 자동 수집하여 이력서·경력·선호와 매칭 분석을 수행하고,
가장 적합한 공고를 매일 아침 슬랙으로 전달합니다.
본인이 매일 실제로 사용하는 도구이자, Spring Boot + Claude API 통합 학습의 결과물입니다.

## Features

- 채용 공고 자동 수집 (Wanted API)
- Claude API 기반 매칭 분석 — 기술/경력/선호도 항목별 점수 + 우려사항 + 자소서 키워드
- 매일 오전 8시 Slack 일일 리포트 자동 전송
- 피드백 루프 — 관심/비관심 피드백을 학습해 개인화 정확도 향상
- 스마트 재분석 — 신규 공고 또는 프로필 변경 시에만 재분석

## Modules

- **UserProfile** — 이력서, 연봉 기준, 선호/회피 카테고리
- **JobCrawler** — 구인 구직 사이트 + 스케줄링 (매일 5회)
- **MatchingEngine** — Claude API 매칭 분석 (항목별 점수 + 자소서 키워드)
- **DailyDigest** — 매일 오전 8시 Slack 자동 전송
- **FeedbackLoop** — 피드백 이력 기반 개인화 정확도 향상

## Tech Stack

| 항목 | 상세 |
|---|---|
| Backend | Spring Boot 4.0.6, Java 17, JPA/Hibernate |
| Database | PostgreSQL 15 |
| AI | Anthropic Claude API (claude-sonnet-4-20250514) |
| Crawling | Jsoup 1.17.2, Wanted API |
| Scheduling | Spring Scheduler |
| Notification | Slack Incoming Webhook |
| Build | Gradle - Groovy |

## Getting Started

```bash