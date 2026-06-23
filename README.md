# Job Matcher Agent

**12년차 백엔드 엔지니어의 정규직 재진입을 위한 AI 채용 매칭 에이전트**

## Why

11년의 C# 백엔드 경력 위에 Java/Spring 생태계로 도구를 확장하며, AI 시대의
백엔드 엔지니어로 다시 시작하는 과정에서 만든 실용 도구입니다.

매일 채용 공고를 자동 수집하여 이력서·경력·선호와 매칭 분석을 수행하고,
가장 적합한 공고를 매일 아침 슬랙으로 전달합니다.
본인이 매일 실제로 사용하는 도구이자, Spring Boot + Claude API 통합 학습의 결과물입니다.

## Features

- 채용 공고 자동 수집 — 검색 키워드는 프로필에서 관리(하드코딩 제거)
- Claude API 기반 매칭 분석 — 기술/경력/선호도 항목별 점수 + 우려사항 + 자소서 키워드
- **structured outputs** — anthropic-java SDK로 응답을 타입 안전하게 파싱(프롬프트 이탈에도 안전)
- 매일 오전 8시 Slack 일일 리포트 자동 전송
- 피드백 루프 — 관심/관심없음/지원함 피드백을 학습해 개인화 정확도 향상, 화면에 현재 선택 표시
- 스마트 재분석 — 신규 공고 또는 프로필 변경 시에만 재분석
- 회복탄력성 — 외부 API 재시도(Resilience4j) + 공고별 실패 격리(1건 실패가 배치 전체를 멈추지 않음)

## Modules

- **UserProfile** — 이력서, 연봉 기준, 검색 키워드(검색+매칭 통합)·회피 키워드
- **JobCrawler** — 구인/구직사이트 크롤링 + 스케줄링 (매일 5회), 키워드/공고별 실패 격리
- **MatchingEngine / MatchAnalysisService** — Claude SDK structured outputs 분석 + 트랜잭션 경계 분리
- **DailyDigest** — 매일 오전 8시 Slack 자동 전송
- **FeedbackLoop** — 공고당 피드백 1개(upsert) 이력 기반 개인화

## Tech Stack

| 항목 | 상세 |
|---|---|
| Backend | Spring Boot 4.0.6, Java 17, JPA/Hibernate |
| Database | PostgreSQL 15 |
| AI | Anthropic Claude API (`claude-sonnet-4-6`) · anthropic-java SDK (structured outputs) |
| Resilience | Resilience4j (retry) + 실패 격리 |
| Crawling | Jsoup 1.17.2, Wanted API |
| Scheduling | Spring Scheduler |
| Notification | Slack Incoming Webhook |
| Test | JUnit 5, Mockito, Testcontainers(PostgreSQL), WireMock, JaCoCo |
| Build | Gradle - Groovy |

## Getting Started

```bash
# 환경변수 설정
export MY_ANTHROPIC_API_KEY=your_api_key
export SLACK_WEBHOOK_URL=your_webhook_url

# 실행
./gradlew bootRun
```

## API

| Method | URL | 설명 |
|---|---|---|
| GET | /api/profile | 프로필 조회 |
| PUT | /api/profile | 프로필 수정 |
| GET | /api/postings | 수집된 공고 목록 |
| POST | /api/crawl | 공고 수집 실행 (프로필 검색 키워드 사용) |
| POST | /api/analyze | AI 분석 실행 (스마트 재분석) |
| GET | /api/results | 분석 결과 조회 (현재 피드백 포함) |
| POST | /api/feedback/{matchResultId} | 피드백 저장 (공고당 1개, upsert) |
| GET | /api/feedback | 타입별 피드백 조회 |

## Roadmap

| Phase | 목표                                  | 상태 |
|---|-------------------------------------|---|
| 1 | UserProfile 엔티티 + Claude API 연동 PoC | ✅ 완료 |
| 2 | 구인구직 사이트 크롤링 + 매칭 엔진 MVP            | ✅ 완료 |
| 3 | PostgreSQL 저장 + Slack 전송 + 자동 스케줄링  | ✅ 완료 |
| 4 | 피드백 루프 + 패키지 구조 정리                  | ✅ 완료 |
| 5 | 매칭 정확도 개선 + 자소서 키워드 고도화             | ✅ 완료 |
| 6 | 문서화 + 스크린샷 + 회고 블로그                 | ✅ 완료 |
| 7 | 신뢰성 — 트랜잭션 경계·SDK structured outputs·DTO·테스트(JaCoCo) | ✅ 완료 |
| 8 | 회복탄력성 — Resilience4j 재시도 + 공고별 실패 격리 | ✅ 완료 |
| 9 | 프로필 검색 키워드 통합 + 피드백 상태 표시(upsert)   | ✅ 완료 |

## 품질

- 테스트 37개 (단위 + `@WebMvcTest` 슬라이스 + WireMock + Testcontainers PostgreSQL 통합 + 결정적 스모크)
- JaCoCo 브랜치 커버리지: matching/digest/crawler 핵심 70%+
- `./gradlew test` 로 전체 검증 (Testcontainers는 Docker 필요)

## Screenshots

### 분석 결과 화면
![분석 결과](docs/screenshots/results.png)

### Slack 일일 리포트
![Slack 리포트](docs/screenshots/slack.png)