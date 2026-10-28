package com.assistant.acc.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.domain.member.Member;
import com.assistant.acc.dto.auth.AuthDtos.RoleRequest;
import com.assistant.acc.dto.auth.AuthDtos.StateRequest;
import com.assistant.acc.mapper.member.MemberMapper;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.service.admin.TestDataResetService;
import com.assistant.acc.service.auth.AuthService;
import com.assistant.acc.service.credit.CreditService;

import lombok.RequiredArgsConstructor;

/** 관리자 전용 (SecurityFilterChain 에서 ROLE_ADMIN 만 허용): 회원 목록 / 권한 변경 / 계정 정지 / test 데이터 초기화 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> ROLES = Set.of(Member.ROLE_USER, Member.ROLE_TEST, Member.ROLE_ADMIN);
    private static final Set<String> STATES = Set.of("ACTIVE", "SUSPENDED");

    private final MemberMapper memberMapper;
    private final TestDataResetService resetService;
    private final CreditService creditService;

    @GetMapping("/members")
    public List<Map<String, Object>> members() {
        return memberMapper.findAll().stream().map(AuthService::adminView).collect(Collectors.toList());
    }

    @PatchMapping("/members/{mNo}/role")
    public Map<String, Object> changeRole(@PathVariable String mNo, @RequestBody RoleRequest req) {
        if (req.getRole() == null || !ROLES.contains(req.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role 은 USER / TEST / ADMIN 중 하나여야 합니다.");
        }
        // 마지막 관리자를 스스로 강등해 잠기는 일을 막는다
        if (mNo.equals(CurrentMember.no()) && !Member.ROLE_ADMIN.equals(req.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인의 관리자 권한은 변경할 수 없습니다.");
        }
        requireMember(mNo);
        memberMapper.updateRole(mNo, req.getRole());
        return AuthService.adminView(memberMapper.findByNo(mNo));
    }

    @PatchMapping("/members/{mNo}/state")
    public Map<String, Object> changeState(@PathVariable String mNo, @RequestBody StateRequest req) {
        if (req.getState() == null || !STATES.contains(req.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state 는 ACTIVE / SUSPENDED 중 하나여야 합니다.");
        }
        if (mNo.equals(CurrentMember.no())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 계정은 정지할 수 없습니다.");
        }
        requireMember(mNo);
        memberMapper.updateState(mNo, req.getState());
        return AuthService.adminView(memberMapper.findByNo(mNo));
    }

    /** 크레딧 지급(양수)·회수(음수). body: {"amount": 100, "memo": "이벤트"} */
    @PostMapping("/members/{mNo}/credits")
    public Map<String, Object> grantCredits(@PathVariable String mNo, @RequestBody Map<String, Object> body) {
        requireMember(mNo);
        int amount;
        try {
            amount = Integer.parseInt(String.valueOf(body.get("amount")));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지급할 크레딧을 숫자로 입력해주세요.");
        }
        if (amount == 0 || Math.abs(amount) > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "크레딧은 -100000 ~ 100000 사이로 입력해주세요.");
        }
        Object memo = body.get("memo");
        creditService.grant(mNo, amount, CreditService.ADMIN_GRANT, memo == null ? "관리자 지급" : memo.toString());
        return AuthService.adminView(memberMapper.findByNo(mNo));
    }

    /** 회원의 프로젝트(및 하위 생성물 DB 기록)를 모두 삭제 */
    @PostMapping("/members/{mNo}/reset-data")
    public Map<String, Object> resetData(@PathVariable String mNo) {
        requireMember(mNo);
        // test 계정의 프로젝트는 데모로 보여 주는 미리 만든 결과라 지우지 않는다 (데모는 새로 만들거나 저장하지 않으므로 초기화할 일이 없다)
        if (Member.ROLE_TEST.equals(memberMapper.findByNo(mNo).getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "데모(test) 계정의 데이터는 초기화할 수 없습니다.");
        }
        int deletedProjects = resetService.deleteAllProjectsOf(mNo);
        return Map.of("mNo", mNo, "deletedProjects", deletedProjects);
    }

    private void requireMember(String mNo) {
        if (memberMapper.findByNo(mNo) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }
}
