package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDailyStatusRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.DashboardRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<DailyStatusType> CADDIE_EXCLUDE_TYPES =
            EnumSet.of(DailyStatusType.DAY_OFF, DailyStatusType.ABSENCE, DailyStatusType.ASSIGN_EXCLUDED);

    private final ReservationTeamRepository reservationTeamRepository;
    private final TeeTimeRepository teeTimeRepository;
    private final CaddieRepository caddieRepository;
    private final CaddieDailyStatusRepository caddieDailyStatusRepository;
    private final CartRepository cartRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final GolfCourseContextResolver contextResolver;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public DashboardRes getDashboard(LocalDate targetDate, AuthenticatedUser auth) {
        if (!auth.isAdmin() && !auth.isManager()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));

        long totalTeams = reservationTeamRepository.countByGolfCourse_IdAndStatusAndIsDeletedFalse(
                golfCourseId, ReservationTeamStatus.RESERVED);

        Set<Long> excludedCaddieIds = caddieDailyStatusRepository
                .findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(golfCourseId, targetDate)
                .stream()
                .filter(ds -> CADDIE_EXCLUDE_TYPES.contains(ds.getType()))
                .map(ds -> ds.getCaddie().getId())
                .collect(Collectors.toSet());

        long availableCaddies = caddieRepository
                .findByGolfCourse_IdAndStatusAndIsDeletedFalse(golfCourseId, CaddieStatus.ACTIVE)
                .stream()
                .filter(c -> !excludedCaddieIds.contains(c.getId()))
                .count();

        long availableCarts = cartRepository
                .findAllByGolfCourseAndStatusAndIsDeletedFalse(golfCourse, CartStatus.AVAILABLE)
                .size();

        long operatingCourses = teeTimeRepository
                .findByGolfCourse_IdAndPlayDateAndIsDeletedFalse(golfCourseId, targetDate)
                .stream()
                .filter(tt -> tt.getStatus() == TeeTimeStatus.OPEN)
                .map(tt -> tt.getCourse().getId())
                .distinct()
                .count();

        // FR-421: 당일 배정이 없는 RESERVED 팀 수 — AssignmentService.getUnassignedTeams와 동일 기준
        Set<Long> assignedTeamIds = assignmentRepository
                .findByGolfCourseAndDateWithDetails(golfCourseId, targetDate)
                .stream()
                .map(a -> a.getReservationTeam().getId())
                .collect(Collectors.toSet());

        long unassignedTeams = reservationTeamRepository
                .findByGolfCourseIdAndPlayDate(golfCourseId, targetDate)
                .stream()
                .filter(t -> t.getStatus() == ReservationTeamStatus.RESERVED)
                .filter(t -> !assignedTeamIds.contains(t.getId()))
                .count();

        return new DashboardRes(totalTeams, availableCaddies, availableCarts, unassignedTeams, operatingCourses);
    }
}
