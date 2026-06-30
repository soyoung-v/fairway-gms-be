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
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.PeriodReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdatePeriodReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.OperationSettingRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationSettingService {

    private final OperationSettingRepository settingRepository;
    private final OperationPeriodRepository periodRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final CourseRepository courseRepository;
    private final GolfCourseContextResolver contextResolver;

    public OperationSettingRes createSetting(CreateOperationSettingReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        if (settingRepository.existsByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, req.yearMonth())) {
            throw new BusinessException(OperationErrorCode.SETTING_ALREADY_EXISTS);
        }

        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        OperationSetting setting = OperationSetting.create(golfCourse, req.yearMonth());
        settingRepository.save(setting);

        List<OperationPeriod> periods = new ArrayList<>();
        for (PeriodReq pr : req.periods()) {
            Course course = courseRepository.findByIdAndIsDeletedFalse(pr.courseId())
                    .orElseThrow(() -> new BusinessException(GolfCourseErrorCode.COURSE_NOT_FOUND));
            OperationPeriod period = OperationPeriod.create(setting, golfCourse, course, pr.periodNumber(),
                    pr.startTime(), pr.endTime(), pr.teeTimeInterval());
            periods.add(periodRepository.save(period));
        }

        return OperationSettingRes.of(setting, periods);
    }

    @Transactional(readOnly = true)
    public OperationSettingRes getSetting(String yearMonth, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        OperationSetting setting = settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));

        List<OperationPeriod> periods = periodRepository.findByOperationSetting_IdAndIsDeletedFalse(setting.getId());
        return OperationSettingRes.of(setting, periods);
    }

    public OperationSettingRes updateSetting(Long settingId, UpdateOperationSettingReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        OperationSetting setting = findSetting(settingId);
        validateGolfCourseMatch(setting.getGolfCourse().getId(), golfCourseId);

        for (UpdatePeriodReq pr : req.periods()) {
            OperationPeriod period = periodRepository.findByIdAndIsDeletedFalse(pr.periodId())
                    .orElseThrow(() -> new BusinessException(OperationErrorCode.PERIOD_NOT_FOUND));
            period.update(pr.startTime(), pr.endTime(), pr.teeTimeInterval(), pr.isActive());
        }

        List<OperationPeriod> periods = periodRepository.findByOperationSetting_IdAndIsDeletedFalse(settingId);
        return OperationSettingRes.of(setting, periods);
    }

    private void validateManager(AuthenticatedUser auth) {
        if (!auth.isManager()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseMatch(Long settingGolfCourseId, Long resolvedGolfCourseId) {
        if (!settingGolfCourseId.equals(resolvedGolfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private OperationSetting findSetting(Long settingId) {
        return settingRepository.findByIdAndIsDeletedFalse(settingId)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }
}
