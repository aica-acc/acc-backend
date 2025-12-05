package com.assistant.acc.service.report;

public interface ReportService {
    // 보도 자료
    String generateArticle(Integer projectNo);
    String generateNotice(Integer projectNo);
    String generateSns(Integer projectNo);
    String generatePackage(Integer projectNo);
}
