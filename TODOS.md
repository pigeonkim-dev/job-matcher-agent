# TODOS

엔지니어 리뷰에서 범위 밖으로 미룬 항목들. (2026-06-23)

## updateProfile 부분 수정 미지원
- **What:** `PUT /api/profile`가 모든 필드를 무조건 `set` → 일부 필드가 빠진 요청이 기존 값을 null로 덮어씀.
- **Why:** 현재 UI는 항상 전체 필드를 보내 문제없지만, 다른 호출자/부분 업데이트엔 위험.
- **어떻게:** null인 필드는 건너뛰거나, 명시적 부분 업데이트 DTO 사용.
- **연관:** `api/JobMatchingController.java` updateProfile.

## getResults 공고 N+1
- **What:** `getResults()`가 결과마다 `jobPostingRepository.findById`를 개별 호출(N+1). 피드백은 Map으로 잡았지만 공고는 아님.
- **Why:** 공고/결과가 많아지면 쿼리 수 증가.
- **어떻게:** 공고를 `findAllById`로 한 번에 조회해 Map으로. (Phase 11 아키텍처 정리에서)
- **연관:** `api/JobMatchingController.java` getResults.

## 피드백 unique 제약 — 기존 중복 데이터 정리
- **What:** `feedback_log.match_result_id`에 unique 제약 추가. `ddl-auto: update`는 **기존 중복 행이 있으면 제약을 조용히 추가하지 않음**.
- **조치(중요):** 기존 dev DB에서 피드백을 눌러봤다면 중복 행이 있을 수 있음. 적용 전 정리 필요:
  ```sql
  -- 공고당 가장 최근 1건만 남기고 삭제 (PostgreSQL)
  DELETE FROM feedback_log f USING feedback_log g
  WHERE f.match_result_id = g.match_result_id AND f.id < g.id;
  ```
  또는 dev라면 `TRUNCATE feedback_log;` 후 재시작. (테스트 DB는 매번 새로 만들어 무관)
