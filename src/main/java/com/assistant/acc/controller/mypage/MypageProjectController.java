package com.assistant.acc.controller.mypage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
public class MypageProjectController {

    private final JdbcTemplate jdbcTemplate;

    public MypageProjectController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ----------------- 공통 DTO -----------------

    /** 프로젝트 목록(카드)에서 사용할 요약 정보 */
    @Data
    public static class ProjectSummaryDTO {
        private Integer projectId;           // project.p_no
        private String festivalName;         // proposal_metadata.title
        private LocalDate festivalStartDate; // proposal_metadata.festival_start_date
        private LocalDate festivalEndDate;   // proposal_metadata.festival_end_date
        private Integer promotionCount;      // promotion_path 개수
        private String thumbnailUrl;         // poster 이미지 URL (/data/promotion/... 형태)
    }

    /** 프로젝트 상세 화면에서 사용할 개별 홍보물(파생물) 한 카드 */
    @Data
    public static class PromotionAssetDTO {
        private Integer assetId;   // promotion_path_no
        private String typeCode;   // db_file_type (road_banner, logo_typography ...)
        private String typeLabel;  // 화면에 보여줄 이름 (도로용 현수막 등)
        private String imageUrl;   // /data/promotion/... 형태 (프론트에서 <img src>로 사용)
    }

    /** 프로젝트 상세 화면 전체 데이터 */
    @Data
    public static class ProjectDetailDTO {
        private Integer projectId;
        private String festivalName;
        private LocalDate festivalStartDate;
        private LocalDate festivalEndDate;
        private String location;       // proposal_metadata.location
        private Integer promotionCount;
        private List<PromotionAssetDTO> assets;
    }

    // ----------------- 프로젝트 목록 -----------------

    /**
     * 프로젝트 목록 조회
     * GET /api/mypage/projects?m_no=M000001
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectSummaryDTO>> getProjects(
            @RequestParam(name = "m_no", required = false) String mNo
    ) {
        // 람다에서 쓸 값은 한 번만 정해서 final 처럼 쓰기
        final String memberNo = (mNo == null || mNo.isBlank()) ? "M000001" : mNo;

        log.info("📂 [MypageProjectController] 프로젝트 목록 조회, m_no={}", memberNo);

        String sql = """
            SELECT
                p.p_no AS project_id,
                pm.title AS festival_name,
                pm.festival_start_date,
                pm.festival_end_date,
                COUNT(pp.promotion_path_no) AS promotion_count
            FROM project p
            JOIN proposal_metadata pm
              ON pm.p_no = p.p_no
            LEFT JOIN promotion_path pp
              ON pp.p_no = p.p_no
            WHERE p.m_no = ?
            GROUP BY
                p.p_no,
                pm.title,
                pm.festival_start_date,
                pm.festival_end_date
            ORDER BY p.p_no DESC
            """;

        List<ProjectSummaryDTO> list = jdbcTemplate.query(
                sql,
                ps -> ps.setString(1, memberNo),
                (rs, rowNum) -> {
                    ProjectSummaryDTO dto = mapRowToProjectSummary(rs);
                    
                    // 각 프로젝트의 poster 이미지 찾기 (db_file_type이 정확히 'poster'인 것만)
                    Integer projectId = dto.getProjectId();
                    String posterSql = """
                        SELECT db_file_path
                        FROM promotion_path
                        WHERE p_no = ?
                          AND db_file_type = 'poster'
                        ORDER BY promotion_path_no
                        LIMIT 1
                        """;
                    
                    List<String> posterPaths = jdbcTemplate.query(
                            posterSql,
                            ps -> ps.setInt(1, projectId),
                            (rs2, rowNum2) -> rs2.getString("db_file_path")
                    );
                    
                    // poster 이미지가 있으면 thumbnailUrl 설정
                    if (!posterPaths.isEmpty()) {
                        String rawPath = posterPaths.get(0);
                        dto.setThumbnailUrl(toWebPath(rawPath));
                    }
                    
                    return dto;
                }
        );

        return ResponseEntity.ok(list);
    }

    /** ResultSet → ProjectSummaryDTO 매핑 */
    private ProjectSummaryDTO mapRowToProjectSummary(ResultSet rs) throws SQLException {
        ProjectSummaryDTO dto = new ProjectSummaryDTO();
        dto.setProjectId(rs.getInt("project_id"));
        dto.setFestivalName(rs.getString("festival_name"));

        java.sql.Date start = rs.getDate("festival_start_date");
        java.sql.Date end = rs.getDate("festival_end_date");
        if (start != null) {
            dto.setFestivalStartDate(start.toLocalDate());
        }
        if (end != null) {
            dto.setFestivalEndDate(end.toLocalDate());
        }

        dto.setPromotionCount(rs.getInt("promotion_count"));
        return dto;
    }

    // ----------------- 프로젝트 상세 -----------------

