package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "caddie",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_caddie_golf_course_number", columnNames = {"golf_course_id", "caddie_number"})
        },
        indexes = {
                @Index(name = "idx_caddie_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Caddie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    // Caddy 계정 연동 시 설정. 가입 승인 전까지 NULL 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "caddie_number", nullable = false, length = 20)
    private String caddieNumber;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaddieStatus status = CaddieStatus.ACTIVE;

    // null이면 골프장 기본 HOUSE 그룹으로 처리 — 그룹 관리 이전 캐디 데이터 호환
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_group_id")
    private CaddieGroup caddieGroup;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 캐디 가입 승인 시 user 계정과 연동하여 자동 생성
    public static Caddie createOnApproval(GolfCourse golfCourse, User user, String name) {
        Caddie caddie = new Caddie();
        caddie.golfCourse = golfCourse;
        caddie.user = user;
        caddie.name = name;
        // 승인 자동 생성 캐디는 번호 미정 상태다. 빈 문자열은 UNIQUE(golf_course_id, caddie_number)에 걸려
        // 골프장당 1명만 승인 가능해지므로, userId 기반 전역 고유 임시번호를 부여하고 Manager가 이후 보완한다.
        // (테스트 픽스처는 user=null로 생성 후 번호를 직접 지정하므로 null-safe 처리)
        caddie.caddieNumber = (user != null) ? "TEMP-" + user.getId() : "";
        caddie.status = CaddieStatus.ACTIVE;
        caddie.isDeleted = false;
        return caddie;
    }

    // 계정 없는 캐디 직접 등록 (FR-301) — 계정은 이후 계정 연동 API로 연결
    public static Caddie createDirect(GolfCourse golfCourse, String caddieNumber, String name,
                                      String phone, LocalDate hireDate) {
        Caddie caddie = new Caddie();
        caddie.golfCourse = golfCourse;
        caddie.user = null;
        caddie.caddieNumber = caddieNumber;
        caddie.name = name;
        caddie.phone = phone;
        caddie.hireDate = hireDate;
        caddie.status = CaddieStatus.ACTIVE;
        caddie.isDeleted = false;
        return caddie;
    }

    // 캐디-계정 연동 (FR-306) — 캐디 모바일 로그인 식별 기준
    public void linkAccount(User user) {
        this.user = user;
    }

    public void updateInfo(String caddieNumber, String phone, LocalDate hireDate) {
        this.caddieNumber = caddieNumber;
        this.phone = phone;
        this.hireDate = hireDate;
    }

    public void assignGroup(CaddieGroup caddieGroup) {
        this.caddieGroup = caddieGroup;
    }

    public void changeStatus(CaddieStatus status) {
        this.status = status;
    }

    // 퇴사 처리 — user 계정은 AuthService에서 별도로 WITHDRAWN 처리
    public void resign() {
        this.status = CaddieStatus.RESIGNED;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
