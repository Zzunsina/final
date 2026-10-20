package com.employment.admin;

import static com.employment.global.common.Database.*;

import com.employment.global.common.Database;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final Database db;

    public AdminDashboardService(Database db) {
        this.db = db;
    }

    public Map<String, Object> overview() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        Timestamp start = Timestamp.from(today.atStartOfDay(zone).toInstant());
        Timestamp end = Timestamp.from(today.plusDays(1).atStartOfDay(zone).toInstant());
        long companies = count(
            "SELECT COUNT(*) AS n FROM companies WHERE approval_status='PENDING'"
        );
        long jobs = count("SELECT COUNT(*) AS n FROM job_postings WHERE status='PENDING'");
        long inquiries = count("SELECT COUNT(*) AS n FROM inquiries WHERE status='OPEN'");
        long links = count(
            "SELECT COUNT(*) AS n FROM employment_links WHERE status NOT IN ('CLOSED','EMPLOYED')"
        );
        long requested = count(
            "SELECT COUNT(*) AS n FROM employment_links WHERE status='REQUESTED'"
        );
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Timestamp from = Timestamp.from(date.atStartOfDay(zone).toInstant());
            Timestamp until = Timestamp.from(date.plusDays(1).atStartOfDay(zone).toInstant());
            trend.add(
                fields(
                    "date",
                    date.toString(),
                    "members",
                    count(
                        "SELECT COUNT(*) AS n FROM users WHERE role IN ('JOB_SEEKER','COMPANY') AND created_at>=? AND created_at<?",
                        from,
                        until
                    ),
                    "applications",
                    count(
                        "SELECT COUNT(*) AS n FROM applications WHERE applied_at>=? AND applied_at<?",
                        from,
                        until
                    )
                )
            );
        }
        return fields(
            "asOf",
            Instant.now().toString(),
            "date",
            today.toString(),
            "companies",
            companies,
            "jobs",
            jobs,
            "inquiries",
            inquiries,
            "employmentLinks",
            links,
            "newEmploymentLinks",
            requested,
            "pendingTotal",
            companies + jobs + inquiries + requested,
            "totalMembers",
            count("SELECT COUNT(*) AS n FROM users WHERE role IN ('JOB_SEEKER','COMPANY')"),
            "jobSeekers",
            count("SELECT COUNT(*) AS n FROM users WHERE role='JOB_SEEKER'"),
            "companyMembers",
            count("SELECT COUNT(*) AS n FROM users WHERE role='COMPANY'"),
            "todayMembers",
            count(
                "SELECT COUNT(*) AS n FROM users WHERE role IN ('JOB_SEEKER','COMPANY') AND created_at>=? AND created_at<?",
                start,
                end
            ),
            "publishedJobs",
            count(
                "SELECT COUNT(*) AS n FROM job_postings j JOIN companies c ON c.company_id=j.company_id JOIN users u ON u.user_id=c.user_id WHERE j.status='PUBLISHED' AND j.end_date>=? AND c.approval_status='APPROVED' AND u.status='ACTIVE'",
                today
            ),
            "totalApplications",
            count("SELECT COUNT(*) AS n FROM applications"),
            "todayApplications",
            count(
                "SELECT COUNT(*) AS n FROM applications WHERE applied_at>=? AND applied_at<?",
                start,
                end
            ),
            "employedLinks",
            count("SELECT COUNT(*) AS n FROM employment_links WHERE status='EMPLOYED'"),
            "trend",
            trend,
            "applicationStatuses",
            db.list("SELECT status,COUNT(*) AS total FROM applications GROUP BY status"),
            "pendingCompanies",
            db.list(
                "SELECT company_id,company_name,industry,approval_status FROM companies WHERE approval_status='PENDING' ORDER BY company_id DESC LIMIT 5"
            ),
            "pendingJobs",
            db.list(
                "SELECT j.job_id,j.title,j.created_at,j.status,c.company_name FROM job_postings j JOIN companies c ON j.company_id=c.company_id WHERE j.status='PENDING' ORDER BY j.job_id DESC LIMIT 5"
            ),
            "openInquiries",
            db.list(
                "SELECT inquiry_id,title,created_at,status FROM inquiries WHERE status='OPEN' ORDER BY inquiry_id DESC LIMIT 4"
            ),
            "recentMembers",
            db.list(
                "SELECT user_id,name,role,status,created_at FROM users WHERE role IN ('JOB_SEEKER','COMPANY') ORDER BY user_id DESC LIMIT 5"
            ),
            "recentLinks",
            db.list(
                "SELECT employment_link_id,status,requested_at FROM employment_links WHERE status NOT IN ('CLOSED','EMPLOYED') ORDER BY employment_link_id DESC LIMIT 4"
            )
        );
    }

    private long count(String sql, Object... args) {
        return number(db.one(sql, args), "n");
    }
}
