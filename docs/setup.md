# 실행 환경

## 고정 버전

- JDK 17 (이 환경의 확인 버전 17.0.19), Spring Boot 3.5.16, Gradle Wrapper 8.14.3.
- Node.js 확인 버전 24.19.0, React 19.2.0, Vite 7.1.10.
- MySQL 운영 대상 8.4, 개발·테스트 H2 2.3.232.
- 프런트 패키지의 정확한 의존성은 `package.json`과 `package-lock.json`에 고정합니다.

호환성 확인 자료:

- [Spring Boot 3.5 환경 요구 사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Vite 환경 요구 사항](https://vite.dev/guide/)
- [React 19.2](https://react.dev/blog/2025/10/01/react-19-2)

## 일반 개발 명령

```powershell
cd C:\JobBridge\employment-backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
$env:DEV_SEED_PASSWORD = 'JobBridge123!'
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

```powershell
cd C:\JobBridge\employment-frontend
npm ci
npm run dev
```

의존성은 npm과 `package-lock.json`으로 관리합니다. 설치된 상태에서는 `npm run dev`만 실행합니다.
프런트는 `http://localhost:80`, 백엔드는 `http://localhost:8080`을 사용합니다.

## MySQL

이 PC에 설치된 MariaDB를 직접 사용하는 경우에는
[MariaDB·IntelliJ 연결 안내](mariadb-intellij.md)를 따릅니다.
`mariadb` 프로필과 MariaDB Connector/J를 사용하며 `dev` H2 실행과 구분합니다.

관리자가 빈 개발용 DB와 전용 계정을 먼저 만듭니다. 기존 DB를 초기화하는 명령은 제공하지 않습니다.

```sql
CREATE DATABASE employment_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

환경 변수는 PowerShell 또는 IDE Run Configuration에 넣습니다.
`.env.example`을 복사하는 것만으로 Spring Boot에 적용되지 않습니다.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/employment_db?connectionTimeZone=UTC'
$env:DB_USERNAME = 'employment'
$env:DB_PASSWORD = '<설정한 DB 비밀번호>'
$env:JWT_SECRET = '<32자 이상의 임의 비밀키>'
$env:SECURE_COOKIE = 'true'
.\gradlew.bat bootRun
```

기본 프로필에는 샘플 생성기가 없습니다. HTTPS reverse proxy를 통해 `/api`를 백엔드에 전달하세요.
로컬 HTTP로 MySQL 연결만 시험하려면 `SECURE_COOKIE=false`를 명시합니다.
다른 출처를 허용하는 CORS는 열지 않았습니다. 개발은 Vite proxy, 운영은 동일 출처를 사용합니다.

## 계정과 데이터

구직자·기업은 공개 회원가입으로 생성합니다. 관리자 공개 가입 API는 없습니다.
운영 관리자 프로비저닝은 배포 환경의 별도 보안 절차로 수행해야 합니다.
개발 모드의 관리자 계정이 운영에서 자동 생성되는 기능은 없습니다.

개발 H2는 파일 DB, 테스트 H2는 메모리 DB입니다. 통합 테스트는 개발 DB를 비우지 않습니다.
시간은 DB 연결 세션에서 UTC를 사용하고 공고 날짜와 화면 면접 표시는 Asia/Seoul을 사용합니다.

## 포맷

```powershell
cd C:\JobBridge\employment-frontend
npm run format
node node_modules/prettier/bin/prettier.cjs --plugin=prettier-plugin-java --tab-width 4 --write '../employment-backend/src/**/*.java'
```

JSX/CSS/JSON은 2칸, Java는 4칸 들여쓰기를 사용합니다.

