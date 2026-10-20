package com.employment.global.config;

import com.employment.auth.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        AuthService auth,
        @org.springframework.beans.factory.annotation.Value("${app.secure-cookie}") boolean secure
    ) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.sameSite("Lax").path("/").secure(secure));
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .csrf(c ->
                c
                    .csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(a ->
                a
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/jobs",
                        "/api/jobs/*",
                        "/api/companies",
                        "/api/companies/*",
                        "/api/notices",
                        "/api/notices/*"
                    )
                    .permitAll()
                    .requestMatchers("/api/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll()
            )
            .exceptionHandling(e ->
                e
                    .accessDeniedHandler((req, res, ex) ->
                        error(res, 403, "보안 토큰을 확인해 주세요.")
                    )
                    .authenticationEntryPoint((req, res, ex) ->
                        error(res, 401, "로그인이 필요합니다.")
                    )
            )
            .addFilterAfter(
                new OncePerRequestFilter() {
                    private final Map<String, Window> limits = new ConcurrentHashMap<>();

                    @Override
                    protected void doFilterInternal(
                        HttpServletRequest req,
                        HttpServletResponse res,
                        FilterChain chain
                    ) throws ServletException, IOException {
                        if (
                            req.getMethod().equals("POST") &&
                            (req.getRequestURI().equals("/api/auth/login") ||
                                req.getRequestURI().equals("/api/auth/admin/login") ||
                                req.getRequestURI().contains("password-reset"))
                        ) {
                            long now = System.currentTimeMillis();
                            limits
                                .entrySet()
                                .removeIf(entry -> now - entry.getValue().start > 60000);
                            Window w = limits.computeIfAbsent(req.getRemoteAddr(), key ->
                                new Window(now)
                            );
                            synchronized (w) {
                                if (++w.count > 15) {
                                    error(res, 429, "잠시 후 다시 시도해 주세요.");
                                    return;
                                }
                            }
                        }
                        auth.authenticate(req);
                        chain.doFilter(req, res);
                    }
                },
                CsrfFilter.class
            );
        return http.build();
    }

    private static class Window {

        final long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }

    private static void error(HttpServletResponse response, int status, String message)
        throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response
            .getWriter()
            .write(
                "{\"code\":\"HTTP_" +
                    status +
                    "\",\"message\":\"" +
                    message +
                    "\",\"fieldErrors\":[],\"traceId\":\"" +
                    UUID.randomUUID() +
                    "\"}"
            );
    }
}
