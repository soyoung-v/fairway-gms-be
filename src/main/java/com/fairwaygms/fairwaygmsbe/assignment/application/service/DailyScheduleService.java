package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentCompletedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentConfirmedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CreateDailyScheduleReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.DailyScheduleRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.DailySchedule;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentChangeType;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.DailyScheduleStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentHistoryRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.DailyScheduleRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyScheduleService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentHistoryRepository assignmentHistoryRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 일별 배정표 생성 — DRAFT 상태로 시작, 날짜당 1건만 허용
    public DailyScheduleRes createDailySchedule(CreateDailyScheduleReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        if (dailyScheduleRepository.existsByGolfCourse_IdAndScheduleDateAndIsDeletedFalse(
                golfCourseId, req.scheduleDate())) {
            throw new BusinessException(AssignmentErrorCode.DAILY_SCHEDULE_ALREADY_EXISTS);
        }

        DailySchedule schedule = DailySchedule.create(golfCourse, req.scheduleDate());
        return DailyScheduleRes.from(dailyScheduleRepository.save(schedule));
    }

    // 배정표 조회
    @Transactional(readOnly = true)
    public DailyScheduleRes getDailySchedule(Long scheduleId, AuthenticatedUser auth) {
        validateManager(auth);
        DailySchedule schedule = findSchedule(scheduleId);
        validateGolfCourseAccess(schedule.getGolfCourse().getId(), auth);
        return DailyScheduleRes.from(schedule);
    }

    // 배정표 확정 — DRAFT → CONFIRMED, 모든 ASSIGNED 건 일괄 CONFIRMED 처리, 이벤트 발행
    public DailyScheduleRes confirmSchedule(Long scheduleId, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());
        DailySchedule schedule = findSchedule(scheduleId);
        validateGolfCourseAccess(schedule.getGolfCourse().getId(), auth);

        if (schedule.getStatus() == DailyScheduleStatus.CONFIRMED) {
            throw new BusinessException(AssignmentErrorCode.DAILY_SCHEDULE_ALREADY_CONFIRMED);
        }
        if (schedule.getStatus() == DailyScheduleStatus.COMPLETED) {
            throw new BusinessException(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS);
        }

        // 모든 ASSIGNED 상태 배정을 CONFIRMED로 일괄 처리
        List<Assignment> assignedList = assignmentRepository.findByGolfCourseAndDateAndStatus(
                schedule.getGolfCourse().getId(), schedule.getScheduleDate(), AssignmentStatus.ASSIGNED);
        for (Assignment a : assignedList) {
            a.confirm();
            assignmentHistoryRepository.save(AssignmentHistory.record(
                    a, schedule.getGolfCourse(), AssignmentChangeType.CONFIRM,
                    a.getCaddie(), a.getCaddie(), null, manager));
        }

        schedule.confirm(manager);

        // AFTER_COMMIT 리스너에서 알림·게시판 시간표 생성
        eventPublisher.publishEvent(new AssignmentConfirmedEvent(
                this, schedule.getGolfCourse().getId(), schedule.getScheduleDate(),
                schedule.getId(), manager.getId()));

        return DailyScheduleRes.from(schedule);
    }

    // 확정 취소 — CONFIRMED → DRAFT, 모든 CONFIRMED 건 ASSIGNED로 복귀
    public DailyScheduleRes cancelConfirmSchedule(Long scheduleId, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());
        DailySchedule schedule = findSchedule(scheduleId);
        validateGolfCourseAccess(schedule.getGolfCourse().getId(), auth);

        if (schedule.getStatus() != DailyScheduleStatus.CONFIRMED) {
            throw new BusinessException(AssignmentErrorCode.DAILY_SCHEDULE_NOT_CONFIRMED);
        }

        List<Assignment> confirmedList = assignmentRepository.findByGolfCourseAndDateAndStatus(
                schedule.getGolfCourse().getId(), schedule.getScheduleDate(), AssignmentStatus.CONFIRMED);
        for (Assignment a : confirmedList) {
            a.cancelConfirm();
            assignmentHistoryRepository.save(AssignmentHistory.record(
                    a, schedule.getGolfCourse(), AssignmentChangeType.CONFIRM_CANCEL,
                    a.getCaddie(), a.getCaddie(), null, manager));
        }

        schedule.cancelConfirm();
        return DailyScheduleRes.from(schedule);
    }

    // 배정표 완료 — CONFIRMED → COMPLETED, 정산 이벤트 발행
    public DailyScheduleRes completeDailySchedule(Long scheduleId, AuthenticatedUser auth) {
        validateManager(auth);
        DailySchedule schedule = findSchedule(scheduleId);
        validateGolfCourseAccess(schedule.getGolfCourse().getId(), auth);

        if (schedule.getStatus() != DailyScheduleStatus.CONFIRMED) {
            throw new BusinessException(AssignmentErrorCode.DAILY_SCHEDULE_NOT_CONFIRMED);
        }

        // 모든 배정을 COMPLETED 처리
        List<Assignment> confirmedList = assignmentRepository.findByGolfCourseAndDateAndStatus(
                schedule.getGolfCourse().getId(), schedule.getScheduleDate(), AssignmentStatus.CONFIRMED);
        for (Assignment a : confirmedList) {
            a.complete();
        }

        schedule.complete();

        eventPublisher.publishEvent(new AssignmentCompletedEvent(
                this, schedule.getGolfCourse().getId(), schedule.getScheduleDate(), schedule.getId()));

        return DailyScheduleRes.from(schedule);
    }

    private DailySchedule findSchedule(Long scheduleId) {
        return dailyScheduleRepository.findById(scheduleId)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.DAILY_SCHEDULE_NOT_FOUND));
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long resourceGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!resourceGolfCourseId.equals(auth.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
