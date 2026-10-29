package com.assistant.acc.service.poster.generate;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class AiFailureTest {

    @Test
    void 일부_실패는_몇번째_장인지_알려준다() {
        String body = "{\"detail\":{\"message\":\"x\",\"failed\":[{\"index\":1,\"style_name\":\"B\",\"error\":\"NSFW\"}],"
                + "\"succeeded\":[\"a\",\"c\",\"d\"]}}";
        var http = HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        var e = AiFailure.of(new RuntimeException("파이썬 호출 실패", http), "포스터");

        assertThat(e.getStatusCode().value()).isEqualTo(502);
        assertThat(e.getReason()).isEqualTo("포스터 4장 중 1장(2번)을 만들지 못했습니다. 다시 시도하면 실패한 장만 다시 만듭니다.");
        assertThat(e.getReason()).doesNotContain("NSFW");
    }

    @Test
    void 연결_끊김은_다시_시도_안내() {
        var e = AiFailure.of(new RuntimeException(new ResourceAccessException("Read timed out")), "마스코트");
        assertThat(e.getReason()).contains("연결이 끊겼습니다").contains("마스코트");
    }

    @Test
    void 형식이_다른_오류는_기본_문구() {
        var http = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "err", HttpHeaders.EMPTY,
                "{\"detail\":\"/app/x.py boom\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        var e = AiFailure.of(new RuntimeException(http), "포스터");
        assertThat(e.getReason()).isEqualTo("포스터 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.").doesNotContain("/app");
    }
}
