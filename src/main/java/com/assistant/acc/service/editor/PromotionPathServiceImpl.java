package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.PromotionPathDTO;
import com.assistant.acc.mapper.editor.PromotionPathMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionPathServiceImpl implements PromotionPathService {

    private final PromotionPathMapper promotionPathMapper;

    @Override
    @Transactional
    public void savePromotionPaths(int pNo, List<String> paths, List<String> types) {
        log.info("[PromotionPathService] pNo={}, paths={}, types={}", pNo, paths, types);

        if (paths == null || types == null) {
            log.warn("[PromotionPathService] paths or types is null, skip");
            return;
        }
        if (paths.size() != types.size()) {
            throw new IllegalArgumentException("dbFilePath와 dbFileType의 길이가 다릅니다.");
        }

        for (int i = 0; i < paths.size(); i++) {
            PromotionPathDTO dto = new PromotionPathDTO();
            dto.setPNo(pNo);
            dto.setDbFilePath(paths.get(i));
            dto.setDbFileType(types.get(i));
            dto.setCreateAt(LocalDate.now());

            log.info("[PromotionPathService] insert dto={}", dto);
            promotionPathMapper.insertPromotionPath(dto);
        }
    }
}
