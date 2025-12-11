package com.assistant.acc.service.member;

import com.assistant.acc.domain.member.UserInputs;
import com.assistant.acc.mapper.member.UserInputsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInputsServiceImpl implements UserInputsService {
    @Autowired
    private final UserInputsMapper userInputsMapper;

    @Override
    public UserInputs getUserInput(Integer pNo) {
        return userInputsMapper.UserInputSelectLatest(pNo);
    }
}
