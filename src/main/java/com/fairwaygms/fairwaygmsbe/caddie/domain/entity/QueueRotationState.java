package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 캐디 그룹별 순번 이월 상태 — ADR-005 참조
// 자동배정 완료 후 마지막으로 배정된 캐디 기준으로 nextStartCaddie 갱신
// SWAP은 당일 queueNumber만 교환하므로 이 값에 영향을 주지 않음
@Getter
@Entity
@Table(
        name = "queue_rotation_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_queue_rotation_state_group",
                        columnNames = {"golf_course_id", "caddie_group_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueRotationState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_group_id", nullable = false)
    private CaddieGroup caddieGroup;

    // null이면 이 그룹의 첫 번째 캐디(번호 오름차순)부터 시작
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_start_caddie_id")
    private Caddie nextStartCaddie;

    public static QueueRotationState create(GolfCourse golfCourse, CaddieGroup caddieGroup) {
        QueueRotationState state = new QueueRotationState();
        state.golfCourse = golfCourse;
        state.caddieGroup = caddieGroup;
        state.nextStartCaddie = null;
        return state;
    }

    // 자동배정 완료 후 다음날 시작 캐디 설정 — SWAP 전 원래 캐디 ID 기준으로 호출
    public void updateNextStart(Caddie nextStartCaddie) {
        this.nextStartCaddie = nextStartCaddie;
    }

    // 순번 리셋 — Manager가 수동으로 1번부터 다시 시작할 때
    public void reset() {
        this.nextStartCaddie = null;
    }
}
