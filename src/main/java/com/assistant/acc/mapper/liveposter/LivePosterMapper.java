package com.assistant.acc.mapper.liveposter;

import com.assistant.acc.domain.liveposter.LivePoster;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LivePosterMapper {
    void saveLivePoster(LivePoster livePoster);
}