package com.assistant.acc.controller.job;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assistant.acc.service.job.GenerationJobService;
import com.assistant.acc.service.job.GenerationJobService.Job;

import lombok.RequiredArgsConstructor;

/**
 * 백그라운드 생성 작업 조회.
 *  - GET  /api/jobs/{id}      : 작업 상태와 진행 상황 (화면이 몇 초마다 조회)
 *  - GET  /api/jobs/active    : 진행 중 + 아직 확인하지 않은 완료 작업 (헤더 알림)
 *  - POST /api/jobs/{id}/seen : 완료 알림 확인
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final GenerationJobService jobService;

    @GetMapping("/active")
    public List<Job> active() {
        return jobService.active();
    }

    @GetMapping("/{jobId}")
    public Job get(@PathVariable long jobId) {
        return jobService.get(jobId);
    }

    @PostMapping("/{jobId}/seen")
    public ResponseEntity<Void> seen(@PathVariable long jobId) {
        jobService.markSeen(jobId);
        return ResponseEntity.noContent().build();
    }
}
