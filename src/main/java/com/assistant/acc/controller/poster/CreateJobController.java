package com.assistant.acc.controller.poster;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.security.ProjectAccess;
import com.assistant.acc.service.job.GenerationJobService;
import com.assistant.acc.service.job.GenerationJobService.Job;
import com.assistant.acc.service.poster.generate.PromotionAPIService;

import lombok.RequiredArgsConstructor;

/**
 * 베이스 포스터·마스코트 생성을 백그라운드 작업으로 실행한다.
 * 예전에는 화면이 프롬프트 생성 → 이미지 생성을 4번 순서대로 호출해, 로딩 화면을 벗어나거나 새로고침하면
 * 진행 상황을 알 수 없었다. 이제 한 번에 작업으로 맡기고 GET /api/jobs/{id} 로 단계별 진행을 본다.
 */
@RestController
@RequestMapping("/api/create-jobs")
@RequiredArgsConstructor
public class CreateJobController {

    private final PromotionAPIService promotionAPIService;
    private final GenerationJobService jobService;
    private final ProjectAccess projectAccess;

    /** POST /api/create-jobs?pNo=40  (body: 분석 화면의 trendData, 기존 /api/generate-prompt 와 같음) */
    @PostMapping
    public Job start(@RequestParam("pNo") Integer pNo, @RequestBody Map<String, Object> trendData) {
        // 파일은 프로젝트 소유자 폴더에 저장한다 (관리자가 대신 실행해도 소유자 것). 작업 알림은 실행한 사람에게
        String mNo = projectAccess.check(pNo);
        return jobService.start(pNo, CurrentMember.no(), GenerationJobService.CREATE_IMAGES, progress -> {
            progress.step("poster_prompt", "포스터 프롬프트 작성", jobService.expectSeconds("poster_prompt", 20))
                    .step("poster_images", "포스터 이미지 생성", jobService.expectSeconds("poster_images", 110))
                    .step("mascot_prompt", "마스코트 프롬프트 작성", jobService.expectSeconds("mascot_prompt", 20))
                    .step("mascot_images", "마스코트 이미지 생성", jobService.expectSeconds("mascot_images", 100));

            progress.run("poster_prompt", () -> promotionAPIService.generatePrompts(mNo, pNo, trendData, "poster"));
            progress.run("poster_images", () -> promotionAPIService.createPosterImages(mNo, pNo, trendData, "poster"));
            progress.run("mascot_prompt", () -> promotionAPIService.generatePrompts(mNo, pNo, trendData, "mascot"));
            progress.run("mascot_images", () -> promotionAPIService.createPosterImages(mNo, pNo, trendData, "mascot"));
            // 결과 목록은 화면이 /api/assets/list 로 다시 받는다 (기존 결과를 쓴 경우에도 같은 경로)
            return Map.of("pNo", pNo);
        });
    }

    /** 이 프로젝트의 최근 생성 작업 (로딩 화면을 다시 열었을 때 이어서 보기). 없으면 204 */
    @GetMapping("/latest")
    public ResponseEntity<Job> latest(@RequestParam("pNo") Integer pNo) {
        projectAccess.check(pNo);
        Job job = jobService.latestFor(pNo, GenerationJobService.CREATE_IMAGES);
        return job == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(job);
    }
}
