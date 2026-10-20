# REST API

기본 경로 `/api`, JSON camelCase, 오류 `{code,message,fieldErrors,traceId}`.
모든 변경 요청에 `X-XSRF-TOKEN`을 보냅니다. `GET /auth/csrf` 응답 token으로 초기화합니다.
쿠키는 accessToken(`/api`), refreshToken(`/api/auth`), XSRF-TOKEN(`/`)입니다.

## 인증·공개

| 메서드 | 경로 | 입력·기능 |
|---|---|---|
| GET | /auth/csrf | CSRF token |
| POST | /auth/signup/job-seeker | loginId,email,password,name,phone,birthDate(YYYYMMDD),gender(MALE/FEMALE) |
| POST | /auth/signup/company | 위 필드 + companyName,businessNumber |
| POST | /auth/login | loginId,password,memberType(JOB_SEEKER/COMPANY); 선택한 유형과 실제 역할 일치 필요, ADMIN 차단 |
| POST | /auth/admin/login | loginId,password; 활성 ADMIN만 로그인 가능 |
| POST | /auth/refresh | refresh 쿠키, access/refresh 회전 |
| POST | /auth/logout | 세션 취소, 204 |
| POST | /auth/password-reset/request, /confirm | 미설정, 503 |
| GET | /jobs | q,region,type,career,sort(latest/deadline),page(0부터),size(1~100) |
| GET | /jobs/{id} | 승인 활성 기업의 게시·마감 공고 |
| GET | /companies, /companies/{id} | 승인 활성 기업 |
| GET | /notices, /notices/{id} | 게시 공지 |

공고 목록은 `{content,page,size,totalElements,totalPages,first,last}`를 반환합니다.
나머지 목록은 현재 배열 응답입니다. 입력 role은 가입 권한에 사용되지 않습니다.

## 회원·구직자

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET/PATCH | /me | 본인 정보, 수정 name/phone |
| PATCH | /me/password | currentPassword,password; 모든 세션 취소 |
| GET/PATCH | /job-seekers/me | 구직자 상세 |
| GET/POST | /resumes | 목록·생성 |
| GET/PUT/DELETE | /resumes/{id} | 본인 이력서 |
| PUT | /resumes/{id}/default | 기본 지정 |
| GET | /bookmarks | 본인 관심 목록 |
| PUT/DELETE | /bookmarks/{jobId} | 관심 등록·해제 |
| POST | /jobs/{id}/applications | resumeId,coverLetter; 생성 201 |
| GET | /applications, /applications/{id} | 지원 목록·제출 스냅샷·공개 면접 |
| POST | /applications/{id}/cancel | 지원·예정 면접 취소 |
| GET/POST | /employment-links | 목록·신청 |
| GET | /employment-links/{id} | 본인 연계 상세 |
| GET | /notifications | 본인 알림 |
| PATCH | /notifications/{id}/read | 본인 읽음 처리 |
| GET/POST | /inquiries | 본인 문의 조회·등록 title/content |
| GET | /inquiries/{id} | 본인 문의 상세 |

이력서: title,education,careerDescription,skills,portfolioUrl,isPublic,isDefault.
구직자 정보: birthDate,address,desiredJob,desiredRegion,experienceYears,introduction.

## 기업

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | /company/dashboard | 공고·지원 수와 프로필 |
| GET/PUT | /company/profile | 기업 프로필 |
| POST | /company/profile/review-request | 반려 후 재심사 |
| GET/POST | /company/jobs | 본인 기업 공고 목록·초안 작성 |
| GET/PUT | /company/jobs/{id} | 본인 공고 조회·수정 |
| POST | /company/jobs/{id}/review-request | 초안 심사 요청 |
| POST | /company/jobs/{id}/close | 게시 공고 마감 |
| GET | /company/jobs/{id}/applications | 지원자 목록 |
| GET | /company/applications/{id} | 지원 상세·스냅샷·내부 면접 |
| PATCH | /company/applications/{id}/status | status, 순차 전이 |
| POST | /company/applications/{id}/interviews | Idempotency-Key 필수, 면접 생성 201 |
| PUT | /company/interviews/{id} | 예정 면접 수정 |
| PATCH | /company/interviews/{id}/status | status(COMPLETED/CANCELED),result,memo |

공고 입력: title,jobType,careerType,workLocation,description,headcount,startDate,endDate,
education,salary,requirements,preferredQualifications.
면접 입력: interviewAt(시간대 포함 ISO 날짜),interviewType(ONLINE/OFFLINE),location,meetingUrl,memo.
동일 지원·키·본문의 면접 등록은 기존 ID를 반환하고 다른 본문은 409입니다.

## 관리자

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | /admin/dashboard | 회원·공고·지원·연계 집계, 한국 시간 최근 7일 추이, 대기 목록·최근 회원·상태 분포 |
| GET | /admin/{resource}, /admin/{resource}/{id} | users,companies,jobs,employment-links,notices,inquiries |
| PATCH | /admin/users/{id}/status | status ACTIVE/SUSPENDED |
| PATCH | /admin/companies/{id}/approval | status APPROVED/REJECTED,reason |
| PATCH | /admin/jobs/{id}/status | status PUBLISHED/REJECTED/CLOSED,reason |
| PATCH | /admin/employment-links/{id} | status,managerUserId,companyId,jobId,publicMessage,memo |
| POST | /admin/notices | title,content,isPublished |
| PUT | /admin/notices/{id} | 공지 수정·게시 |
| PUT | /admin/inquiries/{id}/answer | answer |

반려에는 reason이 필수입니다. 연계 상태 변경은 순서를 검증하고 기업·공고 관계와 활성 관리자 담당자를 확인합니다.
보호 API는 회원 활성 상태·역할·소유권을 서버에서 검사합니다.
타인 비공개 자원 404, 역할 부족·CSRF 실패 403, 비로그인 401, 상태·중복 충돌 409입니다.
