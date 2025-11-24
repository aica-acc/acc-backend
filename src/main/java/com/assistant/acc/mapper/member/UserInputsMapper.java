package com.assistant.acc.mapper.member;

import com.assistant.acc.domain.member.UserInputs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserInputsMapper {

    int UserInputInsert(UserInputs userInputs);

    UserInputs UserInputSelect(@Param("userInputNo") Integer userInputNo);

    List<UserInputs> UserInputSelectsByProject(@Param("pNo") Integer pNo);

    UserInputs UserInputSelectLatest(@Param("pNo") Integer pNo);

    int UserInputUpdate(UserInputs userInputs);

    int UserInputDelete(@Param("userInputNo") Integer userInputNo);
}