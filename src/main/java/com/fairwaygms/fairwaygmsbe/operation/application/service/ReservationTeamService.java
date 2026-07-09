package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.ChangeTeeTimeReq;
import java.time.LocalDate;
import java.util.List;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateReservationTeamReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.SetDesignatedCaddieReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateReservationTeamReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateVipReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.ReservationTeamDetailRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.ReservationTeamRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationTeamService {

    private final ReservationTeamRepository reservationTeamRepository;
    private final TeeTimeRepository teeTimeRepository;
    private final CaddieRepository caddieRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final GolfCourseContextResolver contextResolver;

    public ReservationTeamRes createTeam(CreateReservationTeamReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        TeeTime teeTime = teeTimeRepository.findByIdAndIsDeletedFalse(req.teeTimeId())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.TEE_TIME_NOT_FOUND));
        if (!teeTime.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        ReservationTeam team = ReservationTeam.create(golfCourse, teeTime,
                req.teamName(), req.bookerName(), req.playerCount(), req.memo());
        reservationTeamRepository.save(team);

        return ReservationTeamRes.from(team);
    }

    @Transactional(readOnly = true)
    public List<ReservationTeamRes> listTeams(LocalDate playDate, Long courseId, Integer periodNumber, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        return reservationTeamRepository.findByGolfCourseIdAndPlayDate(golfCourseId, playDate)
                .stream()
                .filter(t -> courseId == null || t.getTeeTime().getCourse().getId().equals(courseId))
                .filter(t -> periodNumber == null || t.getTeeTime().getOperationPeriod().getPeriodNumber().equals(periodNumber))
                .map(ReservationTeamRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationTeamDetailRes getTeam(Long teamId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return ReservationTeamDetailRes.from(team);
    }

    public ReservationTeamRes updateTeam(Long teamId, UpdateReservationTeamReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        team.update(req.teamName(), req.playerCount(), req.memo(), req.playerNames());
        return ReservationTeamRes.from(team);
    }

    public void cancelTeam(Long teamId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (team.getStatus() != ReservationTeamStatus.RESERVED) {
            throw new BusinessException(OperationErrorCode.INVALID_TEAM_STATUS);
        }

        team.cancel();
    }

    public void noShow(Long teamId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (team.getStatus() != ReservationTeamStatus.RESERVED) {
            throw new BusinessException(OperationErrorCode.INVALID_TEAM_STATUS);
        }

        team.noShow();
    }

    public void rainCancel(Long teamId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (team.getStatus() != ReservationTeamStatus.RESERVED) {
            throw new BusinessException(OperationErrorCode.INVALID_TEAM_STATUS);
        }

        team.rainCancel();
    }

    public void complete(Long teamId, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (team.getStatus() != ReservationTeamStatus.RESERVED) {
            throw new BusinessException(OperationErrorCode.INVALID_TEAM_STATUS);
        }

        team.complete();
    }

    public ReservationTeamRes setDesignatedCaddie(Long teamId, SetDesignatedCaddieReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(req.caddieId())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.CADDIE_NOT_FOUND));

        team.setDesignatedCaddie(caddie);
        return ReservationTeamRes.from(team);
    }

    public ReservationTeamRes updateVip(Long teamId, UpdateVipReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        team.updateVip(req.isVip(), req.memo());
        return ReservationTeamRes.from(team);
    }

    public ReservationTeamRes changeTeeTime(Long teamId, ChangeTeeTimeReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);

        ReservationTeam team = findTeam(teamId);
        if (!team.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (team.getStatus() != ReservationTeamStatus.RESERVED) {
            throw new BusinessException(OperationErrorCode.INVALID_TEAM_STATUS);
        }

        TeeTime newTeeTime = teeTimeRepository.findByIdAndIsDeletedFalse(req.newTeeTimeId())
                .orElseThrow(() -> new BusinessException(OperationErrorCode.TEE_TIME_NOT_FOUND));
        if (!newTeeTime.getGolfCourse().getId().equals(golfCourseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        team.changeTeeTime(newTeeTime);
        return ReservationTeamRes.from(team);
    }

    private void validateManager(AuthenticatedUser auth) {
        if (!auth.isManager() && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private ReservationTeam findTeam(Long teamId) {
        return reservationTeamRepository.findByIdAndIsDeletedFalse(teamId)
                .orElseThrow(() -> new BusinessException(OperationErrorCode.TEAM_NOT_FOUND));
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }
}
