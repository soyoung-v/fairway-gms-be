package com.fairwaygms.fairwaygmsbe.notification.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uq_notification_setting_user", columnNames = "user_id")
})
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "is_assignment_enabled", nullable = false)
    private Boolean isAssignmentEnabled = true;

    @Column(name = "is_board_enabled", nullable = false)
    private Boolean isBoardEnabled = true;

    @Column(name = "is_swap_enabled", nullable = false)
    private Boolean isSwapEnabled = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static NotificationSetting createDefault(Long userId, Long golfCourseId) {
        NotificationSetting s = new NotificationSetting();
        s.userId = userId;
        s.golfCourseId = golfCourseId;
        s.isAssignmentEnabled = true;
        s.isBoardEnabled = true;
        s.isSwapEnabled = true;
        s.isDeleted = false;
        return s;
    }

    public void update(Boolean isAssignmentEnabled, Boolean isBoardEnabled, Boolean isSwapEnabled) {
        if (isAssignmentEnabled != null) this.isAssignmentEnabled = isAssignmentEnabled;
        if (isBoardEnabled != null) this.isBoardEnabled = isBoardEnabled;
        if (isSwapEnabled != null) this.isSwapEnabled = isSwapEnabled;
    }
}
