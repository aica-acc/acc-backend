package com.assistant.acc.service.admin;

import org.springframework.stereotype.Service;

import com.assistant.acc.service.project.ProjectDeleteService;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 '회원 데이터 초기화': 한 회원의 프로젝트를 모두 지운다.
 * 실제 삭제(FK 를 따라 하위 기록까지, 생성 파일 폴더 포함)는 ProjectDeleteService 가 맡는다.
 */
@Service
@RequiredArgsConstructor
public class TestDataResetService {

    private final ProjectDeleteService projectDeleteService;

    public int deleteAllProjectsOf(String mNo) {
        return projectDeleteService.deleteAllProjectsOf(mNo);
    }
}
