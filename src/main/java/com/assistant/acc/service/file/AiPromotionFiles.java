package com.assistant.acc.service.file;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

/**
 * AI 서버가 만든 홍보물(PROMOTION_DIR/{type}/파일) 을 HTTP 로 다룬다 (AI /internal/promotion, 트러블슈팅 #21).
 * 예전에는 공유 볼륨을 마운트해 경로로 직접 읽고 지웠는데, 그러면 두 서버를 다른 PC 로 나눌 수 없다.
 */
@Service
@RequiredArgsConstructor
public class AiPromotionFiles {

    private final RestTemplate restTemplate;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    private UriComponentsBuilder base(String type) {
        return UriComponentsBuilder.fromUriString(pythonApiUrl.replaceAll("/+$", ""))
                .pathSegment("internal", "promotion", type);
    }

    private URI uri(String type, String filename) {
        return base(type).pathSegment(filename).build().encode().toUri();
    }

    /** {type}_ 로 시작하는 .png 목록, 파일명 순 */
    public List<String> list(String type) {
        URI u = base(type).queryParam("prefix", type + "_").queryParam("ext", ".png").build().encode().toUri();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> body = restTemplate.getForObject(u, Map.class);
        return body == null || body.get("files") == null ? List.of() : body.get("files");
    }

    /** 받아서 dest 에 저장. AI 에 없으면 false */
    public boolean download(String type, String filename, Path dest) {
        try {
            return Boolean.TRUE.equals(restTemplate.execute(uri(type, filename), HttpMethod.GET, null, res -> {
                Files.copy(res.getBody(), dest, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }));
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /** 브라우저 응답으로 그대로 흘려보낸다 (/files/ 토큰의 AI 원본 이미지) */
    public void stream(String type, String filename, Consumer<String> contentType, OutputStream out) throws IOException {
        restTemplate.execute(uri(type, filename), HttpMethod.GET, null, res -> {
            String t = res.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (t != null) {
                contentType.accept(t);
            }
            StreamUtils.copy(res.getBody(), out);
            return null;
        });
    }

    /** 다 옮긴 원본을 지운다. 없으면 false */
    public boolean delete(String type, String filename) {
        try {
            restTemplate.delete(uri(type, filename));
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
