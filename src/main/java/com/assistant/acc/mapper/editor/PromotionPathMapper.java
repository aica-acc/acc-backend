package com.assistant.acc.mapper.editor;

import com.assistant.acc.dto.editor.PromotionPathDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PromotionPathMapper {

    int insertPromotionPath(PromotionPathDTO dto);

    // 필요하면 나중에 이런 것도 추가 가능:
    // List<PromotionPathDTO> findByPNo(@Param("pNo") int pNo);
    List<PromotionPathDTO> findAllByPNo(Integer pNo);
}
