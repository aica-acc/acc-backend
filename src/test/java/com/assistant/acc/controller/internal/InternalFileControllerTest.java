package com.assistant.acc.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class InternalFileControllerTest {

    @TempDir
    Path base;

    private InternalFileController c;

    @BeforeEach
    void setUp() {
        c = new InternalFileController();
        ReflectionTestUtils.setField(c, "reactBasePath", base.toString());
        ReflectionTestUtils.setField(c, "internalToken", "secret");
    }

    private MockHttpServletRequest body(byte[] b) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setContent(b);
        return r;
    }

    @Test
    void 올리고_받기() throws Exception {
        String p = "data/promotion/M1/7/video/poster_video.mp4";
        assertThat(c.put(p, "secret", body(new byte[] {1, 2})).getStatusCode().value()).isEqualTo(204);
        assertThat(Files.readAllBytes(base.resolve("public").resolve(p))).containsExactly(1, 2);
        assertThat(c.get("/" + p, "secret").getStatusCode().value()).isEqualTo(200);
        assertThat(c.get("data/promotion/M1/7/none.png", "secret").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void 토큰이_틀리면_403_비어있으면_꺼짐() {
        assertThatThrownBy(() -> c.get("data/promotion/a.png", "wrong"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        ReflectionTestUtils.setField(c, "internalToken", "");
        assertThatThrownBy(() -> c.get("data/promotion/a.png", "secret"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void promotion_밖은_거부() {
        for (String p : new String[] {"data/promotion/../../etc/passwd", "data/other/x.png", "/etc/passwd"}) {
            assertThatThrownBy(() -> c.put(p, "secret", body(new byte[] {1})))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
    }
}
