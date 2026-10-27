package com.assistant.acc.controller.progress;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assistant.acc.service.progress.ProjectProgressService;
import com.assistant.acc.service.progress.ProjectProgressService.Progress;
import com.assistant.acc.service.progress.ProjectProgressService.ProgressUpdate;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 진행 상태 API ("이어서 하기").
 *  - GET /api/progress/current : 가장 최근에 진행한 프로젝트 (없으면 204)
 *  - GET /api/progress/{pNo}   : 특정 프로젝트 (마이페이지에서 골라 이어하기)
 *  - PUT /api/progress/{pNo}   : 화면 간 공유 값 / 마지막 화면 저장
 * 모두 로그인한 회원의 프로젝트만 다룬다 (ProjectAccess).
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProjectProgressController {

    private final ProjectProgressService progressService;

    @GetMapping("/current")
    public ResponseEntity<Progress> current() {
        Progress progress = progressService.current();
        return progress == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(progress);
    }

    @GetMapping("/{pNo}")
    public Progress get(@PathVariable Integer pNo) {
        return progressService.get(pNo);
    }

    @PutMapping("/{pNo}")
    public Progress update(@PathVariable Integer pNo, @RequestBody ProgressUpdate update) {
        return progressService.update(pNo, update);
    }
}
