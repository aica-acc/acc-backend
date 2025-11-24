package com.assistant.acc.service.prompt;

import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.mapper.prompt.PromptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService{
    @Autowired
    private final PromptMapper promptMapper;

    @Override
    public Prompt savePrompt(Prompt prompt) {
        promptMapper.insertPrompt(prompt);
        return prompt;
    }

    @Override
    public Prompt getPrompt(Integer promptNo) {
        return promptMapper.selectPrompt(promptNo);
    }

    @Override
    public List<Prompt> getPrompts(Integer userInputNo) {
        return promptMapper.selectPrompts(userInputNo);
    }

    @Override
    public int updatePrompt(Prompt prompt) {
        return promptMapper.updatePrompt(prompt);
    }

    @Override
    public int deletePrompt(Integer promptNo) {
        return promptMapper.delete(promptNo);
    }
}
