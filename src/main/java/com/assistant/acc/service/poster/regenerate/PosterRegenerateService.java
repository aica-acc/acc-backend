package com.assistant.acc.service.poster.regenerate;

import com.assistant.acc.domain.poster.RegenerateResponseDTO;

public interface PosterRegenerateService {
    RegenerateResponseDTO regenerated(String memberNo, Integer filePathNo, String newPrompt);
}
