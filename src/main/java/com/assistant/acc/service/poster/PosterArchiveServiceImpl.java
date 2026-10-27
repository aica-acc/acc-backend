package com.assistant.acc.service.poster;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import com.assistant.acc.mapper.poster.PosterArchiveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterArchiveServiceImpl implements PosterArchiveService {

    private final PosterArchiveMapper posterArchiveMapper;

    // DB 에 남아있는 과거 절대경로 접두어 (웹 경로로 치환할 때만 사용)
    @Value("${poster.archive.old-base-path:C:\\workspace\\uv_festival\\홍보물}")
    private String OLD_BASE_PATH;
    private final String NEW_WEB_PATH = "/poster-images/";

    // 데이터 조회
    @Override
    public List<PosterArchiveDTO> getPosters(String year, String theme) {

        // 1. DB에서 데이터 조회 (Mapper 호출)
        List<PosterArchiveDTO> postersFromDB = posterArchiveMapper.findPosters(year, theme);

        // 2. 이미지 경로 변환 (C:\... -> /poster-images/...)
        return postersFromDB.stream()
                .peek(dto -> {
                    if (dto.getImageUrl() != null) {
                        String originalPath = dto.getImageUrl();
                        String fileName = originalPath.substring(originalPath.lastIndexOf("\\") + 1);

                        String newImageUrl = String.format("%s/%d/%s/%s/포스터/%s",
                                NEW_WEB_PATH,
                                dto.getYear(),
                                dto.getRegion(),
                                dto.getFestivalName(),
                                fileName);

                        dto.setImageUrl(newImageUrl);
                    }
                })
                .collect(Collectors.toList());
    }
}