    /**
     * 한 프로젝트에 대한 메타데이터 + 홍보물 목록 조회
     * GET /api/mypage/projects/{projectId}
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectDetailDTO> getProjectDetail(
            @PathVariable("projectId") Integer projectId
    ) {
        log.info("📂 [MypageProjectController] 프로젝트 상세 조회, p_no={}", projectId);

        // 1) 상단 정보 (축제명, 기간, 장소, 홍보물 개수)
        String headerSql = """
            SELECT
                p.p_no AS project_id,
                pm.title AS festival_name,
                pm.festival_start_date,
                pm.festival_end_date,
                pm.location,
                COUNT(pp.promotion_path_no) AS promotion_count
            FROM project p
            JOIN proposal_metadata pm
              ON pm.p_no = p.p_no
            LEFT JOIN promotion_path pp
              ON pp.p_no = p.p_no
            WHERE p.p_no = ?
            GROUP BY
                p.p_no,
                pm.title,
                pm.festival_start_date,
                pm.festival_end_date,
                pm.location
            """;

        List<ProjectDetailDTO> headerList = jdbcTemplate.query(
                headerSql,
                ps -> ps.setInt(1, projectId),
                (rs, rowNum) -> {
                    ProjectDetailDTO dto = new ProjectDetailDTO();
                    dto.setProjectId(rs.getInt("project_id"));
                    dto.setFestivalName(rs.getString("festival_name"));

                    java.sql.Date start = rs.getDate("festival_start_date");
                    java.sql.Date end = rs.getDate("festival_end_date");
                    if (start != null) {
                        dto.setFestivalStartDate(start.toLocalDate());
                    }
                    if (end != null) {
                        dto.setFestivalEndDate(end.toLocalDate());
                    }
                    dto.setLocation(rs.getString("location"));
                    dto.setPromotionCount(rs.getInt("promotion_count"));
                    return dto;
                }
        );

        if (headerList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProjectDetailDTO detail = headerList.get(0);

        // 2) 홍보물(파생물) 목록
        String assetSql = """
            SELECT
                promotion_path_no,
                db_file_type,
                db_file_path
            FROM promotion_path
            WHERE p_no = ?
            ORDER BY promotion_path_no
            """;

        List<PromotionAssetDTO> assets = jdbcTemplate.query(
                assetSql,
                ps -> ps.setInt(1, projectId),
                (rs, rowNum) -> {
                    PromotionAssetDTO a = new PromotionAssetDTO();
                    a.setAssetId(rs.getInt("promotion_path_no"));

                    String typeCode = rs.getString("db_file_type");
                    String rawPath = rs.getString("db_file_path");

                    a.setTypeCode(typeCode);
                    // ✅ 이제 db_file_type(=typeCode)만 보고 한글 이름 결정
                    a.setTypeLabel(toTypeLabel(typeCode));

                    a.setImageUrl(toWebPath(rawPath));
                    return a;
                }
        );

        detail.setAssets(assets);

        // promotionCount가 null이면 실제 개수로 세팅
        if (detail.getPromotionCount() == null) {
            detail.setPromotionCount(assets.size());
        }

        return ResponseEntity.ok(detail);
    }

    // --------- 헬퍼: 파일 경로 -> 웹 경로(/data/...) ---------

    private String toWebPath(String dbFilePath) {
        if (dbFilePath == null) return null;

        // 윈도우일 수 있으니 역슬래시를 슬래시로 통일
        String normalized = dbFilePath.replace("\\", "/");

        // acc-front/public 하위의 /data/... 만 잘라서 사용
        int idx = normalized.indexOf("/data/");
        if (idx >= 0) {
            return normalized.substring(idx); // 예: /data/promotion/...
        }
        // 이미 상대 경로로 들어있는 경우
        if (!normalized.startsWith("/")) {
            return "/" + normalized;
        }
        return normalized;
    }

    // --------- 헬퍼: db_file_type 코드 -> 한글 이름 ---------
    private String toTypeLabel(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) return "";

        return switch (typeCode) {
            // 세부 타입 (value, label 리스트 기준)
            case "sign_parking"       -> "주차 표지판";
            case "sign_welcome"       -> "입구 표지판";
            case "sign_toilet"        -> "화장실 표지판";

            case "mascot_video"       -> "마스코트 홍보영상";

            case "goods_sticker"      -> "스티커";
            case "goods_key_ring"     -> "키링";
            case "goods_emoticon"     -> "이모티콘";

            case "logo_illustration"  -> "로고 일러스트";
            case "logo_typography"    -> "로고 타이포그래피";

            case "poster_cardnews"    -> "안내 카드뉴스";
            case "poster_video"       -> "포스터 홍보영상";

            case "leaflet"            -> "리플렛";

            case "road_banner"        -> "도로용 현수막";
            case "streetlamp_banner"  -> "가로등 현수막";

            case "bus_road"           -> "버스 도로 광고";
            case "bus_shelter"        -> "버스정류장 광고";

            case "subway_inner"       -> "지하철 내부 광고";
            case "subway_light"       -> "지하철 조명광고";

            case "etc_video"          -> "축제 홍보영상";

            // 혹시 예전 데이터에서 쓰일 수 있는 대분류 코드들
            case "poster"             -> "포스터";
            case "logo"               -> "로고";
            case "sign"               -> "표지판";
            case "goods"              -> "굿즈";
            case "banner"             -> "배너";
            case "bus"                -> "버스";
            case "subway"             -> "지하철";
            case "video"              -> "영상";
            case "cardnews"           -> "카드뉴스";
            case "mascot"             -> "마스코트";    
            default                   -> typeCode; // 모르는 코드는 그대로 노출
        };
    }

}
