package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "reservation_team",
        indexes = {
                @Index(name = "idx_reservation_team_golf_course_tee_time", columnList = "golf_course_id, tee_time_id"),
                @Index(name = "idx_reservation_team_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationTeam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tee_time_id", nullable = false)
    private TeeTime teeTime;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "booker_name", length = 50)
    private String bookerName;

    @Column(name = "player_count", nullable = false)
    private Integer playerCount = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationTeamStatus status = ReservationTeamStatus.RESERVED;

    // 지정 캐디 요청 — 자동배정 최우선 조건
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designated_caddie_id")
    private Caddie designatedCaddie;

    @Column(name = "is_vip", nullable = false)
    private Boolean isVip = false;

    // 쉼표 구분 명단 (예: 홍길동,김철수)
    @Column(name = "player_names", length = 500)
    private String playerNames;

    @Column(length = 500)
    private String memo;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static ReservationTeam create(GolfCourse golfCourse, TeeTime teeTime,
                                          String teamName, String bookerName, int playerCount, String memo) {
        ReservationTeam team = new ReservationTeam();
        team.golfCourse = golfCourse;
        team.teeTime = teeTime;
        team.teamName = teamName;
        team.bookerName = bookerName;
        team.playerCount = playerCount;
        team.memo = memo;
        team.status = ReservationTeamStatus.RESERVED;
        return team;
    }

    public void update(String teamName, int playerCount, String memo, String playerNames) {
        this.teamName = teamName;
        this.playerCount = playerCount;
        this.memo = memo;
        this.playerNames = playerNames;
    }

    public void cancel() {
        this.status = ReservationTeamStatus.CANCELLED;
    }

    public void noShow() {
        this.status = ReservationTeamStatus.NO_SHOW;
    }

    public void rainCancel() {
        this.status = ReservationTeamStatus.RAIN_CANCELLED;
    }

    public void complete() {
        this.status = ReservationTeamStatus.COMPLETED;
    }

    public void setDesignatedCaddie(Caddie caddie) {
        this.designatedCaddie = caddie;
    }

    public void updateVip(boolean isVip, String memo) {
        this.isVip = isVip;
        this.memo = memo;
    }

    public void changeTeeTime(TeeTime newTeeTime) {
        this.teeTime = newTeeTime;
    }
}
