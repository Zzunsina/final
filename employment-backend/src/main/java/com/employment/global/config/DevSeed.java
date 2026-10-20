package com.employment.global.config;

import static com.employment.global.common.Database.*;

import com.employment.global.common.Database;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevSeed implements CommandLineRunner {

    private final Database db;
    private final String password;

    public DevSeed(Database db, @Value("${app.seed-password}") String password) {
        this.db = db;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (password.isBlank() || !db.list("SELECT user_id FROM users LIMIT 1").isEmpty()) return;
        com.employment.global.common.Input.password(
            java.util.Map.of("password", password),
            "password"
        );
        var encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        long admin = db.insert(
            "users",
            fields(
                "email",
                "admin@jobbridge.local",
                "login_id",
                "admin",
                "password",
                hash,
                "name",
                "운영 관리자",
                "phone",
                "01000000001",
                "role",
                "ADMIN"
            )
        );
        long seeker = db.insert(
            "users",
            fields(
                "email",
                "seeker@jobbridge.local",
                "login_id",
                "seeker",
                "password",
                hash,
                "name",
                "김지원",
                "phone",
                "01000000002",
                "role",
                "JOB_SEEKER"
            )
        );
        long seekerId = db.insert("job_seekers", fields("user_id", seeker));
        db.insert(
            "resumes",
            fields(
                "job_seeker_id",
                seekerId,
                "title",
                "새로운 도전을 준비하는 개발자",
                "education",
                "대학교 졸업",
                "skills",
                "Java, React, MySQL",
                "is_default",
                true
            )
        );
        String[] companies = {
            "브릿지랩",
            "그로우컴퍼니",
            "넥스트웨이브",
            "온유파트너스",
            "코드스튜디오",
            "루트테크",
        };
        String[] titles = {
            "함께 성장할 프런트엔드 개발자",
            "서비스를 만드는 백엔드 개발자",
            "사용자 경험을 설계하는 UX 디자이너",
            "가능성을 연결하는 서비스 기획자",
            "데이터로 답을 찾는 분석가",
            "새로운 가치를 만드는 마케터",
        };
        for (int i = 0; i < companies.length; i++) {
            long user = db.insert(
                "users",
                fields(
                    "email",
                    i == 0 ? "company@jobbridge.local" : "company" + i + "@jobbridge.local",
                    "login_id",
                    i == 0 ? "company" : "company" + i,
                    "password",
                    hash,
                    "name",
                    "채용 담당자",
                    "phone",
                    "0101000000" + i,
                    "role",
                    "COMPANY"
                )
            );
            long company = db.insert(
                "companies",
                fields(
                    "user_id",
                    user,
                    "company_name",
                    companies[i],
                    "business_number",
                    "123456789" + i,
                    "industry",
                    i % 2 == 0 ? "IT·소프트웨어" : "서비스·컨설팅",
                    "company_size",
                    "50~100명",
                    "address",
                    "서울특별시",
                    "description",
                    "사람의 가능성을 믿고 함께 성장하는 팀입니다.",
                    "approval_status",
                    "APPROVED"
                )
            );
            db.insert(
                "job_postings",
                fields(
                    "company_id",
                    company,
                    "title",
                    titles[i],
                    "job_type",
                    "정규직",
                    "career_type",
                    i % 2 == 0 ? "신입·경력" : "경력",
                    "education",
                    "학력 무관",
                    "salary",
                    "회사 내규에 따름",
                    "work_location",
                    i % 2 == 0 ? "서울 강남구" : "경기 성남시",
                    "description",
                    "더 나은 일상을 만드는 서비스에 함께해 주세요.\n\n주요 업무\n· 팀과 협업하여 서비스를 개선합니다.\n· 사용자 관점에서 문제를 해결합니다.",
                    "requirements",
                    "원활한 소통과 협업 능력을 갖춘 분",
                    "preferred_qualifications",
                    "관련 프로젝트 경험이 있는 분",
                    "headcount",
                    2,
                    "start_date",
                    LocalDate.now().minusDays(3),
                    "end_date",
                    LocalDate.now().plusDays(10 + i * 4),
                    "status",
                    "PUBLISHED"
                )
            );
        }
        db.insert(
            "notices",
            fields(
                "title",
                "잡브릿지에 오신 것을 환영합니다",
                "content",
                "이력서를 작성하고 나에게 맞는 기회를 찾아보세요. 기업 회원은 관리자 승인 후 공고 심사를 요청할 수 있습니다.",
                "author_user_id",
                admin,
                "is_published",
                true
            )
        );
    }
}
