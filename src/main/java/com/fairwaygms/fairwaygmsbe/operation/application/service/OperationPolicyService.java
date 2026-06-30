package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateSpecialDayReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateRainPolicyReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.RainPolicyRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.SpecialDayRes;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.RainCancellationPolicy;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.SpecialOperationDay;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.RainCancellationPolicyRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.SpecialOperationDayRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationPolicyService {

    private final SpecialOperationDayRepository specialDayRepository;
    private final RainCancellationPolicyRepository rainPolicyRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final GolfCourseContextResolver contextResolver;

    public SpecialDayRes createSpecialDay(CreateSpecialDayReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        if (specialDayRepository.existsByGolfCourse_IdAndOperationDateAndIsDeletedFalse(golfCourseId, req.operationDate())) {
            throw new BusinessException(OperationErrorCode.SPECIAL_DAY_ALREADY_EXISTS);
        }

        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        SpecialOperationDay specialDay = SpecialOperationDay.create(golfCourse, req.operationDate(), req.note());
        specialDayRepository.save(specialDay);

        return SpecialDayRes.from(specialDay);
    }

    @Transactional(readOnly = true)
    public List<SpecialDayRes> listSpecialDays(String yearMonth, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        return specialDayRepository
                .findByGolfCourse_IdAndOperationDateBetweenAndIsDeletedFalse(golfCourseId, from, to)
                .stream()
                .map(SpecialDayRes::from)
                .toList();
    }

    public void deleteSpecialDay(Long specialDayId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        SpecialOperationDay specialDay = specialDayRepository.findByIdAndIsDeletedFalse(specialDayId)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.SETTING_NOT_FOUND));
        if (!specialDay.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        specialDay.softDelete();
    }

    public RainPolicyRes upsertRainPolicy(UpdateRainPolicyReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        RainCancellationPolicy policy = rainPolicyRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId)
                .map(existing -> {
                    existing.updatePolicyType(req.policyType());
                    return existing;
                })
                .orElseGet(() -> {
                    GolfCourse golfCourse = findGolfCourse(golfCourseId);
                    return rainPolicyRepository.save(RainCancellationPolicy.create(golfCourse, req.policyType()));
                });

        return RainPolicyRes.from(policy);
    }

    @Transactional(readOnly = true)
    public RainPolicyRes getRainPolicy(AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        RainCancellationPolicy policy = rainPolicyRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.RAIN_POLICY_NOT_FOUND));
        return RainPolicyRes.from(policy);
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
