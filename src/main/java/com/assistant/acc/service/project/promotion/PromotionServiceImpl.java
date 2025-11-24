package com.assistant.acc.service.project.promotion;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.project.promotion.Promotion;
import com.assistant.acc.mapper.project.promotion.PromotionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionMapper promotionMapper;

    public Integer createPromotion(Integer pNo, Integer promptNo, String type) {

        Promotion p = Promotion.builder()
                .pNo(pNo)
                .promptNo(promptNo)     // 첫 번째 프롬프트 기준
                .promotionType(type)    // "포스터"
                .createdAt(LocalDateTime.now())
                .build();

        promotionMapper.insertPromotion(p);

        return p.getPromotionNo();
    }
}
