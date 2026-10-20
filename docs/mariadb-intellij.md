# 직접 설치한 MariaDB와 IntelliJ 연결

이 PC에서 확인된 설치 경로는 `C:\Program Files\MariaDB 10.11`입니다.
MariaDB용 JDBC 드라이버와 `mariadb` 프로필을 추가했습니다.
DB 서버의 실제 버전은 접속한 SQL 창에서 `SELECT VERSION();`으로 확인하세요.

## 1. 기존 DB 확인

관리자 권한으로 접속한 HeidiSQL 등의 쿼리 창에서 실행합니다.

```sql
SELECT VERSION();
SHOW TABLES FROM employment_db;
```

`employment_db`가 이미 존재하면 다시 생성하거나 삭제하지 않습니다.
이 프로젝트의 최초 Flyway 실행은 빈 DB를 전제로 합니다.
테이블이 이미 있고 `flyway_schema_history`가 없다면 자동 baseline이나 초기화를 하지 말고,
기존 테이블과 migration을 먼저 비교해야 합니다.

## 2. 애플리케이션 전용 계정 생성

아래의 `직접_정한_비밀번호`를 본인이 정한 비밀번호로 바꾼 뒤 실행합니다.
작은따옴표가 포함된 비밀번호는 SQL 문자열에 맞게 이스케이프해야 합니다.
기존 root 계정의 인증 방식은 변경하지 않습니다.

```sql
CREATE USER 'employment_app'@'localhost'
    IDENTIFIED VIA mysql_native_password
    USING PASSWORD('직접_정한_비밀번호');

GRANT ALL PRIVILEGES ON employment_db.*
    TO 'employment_app'@'localhost';
```

계정이 이미 있다는 오류가 나오면 기존 계정을 덮어쓰지 마세요.
기존 계정의 비밀번호를 사용하거나 별도 계정명을 정하고 DB_USERNAME에 반영합니다.
권한은 이 프로젝트 DB로 한정하며 Flyway의 테이블·인덱스·제약 생성에 사용합니다.
GRANT 후 FLUSH PRIVILEGES는 필요하지 않습니다.

## 3. IntelliJ 실행 설정

1. Gradle 변경 사항을 새로고침합니다.
2. Run → Edit Configurations에서 EmploymentApplication 실행 설정을 엽니다.
3. Program arguments: `--spring.profiles.active=mariadb`
4. Working directory: `C:\JobBridge\employment-backend`
5. Environment variables에 다음 값을 지정합니다.

```text
DB_USERNAME=employment_app
DB_PASSWORD=위에서 직접 정한 비밀번호
JWT_SECRET=32자 이상의 임의 문자열
```

JWT_SECRET 생성은 PowerShell에서 다음 명령으로 할 수 있습니다.
생성된 값을 로컬 실행 설정에만 저장하고 채팅이나 Git에 공유하지 마세요.

```powershell
$taskSecretBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($taskSecretBytes)
[Convert]::ToBase64String($taskSecretBytes)
```

환경 변수 DB_URL이 기존에 등록되어 있다면 지우거나
`jdbc:mariadb://localhost:3306/employment_db`로 바꿉니다.
DB 포트가 3306이 아니라면 DB_URL의 포트도 맞춥니다.

`JobBridge Backend Dev`는 H2 실행 설정이므로 이 단계에서는 사용하지 않습니다.
기존 서버를 중지한 뒤 실행하세요. 정상 로그에는 활성 프로필 `mariadb`와 `Started EmploymentApplication`이 표시됩니다.
샘플 관리자는 이 프로필에서 자동 생성되지 않습니다.

## 관련 공식 문서

- [MariaDB Connector/J](https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j)
- [MariaDB mysql_native_password 인증](https://mariadb.com/docs/server/reference/plugins/authentication-plugins/authentication-plugin-mysql_native_password)
