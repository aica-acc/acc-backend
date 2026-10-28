package com.assistant.acc.controller.credit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.service.credit.CreditService;

import lombok.RequiredArgsConstructor;

/** 내 크레딧: 잔액, 기능별 차감량, 최근 사용 기록 */
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/me")
    public Map<String, Object> me() {
        String mNo = CurrentMember.no();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("balance", creditService.balance(mNo));
        // 관리자는 차감하지 않고, test 계정은 데모라 유료 생성을 하지 않는다
        res.put("unlimited", CurrentMember.isAdmin());
        res.put("demo", CurrentMember.isTest());
        res.put("signupBonus", creditService.signupBonus());
        res.put("costs", creditService.costs());
        res.put("history", creditService.history(mNo, 30));
        return res;
    }
}
