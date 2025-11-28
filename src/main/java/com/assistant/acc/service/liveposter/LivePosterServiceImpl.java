                                                                package com.assistant.acc.service.liveposter;

import com.assistant.acc.domain.liveposter.LivePoster;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.liveposter.LivePosterRequestDTO;
import com.assistant.acc.dto.liveposter.LivePosterResponseDTO;
import com.assistant.acc.mapper.liveposter.LivePosterMapper;
import com.assistant.acc.mapper.poster.PosterArchiveMapper;
import com.assistant.acc.mapper.project.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivePosterServiceImpl implements LivePosterService {

    private final LivePosterMapper livePosterMapper;
    private final ProjectMapper projectMapper;
    private final PosterArchiveMapper posterMapper;
    private final RestTemplate restTemplate;

    private static final String AI_SERVER_URL = "http://localhost:8000/liveposter/generate";

    @Override
    public void createLivePoster(Integer pNo, Integer posterNo) {
        log.info("라이브 포스터 생성 시작 - Project: {}, Poster: {}", pNo, posterNo);

        // 1. [DB 조회]
        ProposalMetadata metadata = projectMapper.findMetadataByPno(pNo);
        String posterPath = posterMapper.findFilePathByNo(posterNo);

        // 2. [요청 객체]
        LivePosterRequestDTO request = new LivePosterRequestDTO();
        request.setProjectId(pNo);
        request.setPosterImagePath(posterPath);

        if (metadata != null) {
            request.setConceptText(metadata.getConceptDescription());
            request.setVisualKeywords(metadata.getVisualKeywords());
        } else {
            request.setConceptText("A creative festival poster");
            request.setVisualKeywords("festival, poster");
        }

        try {
            // 3. [AI 호출] ✅ 배열(Array) 형태로 응답을 받습니다.
            LivePosterResponseDTO[] responses = restTemplate.postForObject(
                    AI_SERVER_URL,
                    request,
                    LivePosterResponseDTO[].class
            );

            // 4. [DB 저장] ✅ 반복문으로 결과 리스트를 모두 저장합니다.
            if (responses != null && responses.length > 0) {
                for (LivePosterResponseDTO res : responses) {
                    LivePoster livePoster = new LivePoster();
                    livePoster.setPNo(pNo);
                    livePoster.setPosterNo(posterNo);

                    livePoster.setTaskId(res.getTaskId());
                    livePoster.setFilePath(res.getFilePath());
                    livePoster.setMotionPrompt(res.getMotionPrompt());
                    livePoster.setAspectRatio(res.getAspectRatio()); // 비율 정보 저장

                    livePosterMapper.saveLivePoster(livePoster);
                    log.info("라이브 포스터 저장 완료 ({}): {}", res.getAspectRatio(), res.getFilePath());
                }
            } else {
                log.warn("AI 서버로부터 생성된 영상이 없습니다.");
            }

        } catch (Exception e) {
            log.error("라이브 포스터 생성 실패", e);
            throw new RuntimeException("AI 서버 통신 오류: " + e.getMessage());
        }
    }
}