package com.employment.auth;

import static com.employment.global.common.Database.*;

import com.employment.global.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final Database db;
    private final ObjectMapper json;
    private final byte[] secret;
    private final boolean secure;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(
        Database db,
        ObjectMapper json,
        @Value("${app.jwt-secret}") String secret,
        @Value("${app.secure-cookie}") boolean secure
    ) {
        if (secret.length() < 32) throw new IllegalArgumentException(
            "JWT_SECRET must be at least 32 characters"
        );
        this.db = db;
        this.json = json;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.secure = secure;
    }

    public Map<String, Object> publicUser(long id) {
        return db.one(
            "SELECT user_id,login_id,email,name,phone,birth_date,gender,role,status FROM users WHERE user_id=?",
            id
        );
    }

    public Map<String, Object> user(HttpServletRequest request) {
        Object user = request.getAttribute("user");
        if (user == null) throw new Problem(401, "로그인이 필요합니다.");
        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) user;
        return result;
    }

    public long require(HttpServletRequest request, String role) {
        var user = user(request);
        if (!role.equals(user.get("role"))) throw new Problem(
            403,
            "이 기능을 사용할 권한이 없습니다."
        );
        return number(user, "userId");
    }

    @Transactional
    public Map<String, Object> signup(Map<String, Object> body, boolean company) {
        String loginId = Input.text(body, "loginId", 30, true).strip().toLowerCase(Locale.ROOT);
        if (!loginId.matches("[a-z0-9]{4,30}")) {
            throw new Problem(400, "아이디는 영문과 숫자만 사용하여 4~30자로 입력해 주세요.");
        }
        if (!db.list("SELECT user_id FROM users WHERE login_id=?", loginId).isEmpty()) {
            throw new Problem(409, "이미 사용 중인 아이디입니다.");
        }
        String email = Input.text(body, "email", 255, true).strip().toLowerCase(Locale.ROOT);
        String birthText = Input.text(body, "birthDate", 8, true);
        LocalDate birth;
        try {
            if (!birthText.matches("[0-9]{8}")) throw new IllegalArgumentException();
            birth = LocalDate.parse(
                birthText,
                java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(
                    java.time.format.ResolverStyle.STRICT
                )
            );
            if (
                birth.getYear() < 1 || birth.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))
            ) throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw new Problem(
                400,
                "생년월일을 실제 날짜의 8자리 숫자로 입력해 주세요. 예: 20000123"
            );
        }
        String gender = Input.text(body, "gender", 10, true);
        if (!Set.of("MALE", "FEMALE").contains(gender)) throw new Problem(
            400,
            "성별을 선택해 주세요."
        );
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new Problem(
            400,
            "이메일을 확인해 주세요."
        );
        String phone = Input.text(body, "phone", 30, true).replaceAll("[^0-9]", "");
        if (!phone.matches("[0-9]{10,11}")) throw new Problem(
            400,
            "전화번호는 10~11자리여야 합니다."
        );
        long id = db.insert(
            "users",
            fields(
                "login_id",
                loginId,
                "birth_date",
                birth,
                "gender",
                gender,
                "email",
                email,
                "password",
                encoder.encode(Input.password(body, "password")),
                "name",
                Input.text(body, "name", 100, true),
                "phone",
                phone,
                "role",
                company ? "COMPANY" : "JOB_SEEKER"
            )
        );
        if (company) {
            String business = Input.text(body, "businessNumber", 20, true).replaceAll("[^0-9]", "");
            if (!business.matches("[0-9]{10}")) throw new Problem(
                400,
                "사업자번호는 10자리여야 합니다."
            );
            db.insert(
                "companies",
                fields(
                    "user_id",
                    id,
                    "company_name",
                    Input.text(body, "companyName", 200, true),
                    "business_number",
                    business
                )
            );
        } else db.insert("job_seekers", fields("user_id", id, "birth_date", birth));
        return publicUser(id);
    }

    @Transactional
    public Map<String, Object> login(Map<String, Object> body, HttpServletResponse response) {
        return loginInternal(body, response, false);
    }

    @Transactional
    public Map<String, Object> adminLogin(Map<String, Object> body, HttpServletResponse response) {
        return loginInternal(body, response, true);
    }

    private Map<String, Object> loginInternal(
        Map<String, Object> body,
        HttpServletResponse response,
        boolean adminOnly
    ) {
        String memberType = adminOnly ? "ADMIN" : Input.text(body, "memberType", 20, true);
        if (!adminOnly && !Set.of("JOB_SEEKER", "COMPANY").contains(memberType)) {
            throw new Problem(400, "개인 또는 기업 회원을 선택해 주세요.");
        }
        String identifier = Input.text(body, "loginId", 30, true).strip().toLowerCase(Locale.ROOT);
        var rows = db.list("SELECT * FROM users WHERE login_id=?", identifier);
        String password = Input.text(body, "password", 64, true);
        if (
            rows.isEmpty() ||
            !encoder.matches(password, rows.get(0).get("password").toString()) ||
            !"ACTIVE".equals(rows.get(0).get("status")) ||
            !memberType.equals(rows.get(0).get("role"))
        ) {
            throw new Problem(401, "아이디 또는 비밀번호를 확인해 주세요.");
        }
        long userId = number(rows.get(0), "userId");
        issue(
            userId,
            UUID.randomUUID().toString(),
            Instant.now().plusSeconds(604800),
            response,
            true
        );
        return publicUser(userId);
    }

    @Transactional
    public Map<String, Object> refresh(HttpServletRequest request, HttpServletResponse response) {
        var session = validate(cookie(request, "refreshToken"), "refresh", true);
        long id = number(session, "userId");
        issue(
            id,
            session.get("sessionId").toString(),
            Instant.parse(session.get("refreshExpiresAt").toString()),
            response,
            false
        );
        return publicUser(id);
    }

    public void authenticate(HttpServletRequest request) {
        String token = cookie(request, "accessToken");
        if (token == null) return;
        try {
            var session = validate(token, "access", false);
            request.setAttribute("user", publicUser(number(session, "userId")));
        } catch (Problem ignored) {
            /* The endpoint returns 401 if authentication is required. */
        }
    }

    private Map<String, Object> validate(String token, String type, boolean lock) {
        try {
            if (token == null) throw new IllegalArgumentException();
            String[] pieces = token.split("\\.");
            if (
                pieces.length != 3 ||
                !MessageDigest.isEqual(sign(pieces[0] + "." + pieces[1]), decode(pieces[2]))
            ) {
                throw new IllegalArgumentException();
            }
            var claims = json.readTree(decode(pieces[1]));
            if (
                !type.equals(claims.path("type").asText()) ||
                claims.path("exp").asLong() <= Instant.now().getEpochSecond()
            ) {
                throw new IllegalArgumentException();
            }
            var session = db.one(
                "SELECT * FROM auth_sessions WHERE session_id=?" + (lock ? " FOR UPDATE" : ""),
                claims.path("sid").asText()
            );
            if (
                Boolean.TRUE.equals(session.get("revoked")) ||
                !hash(token).equals(session.get(type + "Hash")) ||
                !"ACTIVE".equals(publicUser(number(session, "userId")).get("status")) ||
                !Long.toString(number(session, "userId")).equals(claims.path("sub").asText())
            ) {
                throw new IllegalArgumentException();
            }
            return session;
        } catch (Exception ex) {
            throw new Problem(401, "로그인이 만료되었습니다.");
        }
    }

    private void issue(
        long userId,
        String sid,
        Instant end,
        HttpServletResponse response,
        boolean create
    ) {
        Instant accessEnd = Instant.now().plusSeconds(1800);
        String access = token(userId, sid, "access", accessEnd);
        String refresh = token(userId, sid, "refresh", end);
        if (create) {
            db.update(
                "INSERT INTO auth_sessions(session_id,user_id,access_hash,refresh_hash,access_expires_at,refresh_expires_at) VALUES(?,?,?,?,?,?)",
                sid,
                userId,
                hash(access),
                hash(refresh),
                Timestamp.from(accessEnd),
                Timestamp.from(end)
            );
        } else db.update(
            "UPDATE auth_sessions SET access_hash=?,refresh_hash=?,access_expires_at=? WHERE session_id=?",
            hash(access),
            hash(refresh),
            Timestamp.from(accessEnd),
            sid
        );
        setCookie(response, "accessToken", access, "/api", 1800);
        setCookie(
            response,
            "refreshToken",
            refresh,
            "/api/auth",
            Math.max(0, Duration.between(Instant.now(), end).getSeconds())
        );
    }

    private String token(long id, String sid, String type, Instant end) {
        try {
            String header = encode(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
            );
            String payload = encode(
                json.writeValueAsBytes(
                    Map.of(
                        "sub",
                        Long.toString(id),
                        "sid",
                        sid,
                        "jti",
                        UUID.randomUUID().toString(),
                        "type",
                        type,
                        "iat",
                        Instant.now().getEpochSecond(),
                        "exp",
                        end.getEpochSecond()
                    )
                )
            );
            return header + "." + payload + "." + encode(sign(header + "." + payload));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void setCookie(
        HttpServletResponse response,
        String name,
        String value,
        String path,
        long age
    ) {
        response.addHeader(
            "Set-Cookie",
            ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(age)
                .build()
                .toString()
        );
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        for (String type : List.of("access", "refresh")) {
            String token = cookie(request, type + "Token");
            if (token != null) db.update(
                "UPDATE auth_sessions SET revoked=TRUE WHERE " + type + "_hash=?",
                hash(token)
            );
        }
        setCookie(response, "accessToken", "", "/api", 0);
        setCookie(response, "refreshToken", "", "/api/auth", 0);
    }

    @Transactional
    public void changePassword(long userId, Map<String, Object> body) {
        var user = db.one("SELECT password FROM users WHERE user_id=? FOR UPDATE", userId);
        if (
            !encoder.matches(
                Input.text(body, "currentPassword", 64, true),
                user.get("password").toString()
            )
        ) {
            throw new Problem(400, "현재 비밀번호가 일치하지 않습니다.");
        }
        db.update(
            "UPDATE users SET password=? WHERE user_id=?",
            encoder.encode(Input.password(body, "password")),
            userId
        );
        db.update("UPDATE auth_sessions SET revoked=TRUE WHERE user_id=?", userId);
    }
}
