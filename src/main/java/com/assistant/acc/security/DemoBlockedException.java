package com.assistant.acc.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 데모(test) 계정이 새로 만들기·저장 같은 막힌 동작을 했을 때. 화면은 code=DEMO 로 안내를 띄운다 */
public class DemoBlockedException extends ResponseStatusException {

    public static final String DEFAULT_MESSAGE =
            "데모 계정에서는 새로 만들거나 저장할 수 없습니다. 미리 만들어 둔 결과를 보여 드립니다. 직접 만들려면 회원가입해 주세요.";

    public DemoBlockedException() {
        this(DEFAULT_MESSAGE);
    }

    public DemoBlockedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
