package com.assistant.acc.controller.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 서버 전용 내부 파일 API (트러블슈팅 #33).
 *
 * AI 가 파생물·영상을 만들 때 베이스 포스터·마스코트를 받아 가고(GET), 만든 결과를 올려 보낸다(PUT).
 * 예전에는 두 서버가 같은 폴더(front-public)를 마운트하고 경로를 주고받아, 서버를 나눌 수 없었다.
 *
 * - 경로는 data/promotion/ 아래만 (DB 에 저장하는 웹 경로와 같은 형식)
 * - INTERNAL_TOKEN 이 같아야 한다. 비어 있으면 이 API 는 꺼진다 (공유 폴더 모드)
 * - nginx 는 /internal 을 중계하지 않는다 (브라우저에서 닿지 않음)
 */
@Slf4j
@RestController
public class InternalFileController {

    private static final String PREFIX = "data/promotion/";

    @Value("${react.base-path}")
    private String reactBasePath;

    @Value("${internal.token:}")
    private String internalToken;

    private void checkToken(String token) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        byte[] a = internalToken.getBytes(StandardCharsets.UTF_8);
        byte[] b = (token == null ? "" : token).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Path resolve(String path) {
        String p = path == null ? "" : path.replace('\\', '/');
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (!p.startsWith(PREFIX) || p.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid path");
        }
        Path root = Paths.get(reactBasePath, "public").toAbsolutePath().normalize();
        Path file = root.resolve(p).normalize();
        if (!file.startsWith(root.resolve(PREFIX))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid path");
        }
        return file;
    }

    @GetMapping("/internal/files")
    public ResponseEntity<Resource> get(@RequestParam("path") String path,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) throws IOException {
        checkToken(token);
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        String type = Files.probeContentType(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type != null ? type : "application/octet-stream"))
                .body(new FileSystemResource(file));
    }

    @PutMapping("/internal/files")
    public ResponseEntity<Void> put(@RequestParam("path") String path,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            HttpServletRequest request) throws IOException {
        checkToken(token);
        Path file = resolve(path);
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), ".upload-", ".part");
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
        log.info("[internal] AI 결과 저장: {}", path);
        return ResponseEntity.noContent().build();
    }
}
