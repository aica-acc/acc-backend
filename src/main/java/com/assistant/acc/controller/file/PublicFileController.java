package com.assistant.acc.controller.file;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.assistant.acc.service.file.AiPromotionFiles;
import com.assistant.acc.service.file.PublicFileUrls;
import com.assistant.acc.service.file.PublicFileUrls.Target;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GET /files/{토큰}.{확장자} — 브라우저용 파일 주소 (PublicFileUrls).
 * <img src> 로 불러오므로 로그인 없이 열린다. 토큰은 서버만 만들 수 있고 추측할 수 없어 주소를 받은 사람만 볼 수 있다.
 * 예전 공개 경로(/data/, /static/, /poster-images/)는 막았다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicFileController {

    private static final String CACHE = "private, max-age=86400";

    private final PublicFileUrls urls;
    private final RestTemplate restTemplate;
    private final AiPromotionFiles aiFiles;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    /**
     * 서버에 있는 파일은 Resource 로 돌려준다: Spring 이 Range 요청을 206 으로 나눠 보내므로
     * 영상 넘기기가 되고, Range 응답이 없으면 재생하지 않는 Safari(iOS)에서도 재생된다.
     */
    @GetMapping("/files/{name:.+}")
    public ResponseEntity<Resource> file(@PathVariable String name, HttpServletResponse response) throws IOException {
        int dot = name.lastIndexOf('.');
        Target t = urls.decode(dot > 0 ? name.substring(0, dot) : name);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        if (t.isAiStatic()) {
            proxyAiStatic(t.path(), response);
            return null;
        }
        if (t.isAiPromotion()) {
            proxyAiPromotion(t.path(), response);
            return null;
        }
        Path file = urls.fileOf(t);
        if (file == null || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        String type = Files.probeContentType(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type != null ? type : "application/octet-stream"))
                .header(HttpHeaders.CACHE_CONTROL, CACHE)
                .body(new FileSystemResource(file));
    }

    private void proxyAiPromotion(String path, HttpServletResponse response) throws IOException {
        int slash = path.indexOf('/');
        if (slash <= 0 || path.indexOf('/', slash + 1) >= 0) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        try {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE);
            aiFiles.stream(path.substring(0, slash), path.substring(slash + 1), response::setContentType, response.getOutputStream());
        } catch (HttpStatusCodeException e) {
            response.sendError(e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("AI 원본 이미지 중계 실패: {} ({})", path, e.getMessage());
            response.sendError(HttpStatus.BAD_GATEWAY.value());
        }
    }

    private void proxyAiStatic(String path, HttpServletResponse response) throws IOException {
        // 프로젝트 기록·리포트 같은 JSON/텍스트는 화면용 파일이 아니다
        if (path.startsWith("editor/projects/") || path.endsWith(".json") || path.endsWith(".csv") || path.endsWith(".txt")) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        // 이미 인코딩된 경로(%XX)는 그대로 쓴다 (이중 인코딩 방지)
        String encoded = path.contains("%") ? path : Arrays.stream(path.split("/"))
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        URI target = URI.create(pythonApiUrl.replaceAll("/+$", "") + "/static/" + encoded);
        try {
            restTemplate.execute(target, HttpMethod.GET, null, upstream -> {
                response.setStatus(upstream.getStatusCode().value());
                String type = upstream.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (type != null) {
                    response.setContentType(type);
                }
                response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE);
                StreamUtils.copy(upstream.getBody(), response.getOutputStream());
                return null;
            });
        } catch (HttpStatusCodeException e) {
            response.sendError(e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("AI 정적 파일 중계 실패: {} ({})", path, e.getMessage());
            response.sendError(HttpStatus.BAD_GATEWAY.value());
        }
    }
}
