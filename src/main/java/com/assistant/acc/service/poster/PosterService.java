package com.assistant.acc.service.poster;

import java.io.IOException;
import java.util.List;

import com.assistant.acc.dto.create.poster.PosterCreateApiRequest;
import com.assistant.acc.dto.create.poster.PosterCreateImageResponse;
import com.assistant.acc.dto.create.prompt.PosterPromptApiRequest;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 포스터 분석 및 생성 서비스의 규약(Interface)
 * (이제 이 서비스는 'AI 분석 요청'이라는 단일 책임만 가집니다)
 */
public interface PosterService {

    /**
     * 포스터 파일과 사용자 입력을 받아 AI 분석을 요청하고 결과를 반환합니다.
     * (프로젝트 생성 및 저장은 ProjectService에게 위임합니다)
     *
     * @param file     사용자가 업로드한 포스터 (PDF 또는 이미지)
     * @param theme    축제 주제
     * @param keywords 축제 키워드
     * @param title    축제 제목
     * @return AI 서버가 반환한 원본 JSON 문자열
     * @throws IOException AI 서버 통신 또는 파일 처리 중 예외 발생 시
     */
    String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException;
    /**
     * 2단계: AI 프롬프트 생성을 Python 서버에 요청
     * (JSON을 받아 JSON으로 반환한다)
     *
     * @param jsonBody 1단계 분석 결과가 담긴 원본 JSON 문자열
     * @return AI 서버가 반환한 프롬프트 시안(JSON 문자열)
     * @throws IOException
     */

    PosterPromptResponse generatePrompt(PosterPromptApiRequest request);

    /**
     * 3단계: 최종 홍보물 생성을 Python 서버에 요청.
     * (JSON을 받아 JSON으로 반환한다)
     *
     * @param jsonBody 1단계 분석 결과 + 2단계 선택 시안이 담긴 JSON
     * @return AI 서버가 반환한 최종 결과물(이미지 URL 등 JSON)
     * @throws IOException
     */

    String generateDrafts(String jsonBody) throws IOException;
    PosterArchiveDTO getPosterById(Integer filePathNo);
    List<PosterElementDTO> getPosterPrompts(Integer projectNo);
    ImageRegenerateResponseDTO regeneratePoster(Integer filePathNo, String visualPrompt) throws IOException;
}