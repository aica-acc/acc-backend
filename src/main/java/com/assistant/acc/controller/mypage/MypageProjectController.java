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
    }

    /** 프로젝트 상세 화면에서 사용할 개별 홍보물(파생물) 한 카드 */
    @Data
    public static class PromotionAssetDTO {
        private Integer assetId;   // promotion_path_no
        private String typeCode;   // db_file_type (poster, banner, bus...)
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
                (rs, rowNum) -> mapRowToProjectSummary(rs)
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
                    // ✅ 파일명 + typeCode로 한글 이름 결정
                    a.setTypeLabel(inferTypeLabel(rawPath, typeCode));

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

    // --------- 헬퍼: 타입 코드 -> 한글 이름 ---------

    // --------- 헬퍼: 대분류 타입 코드 -> 한글 이름(fallback 용) ---------
    private String toTypeLabel(String typeCode) {
        if (typeCode == null) return "";
        return switch (typeCode) {
            case "poster" -> "포스터";
            case "logo"   -> "로고";
            case "sign"   -> "표지판";
            case "goods"  -> "굿즈";
            case "banner" -> "배너";
            default       -> typeCode;
        };
    }

    // --------- 헬퍼: 파일 경로 + 타입 코드 -> 한글 이름 ---------
    private String inferTypeLabel(String dbFilePath, String typeCode) {
        if (dbFilePath == null || dbFilePath.isBlank()) {
            // 파일 경로가 없으면 대분류 이름만이라도 반환
            return toTypeLabel(typeCode);
        }

        // 윈도우 경로 대비해서 역슬래시를 슬래시로 통일
        String normalized = dbFilePath.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        String fileName = (lastSlash >= 0) ? normalized.substring(lastSlash + 1) : normalized;
        // 예: "logo_typography.png", "sign_parking.png", "goods_sticker.png"

        // 확장자 제거
        int dotIdx = fileName.lastIndexOf('.');
        String baseName = (dotIdx > 0) ? fileName.substring(0, dotIdx) : fileName;
        // 예: "logo_typography"

        // 🔽 여기서 원하는 규칙대로 매핑
        switch (baseName) {
            case "logo_typography":
                return "타이포그래피 로고";
            case "logo_illustration":
                return "일러스트 로고";

            case "sign_parking":
                return "주차장 표지판";
            case "sign_toilet":
                return "화장실 표지판";
            case "sign_welcome":
                return "웰컴 표지판";

            case "goods_sticker":
                return "스티커 굿즈";
            case "goods_key_ring":
                return "키링 굿즈";
            case "goods_emoticon":
                return "이모티콘 굿즈";

            // 필요하면 나중에 계속 추가하면 됨
            default:
                // 매칭 안 되면 대분류 이름(logo/sign/goods…)으로 fallback
                return toTypeLabel(typeCode);
        }
    }

}
