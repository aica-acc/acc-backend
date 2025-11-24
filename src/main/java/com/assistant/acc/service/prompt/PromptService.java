package com.assistant.acc.service.prompt;

import com.assistant.acc.domain.prompt.Prompt;

import java.util.List;

public interface PromptService {
    Prompt savePrompt(Prompt prompt);
    Prompt getPrompt(Integer promptNo);
    List<Prompt> getPrompts(Integer userInputNo);
    int updatePrompt(Prompt prompt);
    int deletePrompt(Integer promptNo);
}
