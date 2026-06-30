package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 자동배정 순번 기준 원본 데이터.
// 동시성 제어는 비관적 락(SELECT FOR UPDATE)으로 보강 — Repository에서 @Lock 사용
@Getter
@Entity
// 소프트 삭제 구조와 DB 유니크 제약은 동시에 사용할 수 없다 (삭제된 행이 제약을 점유).
// 단건 활성 보장은 서비스 레이어(initializeQueues soft-delete 후 재삽입, adjustQueue 중복 확인)에서 수행한다.
@Table(
        name = "caddie_queue",
        indexes = {
                @Index(name = "idx_caddie_queue_golf_course_date", columnList = "golf_course_id, queue_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieQueue create(Caddie caddie, GolfCourse golfCourse, LocalDate queueDate, int queueNumber) {
        CaddieQueue queue = new CaddieQueue();
        queue.caddie = caddie;
        queue.golfCourse = golfCourse;
        queue.queueDate = queueDate;
        queue.queueNumber = queueNumber;
        queue.isDeleted = false;
        return queue;
    }

    public void adjustNumber(int newQueueNumber) {
        this.queueNumber = newQueueNumber;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
