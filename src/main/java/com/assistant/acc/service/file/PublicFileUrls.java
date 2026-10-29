package com.assistant.acc.service.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.assistant.acc.security.UrlCipher;

import lombok.RequiredArgsConstructor;

/**
 * 브라우저용 파일 주소 변환.
 *
 * 서버 안에서는 지금처럼 경로(/data/promotion/{회원}/{프로젝트}/..., AI 의 /static/..., 절대경로)를 쓰고,
 * 브라우저에는 /files/{토큰}.{확장자} 만 보낸다. 토큰은 UrlCipher 로 암호화한 "종류:경로" 라서
 * 회원번호·프로젝트 번호·서버 폴더 구조가 드러나지 않고, 번호를 바꿔 남의 파일을 추측할 수 없다.
 *
 *  - toPublic: 응답 문자열 속 내부 경로 → /files/토큰  (PublicPathResponseAdvice)
 *  - toInternal: 요청 문자열 속 /files/토큰 → 내부 웹 경로  (PublicPathRequestAdvice)
 *  - resolve: 토큰 → 실제 파일 위치  (PublicFileController)
 */
@Component
@RequiredArgsConstructor
public class PublicFileUrls {

    public static final String PREFIX = "/files/";

    // 경로를 끝내는 문자 (공백, 따옴표, JSON 이스케이프, 괄호, 쉼표, 쿼리)
    private static final String STOP = "\\s\"'<>\\\\(),?#";

    /** 생성물 표식: 절대경로(/data/acc-front/public/data/promotion/..., C:/.../public/data/promotion/...)와 웹 경로 모두 */
    private static final String PROMOTION = "/data/promotion/";
    private static final String STOP_CHARS = " \t\r\n\"'<>\\(),?#";
    /** AI 정적 파일: /static/... 또는 http://호스트/static/... */
    private static final Pattern AI_STATIC = Pattern.compile(
            "(?:https?://[^" + STOP + "/]+|(?<![\\w.\\-/]))/static/([^" + STOP + "]+)");
    /** AI 원본 이미지 (/poster-images/...) */
    private static final Pattern POSTER_IMAGES = Pattern.compile(
            "(?:https?://[^" + STOP + "/]+|(?<![\\w.\\-/]))/poster-images/([^" + STOP + "]+)");
    /** 브라우저가 돌려보낸 파일 주소 */
    private static final Pattern PUBLIC = Pattern.compile(
            "(?:https?://[^" + STOP + "/]+)?/files/([A-Za-z0-9_-]{24,})(?:\\.[A-Za-z0-9]{1,5})?");

    // 남은 내부 정보 (오류 메시지 등): 내부 호스트 주소, 서버 절대경로, 윈도우 경로
    private static final Pattern INTERNAL_URL = Pattern.compile(
            "https?://(?:ai|backend|db|localhost|127\\.0\\.0\\.1)(?::\\d+)?[^\\s\"'<>\\\\]*");
    private static final Pattern SERVER_PATH = Pattern.compile(
            "(?<![\\w/.])/(?:data/acc[\\w-]*|app/app|srv|workspace)(?:/[^\\s\"'<>\\\\]*)?");
    private static final Pattern WINDOWS_PATH = Pattern.compile(
            "(?<![\\w])[A-Za-z]:[\\\\/][^\\s\"'<>]*");
    private static final Pattern EXT = Pattern.compile("\\.([A-Za-z0-9]{1,5})$");

    private final UrlCipher cipher;

    @Value("${react.base-path}")
    private String reactBasePath;

    /** 토큰이 가리키는 파일 */
    public record Target(char kind, String path) {
        public boolean isAiStatic() {
            return kind == 's';
        }

        /** AI 가 만든 원본 이미지 ({type}/{파일}) — AI 내부 파일 API 에서 받아 전달 (#21) */
        public boolean isAiPromotion() {
            return kind == 'i';
        }
    }

    // ── 응답 ────────────────────────────────────────────

