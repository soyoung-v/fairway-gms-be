package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.exception.GolfCourseErrorCode;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.BulkRegenerateTeeTimesReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateTeeTimeReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.GenerateTeeTimesReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.GenerateTeeTimesRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.TeeTimeRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeeTimeService {

    private final TeeTimeRepository teeTimeRepository;
    private final OperationSettingRepository settingRepository;
    private final OperationPeriodRepository periodRepository;
    private final ReservationTeamRepository reservationTeamRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final CourseRepository courseRepository;
    private final GolfCourseContextResolver contextResolver;

    public GenerateTeeTimesRes generateTeeTimes(GenerateTeeTimesReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        OperationSetting setting = settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, req.yearMonth())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));

        List<OperationPeriod> activePeriods = periodRepository
                .findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(setting.getId());

        if (req.courseIds() != null && !req.courseIds().isEmpty()) {
            activePeriods = activePeriods.stream()
                    .filter(p -> req.courseIds().contains(p.getCourse().getId()))
                    .toList();
        }

        YearMonth ym = YearMonth.parse(req.yearMonth());
        int generatedCount = 0;
        for (LocalDate date = ym.atDay(1); !date.isAfter(ym.atEndOfMonth()); date = date.plusDays(1)) {
            for (OperationPeriod period : activePeriods) {
                generatedCount += generateSlotsForPeriod(golfCourse, period, date);
            }
        }

        return new GenerateTeeTimesRes(generatedCount, req.yearMonth());
    }

    @Transactional(readOnly = true)
    public List<TeeTimeRes> listTeeTimes(LocalDate playDate, Long courseId, Integer periodNumber, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        List<TeeTime> teeTimes = teeTimeRepository.findByGolfCourse_IdAndPlayDateAndIsDeletedFalse(golfCourseId, playDate);

        return teeTimes.stream()
                .filter(tt -> courseId == null || tt.getCourse().getId().equals(courseId))
                .filter(tt -> periodNumber == null || tt.getOperationPeriod().getPeriodNumber().equals(periodNumber))
                .map(TeeTimeRes::from)
                .toList();
    }

    public TeeTimeRes addTeeTime(CreateTeeTimeReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Course course = courseRepository.findByIdAndIsDeletedFalse(req.courseId())
                .orElseThrow(() -> new BusinessException(GolfCourseErrorCode.COURSE_NOT_FOUND));

        if (teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                golfCourseId, req.courseId(), req.playDate(), req.startTime())) {
            throw new BusinessException(OperationErrorCode.DUPLICATE_TEE_TIME);
        }

        String yearMonth = YearMonth.from(req.playDate()).toString();
        OperationSetting setting = settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));

        OperationPeriod period = periodRepository.findByOperationSetting_IdAndCourse_IdAndPeriodNumberAndIsDeletedFalse(
                        setting.getId(), req.courseId(), req.periodNumber())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.PERIOD_NOT_FOUND));

        TeeTime teeTime = TeeTime.create(golfCourse, period, course, req.playDate(), req.startTime());
        teeTimeRepository.save(teeTime);

        return TeeTimeRes.from(teeTime);
    }

    public void closeTeeTime(Long teeTimeId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        TeeTime teeTime = teeTimeRepository.findByIdAndIsDeletedFalse(teeTimeId)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.TEE_TIME_NOT_FOUND));
        if (!teeTime.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        teeTime.close();
    }

    public GenerateTeeTimesRes bulkRegenerate(BulkRegenerateTeeTimesReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        // preserveTeams=false일 때 팀이 없는 티타임 삭제 후 재생성
        boolean deleteEmpty = Boolean.FALSE.equals(req.preserveTeams());
        if (deleteEmpty) {
            YearMonth ym = YearMonth.parse(req.yearMonth());
            List<TeeTime> existing = teeTimeRepository.findByGolfCourse_IdAndPlayDateBetweenAndIsDeletedFalse(
                    golfCourseId, ym.atDay(1), ym.atEndOfMonth());
            for (TeeTime tt : existing) {
                boolean hasTeams = !reservationTeamRepository.findByTeeTime_IdAndIsDeletedFalse(tt.getId()).isEmpty();
                if (!hasTeams) {
                    tt.softDelete();
                }
            }
        }

        OperationSetting setting = settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, req.yearMonth())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));

        List<OperationPeriod> activePeriods = periodRepository
                .findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(setting.getId());

        YearMonth ym = YearMonth.parse(req.yearMonth());
        int generatedCount = 0;
        for (LocalDate date = ym.atDay(1); !date.isAfter(ym.atEndOfMonth()); date = date.plusDays(1)) {
            for (OperationPeriod period : activePeriods) {
                generatedCount += generateSlotsForPeriod(golfCourse, period, date);
            }
        }

        return new GenerateTeeTimesRes(generatedCount, req.yearMonth());
    }

    private int generateSlotsForPeriod(GolfCourse golfCourse, OperationPeriod period, LocalDate date) {
        int count = 0;
        LocalTime slot = period.getStartTime();
        while (!slot.isAfter(period.getEndTime())) {
            if (!teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                    golfCourse.getId(), period.getCourse().getId(), date, slot)) {
                teeTimeRepository.save(TeeTime.create(golfCourse, period, period.getCourse(), date, slot));
                count++;
            }
            slot = slot.plusMinutes(period.getTeeTimeInterval());
        }
        return count;
    }

    private void validateManager(AuthenticatedUser auth) {
        if (!auth.isManager()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }
}
