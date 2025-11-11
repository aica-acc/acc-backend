package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ⭐️ 1. 이 import 문을 추가합니다.
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ⭐️ 2. "모르는 필드는 무시" 설정을 추가합니다.
public class PosterStrategy {

    @JsonProperty("strategy_text")
    private String strategy_text;

    // (이제 Python이 'proposed_content'를 보내더라도
    //  이 DTO는 'strategy_text'만 골라서 받고 나머지는 안전하게 무시합니다.)
}