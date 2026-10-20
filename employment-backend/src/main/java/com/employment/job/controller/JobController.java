package com.employment.job.controller;

import com.employment.auth.AuthService;
import com.employment.global.common.*;
import com.employment.job.service.EmploymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class JobController {

    private final EmploymentService service;
    private final AuthService auth;

    public JobController(EmploymentService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/jobs")
    Object jobs(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "") String region,
        @RequestParam(defaultValue = "") String type,
        @RequestParam(defaultValue = "") String career,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return service.jobs(q, region, type, career, sort, page, size);
    }

    @GetMapping("/jobs/{id}")
    Object job(@PathVariable long id) {
        return service.publicJob(id);
    }

    @GetMapping("/companies")
    Object companies() {
        return service.companies();
    }

    @GetMapping("/companies/{id}")
    Object company(@PathVariable long id) {
        return service.companyDetail(id);
    }

    @GetMapping("/resumes")
    Object resumes(HttpServletRequest req) {
        return service.resumes(auth.require(req, "JOB_SEEKER"));
    }

    @GetMapping("/resumes/{id}")
    Object resume(@PathVariable long id, HttpServletRequest req) {
        return service.resume(auth.require(req, "JOB_SEEKER"), id);
    }

    @PostMapping("/resumes")
    @ResponseStatus(HttpStatus.CREATED)
    Object createResume(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return service.saveResume(auth.require(req, "JOB_SEEKER"), null, body);
    }

    @PutMapping("/resumes/{id}")
    Object updateResume(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        return service.saveResume(auth.require(req, "JOB_SEEKER"), id, body);
    }

    @DeleteMapping("/resumes/{id}")
    void deleteResume(@PathVariable long id, HttpServletRequest req) {
        service.deleteResume(auth.require(req, "JOB_SEEKER"), id);
    }

    @PutMapping("/resumes/{id}/default")
    void defaultResume(@PathVariable long id, HttpServletRequest req) {
        service.defaultResume(auth.require(req, "JOB_SEEKER"), id);
    }

    @GetMapping("/bookmarks")
    Object bookmarks(HttpServletRequest req) {
        return service.bookmarks(auth.require(req, "JOB_SEEKER"));
    }

    @PutMapping("/bookmarks/{id}")
    void bookmark(@PathVariable long id, HttpServletRequest req) {
        service.bookmark(auth.require(req, "JOB_SEEKER"), id, true);
    }

    @DeleteMapping("/bookmarks/{id}")
    void unbookmark(@PathVariable long id, HttpServletRequest req) {
        service.bookmark(auth.require(req, "JOB_SEEKER"), id, false);
    }

    @PostMapping("/jobs/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    Object apply(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        return service.apply(auth.require(req, "JOB_SEEKER"), id, body);
    }

    @GetMapping("/applications")
    Object applications(HttpServletRequest req) {
        return service.applications(auth.require(req, "JOB_SEEKER"));
    }

    @GetMapping("/applications/{id}")
    Object application(@PathVariable long id, HttpServletRequest req) {
        return service.application(auth.require(req, "JOB_SEEKER"), id);
    }

    @PostMapping("/applications/{id}/cancel")
    void cancel(@PathVariable long id, HttpServletRequest req) {
        service.cancel(auth.require(req, "JOB_SEEKER"), id);
    }

    @GetMapping("/employment-links")
    Object links(HttpServletRequest req) {
        return service.links(auth.require(req, "JOB_SEEKER"));
    }

    @PostMapping("/employment-links")
    @ResponseStatus(HttpStatus.CREATED)
    Object requestLink(HttpServletRequest req) {
        return service.requestLink(auth.require(req, "JOB_SEEKER"));
    }

    @GetMapping("/employment-links/{id}")
    Object linkDetail(@PathVariable long id, HttpServletRequest req) {
        return service
            .links(auth.require(req, "JOB_SEEKER"))
            .stream()
            .filter(row -> Database.number(row, "employmentLinkId") == id)
            .findFirst()
            .orElseThrow(() -> new Problem(404, "내역을 찾을 수 없습니다."));
    }

    @PutMapping("/company/interviews/{id}")
    void editInterview(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        service.updateInterview(auth.require(req, "COMPANY"), id, body, false);
    }

    @PatchMapping("/company/interviews/{id}/status")
    void interviewStatus(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        service.updateInterview(auth.require(req, "COMPANY"), id, body, true);
    }

    @GetMapping("/company/jobs")
    Object ownJobs(HttpServletRequest req) {
        return service.companyJobs(auth.require(req, "COMPANY"));
    }

    @GetMapping("/company/jobs/{id}")
    Object ownJob(@PathVariable long id, HttpServletRequest req) {
        return service.ownJob(auth.require(req, "COMPANY"), id, false);
    }

    @PostMapping("/company/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    Object createJob(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return service.saveJob(auth.require(req, "COMPANY"), null, body);
    }

    @PutMapping("/company/jobs/{id}")
    Object saveJob(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        return service.saveJob(auth.require(req, "COMPANY"), id, body);
    }

    @PostMapping("/company/jobs/{id}/{action}")
    void jobAction(@PathVariable long id, @PathVariable String action, HttpServletRequest req) {
        service.jobAction(auth.require(req, "COMPANY"), id, action);
    }

    @GetMapping("/company/jobs/{id}/applications")
    Object applicants(@PathVariable long id, HttpServletRequest req) {
        return service.applicants(auth.require(req, "COMPANY"), id);
    }

    @GetMapping("/company/applications/{id}")
    Object applicant(@PathVariable long id, HttpServletRequest req) {
        return service.companyApplication(auth.require(req, "COMPANY"), id);
    }

    @PatchMapping("/company/applications/{id}/status")
    void status(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        service.applicationStatus(
            auth.require(req, "COMPANY"),
            id,
            Input.text(body, "status", 30, true)
        );
    }

    @PostMapping("/company/applications/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    Object interview(
        @PathVariable long id,
        @RequestHeader(value = "Idempotency-Key", required = false) String key,
        @RequestBody Map<String, Object> body,
        HttpServletRequest req
    ) {
        return service.interview(auth.require(req, "COMPANY"), id, key, body);
    }
}
