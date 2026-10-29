package com.assistant.acc.service.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class AiPromotionFilesTest {

    private MockRestServiceServer server;
    private AiPromotionFiles files;

    @BeforeEach
    void setUp() {
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        files = new AiPromotionFiles(rest);
        ReflectionTestUtils.setField(files, "pythonApiUrl", "http://ai:5000/");
    }

    @Test
    void list_타입_접두어로_조회() {
        server.expect(requestTo("http://ai:5000/internal/promotion/mascot?prefix=mascot_&ext=.png"))
                .andRespond(withSuccess("{\"files\":[\"mascot_1_0.png\",\"mascot_1_1.png\"]}", MediaType.APPLICATION_JSON));

        assertThat(files.list("mascot")).containsExactly("mascot_1_0.png", "mascot_1_1.png");
        server.verify();
    }

    @Test
    void download_저장하고_없으면_false(@TempDir Path dir) throws Exception {
        server.expect(requestTo("http://ai:5000/internal/promotion/poster/poster_1_0.png"))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG));
        server.expect(requestTo("http://ai:5000/internal/promotion/poster/none.png"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Path dest = dir.resolve("a.png");
        assertThat(files.download("poster", "poster_1_0.png", dest)).isTrue();
        assertThat(Files.readAllBytes(dest)).containsExactly(1, 2, 3);
        assertThat(files.download("poster", "none.png", dir.resolve("b.png"))).isFalse();
        server.verify();
    }

    @Test
    void delete_없으면_false() {
        server.expect(requestTo("http://ai:5000/internal/promotion/mascot/mascot_1_0.png"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withSuccess());
        server.expect(requestTo("http://ai:5000/internal/promotion/mascot/gone.png"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(files.delete("mascot", "mascot_1_0.png")).isTrue();
        assertThat(files.delete("mascot", "gone.png")).isFalse();
        server.verify();
    }
}