    /** 문자열 속 내부 경로를 /files/토큰 으로 바꾸고, 남은 내부 주소·서버 경로는 지운다 */
    public String toPublic(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // AI 가 앞의 "/" 없이 "static/...", "data/promotion/..." 로 주는 값도 있다 (배너 트렌드 이미지, 리플렛·마스코트 영상 등)
        String out = text.startsWith("static/") || text.startsWith("data/promotion/") ? "/" + text : text;
        if (out.contains(PROMOTION)) {
            out = replacePromotion(out);
        }
        if (out.contains("/static/")) {
            out = replace(out, AI_STATIC, m -> url('s', m.group(1)));
        }
        if (out.contains("/poster-images/")) {
            out = replace(out, POSTER_IMAGES, m -> url('i', m.group(1)));
        }
        if (out.contains("://")) {
            out = replace(out, INTERNAL_URL, m -> "(내부 서버)");
        }
        if (out.contains("/")) {
            out = replace(out, SERVER_PATH, m -> "(서버 경로)");
        }
        if (out.contains(":\\") || out.contains(":/")) {
            out = replace(out, WINDOWS_PATH, m -> "(서버 경로)");
        }
        return out;
    }

    /**
     * 표식 앞뒤로 경로 경계(공백·따옴표 등)까지 넓혀 통째로 바꾼다.
     * 정규식의 앞쪽 와일드카드는 긴 문자열(base64 이미지 등)에서 매우 느려져 직접 찾는다.
     */
    private String replacePromotion(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        int last = 0;
        int i = text.indexOf(PROMOTION);
        while (i >= 0) {
            int start = i;
            while (start > last && STOP_CHARS.indexOf(text.charAt(start - 1)) < 0) {
                start--;
            }
            int end = i + PROMOTION.length();
            while (end < text.length() && STOP_CHARS.indexOf(text.charAt(end)) < 0) {
                end++;
            }
            String rest = text.substring(i + PROMOTION.length(), end);
            if (!rest.isEmpty()) {
                sb.append(text, last, start).append(url('p', rest));
                last = end;
            }
            i = text.indexOf(PROMOTION, Math.max(end, i + 1));
        }
        return sb.append(text.substring(last)).toString();
    }

    private String url(char kind, String path) {
        Matcher ext = EXT.matcher(path);
        return PREFIX + cipher.encrypt(kind + ":" + path) + (ext.find() ? "." + ext.group(1).toLowerCase() : "");
    }

    // ── 요청 ────────────────────────────────────────────

    /** 문자열 속 /files/토큰 을 서버가 쓰던 웹 경로로 되돌린다. 풀리지 않는 토큰은 그대로 둔다 */
    public String toInternal(String text) {
        if (text == null || !text.contains(PREFIX)) {
            return text;
        }
        return replace(text, PUBLIC, m -> {
            Target t = decode(m.group(1));
            if (t == null) {
                return m.group();
            }
            return switch (t.kind()) {
                case 'p' -> "/data/promotion/" + t.path();
                case 's' -> "/static/" + t.path();
                default -> "/poster-images/" + t.path();
            };
        });
    }

    // ── 파일 제공 ────────────────────────────────────────

    public Target decode(String token) {
        String plain = cipher.decrypt(token);
        if (plain == null || plain.length() < 3 || plain.charAt(1) != ':') {
            return null;
        }
        String path = plain.substring(2);
        if (path.contains("..") || path.contains("\\") || path.startsWith("/")) {
            return null;
        }
        char kind = plain.charAt(0);
        return kind == 'p' || kind == 's' || kind == 'i' ? new Target(kind, path) : null;
    }

    /** 디스크 파일 위치 (AI 파일은 null — 컨트롤러가 AI 에서 받아 전달) */
    public Path fileOf(Target t) {
        Path root = switch (t.kind()) {
            case 'p' -> Paths.get(reactBasePath, "public", "data", "promotion");
            default -> null;
        };
        if (root == null) {
            return null;
        }
        root = root.toAbsolutePath().normalize();
        Path file = root.resolve(t.path()).normalize();
        return file.startsWith(root) ? file : null;
    }

    private static String replace(String text, Pattern p, Function<Matcher, String> fn) {
        Matcher m = p.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        do {
            m.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(m)));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }
}
