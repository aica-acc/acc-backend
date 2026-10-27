package com.assistant.acc.config.web;

import java.util.Iterator;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.assistant.acc.service.file.PublicFileUrls;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.RequiredArgsConstructor;

/**
 * 모든 API 응답에서 내부 경로를 브라우저용 주소로 바꾼다 (PublicFileUrls.toPublic).
 * DTO 마다 고치지 않고 한 곳에서 처리해, 예전에 저장된 경로(DB, 진행 상태, AI 결과 JSON)도 같이 가려진다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PublicPathResponseAdvice implements ResponseBodyAdvice<Object> {

    private final PublicFileUrls urls;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body == null || body instanceof byte[] || body instanceof Resource) {
            return body;
        }
        if (body instanceof String s) {
            return urls.toPublic(s);
        }
        if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }
        JsonNode tree = objectMapper.valueToTree(body);
        return rewrite(tree);
    }

    private JsonNode rewrite(JsonNode node) {
        if (node.isTextual()) {
            String before = node.textValue();
            String after = urls.toPublic(before);
            return after.equals(before) ? node : TextNode.valueOf(after);
        }
        if (node instanceof ObjectNode obj) {
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                e.setValue(rewrite(e.getValue()));
            }
        } else if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, rewrite(arr.get(i)));
            }
        }
        return node;
    }
}
