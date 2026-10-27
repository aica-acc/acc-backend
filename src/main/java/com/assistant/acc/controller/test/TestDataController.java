package com.assistant.acc.controller.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * test 계정(및 관리자)이 "다음 단계 버튼만 누르며" 써볼 수 있도록 샘플 기획서와 입력값을 제공한다.
 * 샘플은 test.samples-dir 아래의 samples.json (메타데이터) + 기획서 파일들로 구성된다.
 * SecurityFilterChain 에서 ROLE_TEST / ROLE_ADMIN 만 허용한다.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestDataController {

    private final ObjectMapper objectMapper;

    @Value("${test.samples-dir:./data/samples/proposals}")
    private String samplesDir;

    @SuppressWarnings("unchecked")
    @GetMapping("/samples")
    public List<Map<String, Object>> samples() throws IOException {
        Path manifest = Paths.get(samplesDir).resolve("samples.json");
        if (!Files.isRegularFile(manifest)) {
            return List.of();
        }
        return objectMapper.readValue(Files.readString(manifest, StandardCharsets.UTF_8), List.class);
    }

    @GetMapping("/samples/{id}/file")
    public ResponseEntity<Resource> sampleFile(@PathVariable String id) throws IOException {
        for (Map<String, Object> s : samples()) {
            if (id.equals(String.valueOf(s.get("id")))) {
                Path base = Paths.get(samplesDir).toAbsolutePath().normalize();
                Path file = base.resolve(String.valueOf(s.get("file"))).normalize();
                // samples.json 이 가리키는 파일이 샘플 폴더 밖이면 거부 (경로 이탈 방지)
                if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "샘플 파일을 찾을 수 없습니다.");
                }
                String name = file.getFileName().toString();
                MediaType type = name.toLowerCase().endsWith(".pdf")
                        ? MediaType.APPLICATION_PDF
                        : MediaType.APPLICATION_OCTET_STREAM;
                return ResponseEntity.ok()
                        .contentType(type)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                        .body(new FileSystemResource(file));
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "샘플을 찾을 수 없습니다.");
    }
}
