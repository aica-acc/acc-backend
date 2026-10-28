package com.assistant.acc.service.credit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.domain.member.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 크레딧: 유료 AI 생성을 시작할 때 차감하고, 실패하면 돌려준다.
 * - 1 크레딧 ≈ 원가 10원. 기능별 차감량은 credit.cost.* 로 바꿀 수 있다 (기본값은 COSTS)
 * - 관리자는 차감하지 않는다. test 계정은 데모라 유료 생성 자체를 하지 않는다 (DemoGuard)
 * - 가입하면 credit.signup-bonus 만큼 지급 (전체 홍보물을 한 번 만들 수 있는 양)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    public static final String SIGNUP_BONUS = "SIGNUP_BONUS";
    public static final String ADMIN_GRANT = "ADMIN_GRANT";
    public static final String USE = "USE";
    public static final String REFUND = "REFUND";

    /** 기능별 기본 차감량 (실측 원가 기준, 1 크레딧 ≈ 10원) */
    public static final Map<String, Integer> COSTS = new LinkedHashMap<>();
    static {
        COSTS.put("analyze-proposal", 2);      // 기획서 분석 (gpt-4.1-mini)
        COSTS.put("trend", 10);                // 트렌드 분석 1회 (gpt-4o, 검색 API)
        COSTS.put("poster-images", 80);        // 포스터 4장 (nano-banana-pro)
        COSTS.put("mascot-images", 30);        // 마스코트 4장
        COSTS.put("regenerate", 20);           // 포스터 다시 만들기 1장
        COSTS.put("derivative-basic", 5);      // 로고·표지판·굿즈 (seedream-4)
        COSTS.put("derivative-banner", 10);    // 현수막·버스·지하철 (seedream-4 + 글자 지우기)
        COSTS.put("derivative-premium", 20);   // 리플렛·카드뉴스 (nano-banana-pro)
        COSTS.put("derivative-video", 60);     // 홍보 영상 15초 (seedance)
        COSTS.put("ai-render", 3);             // 에디터 AI 색상 추천 (gpt-4o)
        COSTS.put("report", 1);                // 리포트 1종 (프로젝트마다 처음 한 번)
        COSTS.put("live-poster", 50);          // 라이브 포스터 5초 1080p
    }

    private static final Set<String> BANNER_TYPES = Set.of(
            "road_banner", "bus_shelter", "subway_light", "bus_road", "streetlamp_banner", "subway_inner");
    private static final Set<String> PREMIUM_TYPES = Set.of("leaflet", "poster_cardnews");
    private static final Set<String> VIDEO_TYPES = Set.of("poster_video", "mascot_video", "etc_video");

    private final JdbcTemplate jdbc;
    private final Environment env;

    @Value("${credit.signup-bonus:500}")
    private int signupBonus;

    public int cost(String key) {
        Integer def = COSTS.get(key);
        if (def == null) {
            throw new IllegalArgumentException("unknown credit cost: " + key);
        }
        return env.getProperty("credit.cost." + key, Integer.class, def);
    }

    /** 파생물 한 종류의 차감량 (news 처럼 생성하지 않는 종류는 0) */
    public int derivativeCost(String type) {
        if ("news".equals(type)) {
            return 0;
        }
        if (BANNER_TYPES.contains(type)) {
            return cost("derivative-banner");
        }
        if (PREMIUM_TYPES.contains(type)) {
            return cost("derivative-premium");
        }
        if (VIDEO_TYPES.contains(type)) {
            return cost("derivative-video");
        }
        return cost("derivative-basic");
    }

    public int derivativeCost(Collection<String> types) {
        return types.stream().mapToInt(this::derivativeCost).sum();
    }

    /** 화면에 보여 줄 기능별 차감량 */
    public Map<String, Integer> costs() {
        Map<String, Integer> out = new LinkedHashMap<>();
        COSTS.keySet().forEach(k -> out.put(k, cost(k)));
        return out;
    }

    public int signupBonus() {
        return signupBonus;
    }

    public int balance(String mNo) {
        Integer v = jdbc.query("SELECT credit FROM member WHERE m_no = ?",
                rs -> rs.next() ? rs.getInt(1) : null, mNo);
        return v == null ? 0 : v;
    }

    /** 지급 (가입 기념, 관리자 지급). 음수면 회수 */
    @Transactional
    public int grant(String mNo, int amount, String reason, String memo) {
        jdbc.update("UPDATE member SET credit = GREATEST(credit + ?, 0) WHERE m_no = ?", amount, mNo);
        int balance = balance(mNo);
        record(mNo, amount, balance, reason, null, memo);
        log.info("[credit] 지급 mNo={}, amount={}, reason={}, balance={}", mNo, amount, reason, balance);
        return balance;
    }

    /** 가입 기념 크레딧 (일반 회원 가입 시 한 번). 소셜 로그인 가입도 이 메서드를 부르면 된다 */
    public void grantSignupBonus(Member m) {
        if (Member.ROLE_USER.equals(m.getRole()) && signupBonus > 0) {
            grant(m.getMNo(), signupBonus, SIGNUP_BONUS, "가입 기념");
        }
    }

    /**
     * 차감. 관리자이거나 0 이면 아무것도 하지 않는다. 모자라면 402.
     * 돌려받은 Charge 의 refund() 를 실패했을 때 부르면 한 번만 환불된다.
     */
    @Transactional
    public Charge charge(String mNo, String role, int amount, String what, Integer pNo) {
        if (amount <= 0 || Member.ROLE_ADMIN.equals(role)) {
            return Charge.NONE;
        }
        int updated = jdbc.update("UPDATE member SET credit = credit - ? WHERE m_no = ? AND credit >= ?",
                amount, mNo, amount);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "크레딧이 부족합니다. (필요 " + amount + ", 남은 크레딧 " + balance(mNo) + ")");
        }
        record(mNo, -amount, balance(mNo), USE, pNo, what);
        log.info("[credit] 차감 mNo={}, amount={}, what={}, pNo={}", mNo, amount, what, pNo);
        return new Charge(this, mNo, amount, what, pNo);
    }

    void refund(String mNo, int amount, String what, Integer pNo) {
        jdbc.update("UPDATE member SET credit = credit + ? WHERE m_no = ?", amount, mNo);
        record(mNo, amount, balance(mNo), REFUND, pNo, what + " 실패");
        log.info("[credit] 환불 mNo={}, amount={}, what={}, pNo={}", mNo, amount, what, pNo);
    }

    public List<Map<String, Object>> history(String mNo, int limit) {
        return jdbc.queryForList(
                "SELECT delta, balance, reason, p_no AS pNo, memo, created_at AS createdAt "
                        + "FROM credit_ledger WHERE m_no = ? ORDER BY ledger_no DESC LIMIT ?",
                mNo, limit);
    }

    private void record(String mNo, int delta, int balance, String reason, Integer pNo, String memo) {
        jdbc.update("INSERT INTO credit_ledger (m_no, delta, balance, reason, p_no, memo) VALUES (?, ?, ?, ?, ?, ?)",
                mNo, delta, balance, reason, pNo, memo);
    }

    /** 차감 한 건. 실패하면 refund() (여러 번 불러도 한 번만 환불) */
    public static final class Charge {
        static final Charge NONE = new Charge(null, null, 0, null, null);

        private final CreditService service;
        private final String mNo;
        private final int amount;
        private final String what;
        private final Integer pNo;
        private final AtomicBoolean refunded = new AtomicBoolean(false);

        Charge(CreditService service, String mNo, int amount, String what, Integer pNo) {
            this.service = service;
            this.mNo = mNo;
            this.amount = amount;
            this.what = what;
            this.pNo = pNo;
        }

        public void refund() {
            if (service != null && amount > 0 && refunded.compareAndSet(false, true)) {
                service.refund(mNo, amount, what, pNo);
            }
        }
    }
}
