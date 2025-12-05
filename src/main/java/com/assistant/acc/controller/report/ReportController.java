package com.assistant.acc.controller.report;

import com.assistant.acc.dto.report.ArticleReportRequest;
import com.assistant.acc.dto.report.NoticeReportRequest;
import com.assistant.acc.dto.report.PackageReportRequest;
import com.assistant.acc.dto.report.SnsReportRequest;
import com.assistant.acc.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/article")
    public ResponseEntity<?> generateArticle(@RequestBody ArticleReportRequest request) { // 🔥 변경
        return ResponseEntity.ok(Map.of("status", "success", "content", reportService.generateArticle(request.getProjectNo())));
    }

    @PostMapping("/notice")
    public ResponseEntity<?> generateNotice(@RequestBody NoticeReportRequest request) { // 🔥 변경
        return ResponseEntity.ok(Map.of("status", "success", "content", reportService.generateNotice(request.getProjectNo())));
    }

    @PostMapping("/sns")
    public ResponseEntity<?> generateSns(@RequestBody SnsReportRequest request) { // 🔥 변경
        return ResponseEntity.ok(Map.of("status", "success", "content", reportService.generateSns(request.getProjectNo())));
    }

    @PostMapping("/package")
    public ResponseEntity<?> generatePackage(@RequestBody PackageReportRequest request) { // 🔥 변경
        return ResponseEntity.ok(Map.of("status", "success", "content", reportService.generatePackage(request.getProjectNo())));
    }
}