package com.assistant.acc.mapper.member;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.assistant.acc.domain.member.Member;

/**
 * DB에 저장된 회원 정보를 가져오는 계층
 */
@Mapper
public interface MemberMapper {

    Member findByEmail(@Param("email") String email);

    Member findByNo(@Param("mNo") String mNo);

    /** 가장 큰 회원번호(M000123). 없으면 null */
    String selectMaxNo();

    int insert(Member member);

    List<Member> findAll();

    int updateRole(@Param("mNo") String mNo, @Param("role") String role);

    int updateState(@Param("mNo") String mNo, @Param("state") String state);

    int updateProfile(@Param("mNo") String mNo, @Param("name") String name, @Param("phone") String phone);

    int updatePassword(@Param("mNo") String mNo, @Param("passwordHash") String passwordHash);
}
