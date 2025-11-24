package com.assistant.acc.mapper.project.promotion;

import com.assistant.acc.domain.project.promotion.Promotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromotionMapper {
    int insertPromotion(Promotion promotion);

    Promotion selectPromotion(@Param("promotionNo") Integer promotionNo);

    List<Promotion> selectPromotions(@Param("pNo") Integer pNo);

    int updatePromotion(Promotion promotion);

    int deletePromotion(@Param("promotionNo") Integer promotionNo);
}
