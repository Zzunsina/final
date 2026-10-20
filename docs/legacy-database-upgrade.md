# 기존 MariaDB 도입 오류 해결 (2026-10-20)

## 원인

DB 연결 자체는 성공했지만 원본 12개 테이블이 먼저 생성되어 있고
`flyway_schema_history`가 없어 자동 migration이 중단됐습니다.
기존 스키마는 현재 V1~V3와 달라 baseline-on-migrate만 켜는 것으로는 충분하지 않았습니다.

## 적용

- 기존 DB를 `.tools/backups/employment-before-flyway.sql`에 먼저 백업했습니다. Git 제외 경로입니다.
- 원본 테이블 12개를 메타데이터로 확인한 후 일회성 `adoptLegacyDatabase` 작업으로 baseline 3을 등록했습니다.
- V4 Java migration이 누락된 심사 컬럼, 연계 공개 안내, 면접 요청 키를 추가했습니다.
- 로그인 세션·비밀번호 재설정 토큰·지원 스냅샷 테이블, 인덱스·제약을 추가했습니다.
- 기존 문의의 WAITING 상태는 OPEN으로 정규화했습니다.
- 기존 면접에는 legacy-ID 요청 키를 부여했습니다.
- 기존 지원의 제출 당시 스냅샷을 임의로 복원하지 않습니다. 없는 기록은 화면에서 안내합니다.
- 테이블이나 기존 행을 삭제하지 않았습니다. 일반 실행에는 자동 baseline을 활성화하지 않았습니다.

이 작업은 검토한 원본 DB 도입용입니다. 다른 DB에 임의로 실행하지 마세요.
이미 적용된 DB는 IntelliJ에서 일반적으로 재실행하면 됩니다.

## 검증

실제 MariaDB 10.11에서 baseline, V4 적용과 Flyway validate가 성공했습니다.
실제 MariaDB 연결 상태에서 `Started EmploymentApplication`과 8080 서버 기동도 확인했습니다.
검증용 서버는 IntelliJ 재실행 시 포트가 겹치지 않도록 종료했습니다.
격리된 H2의 자동 테스트 6개도 통과했습니다.
사용자 application.properties가 테스트 DB 설정을 덮어쓰지 않도록 테스트용 properties도 추가했습니다.

## 실행 설정

사용자가 작성한 `application.properties`의 MariaDB 설정을 유지합니다.
JWT_SECRET은 IntelliJ 환경 변수에 32자 이상으로 설정해야 합니다.
다른 곳에서 서버가 실행 중이면 중지한 뒤 다시 실행하세요.

[Flyway baseline 동작 공식 문서](https://documentation.red-gate.com/flyway/reference/commands/baseline)
