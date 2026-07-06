package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AssignCaddieGroupReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.CreateCaddieGroupReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateCaddieGroupReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieGroupRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieGroupRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CaddieGroupService {

    private final CaddieGroupRepository caddieGroupRepository;
    private final CaddieRepository caddieRepository;
    private final GolfCourseRepository golfCourseRepository;

    // ADR-005: 그룹 목록 조회 — 골프장에 그룹이 하나도 없으면 기본 하우스 그룹을 lazy 생성한다.
    public List<CaddieGroupRes> getGroups(AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();

        if (!caddieGroupRepository.existsByGolfCourse_IdAndIsDeletedFalse(golfCourseId)) {
            GolfCourse golfCourse = findGolfCourse(golfCourseId);
            caddieGroupRepository.save(CaddieGroup.createDefault(golfCourse));
        }

        return caddieGroupRepository.findByGolfCourse_IdAndIsDeletedFalseOrderByAssignmentTypeAscNameAsc(golfCourseId)
                .stream()
                .map(group -> CaddieGroupRes.from(group,
                        caddieRepository.countByCaddieGroup_IdAndIsDeletedFalse(group.getId())))
                .toList();
    }

    public CaddieGroupRes create(CreateCaddieGroupReq req, AuthenticatedUser auth) {
        validateManager(auth);
        GolfCourse golfCourse = findGolfCourse(auth.getGolfCourseId());
        CaddieGroup group = caddieGroupRepository.save(
                CaddieGroup.create(golfCourse, req.name(), req.assignmentType()));
        return CaddieGroupRes.from(group, 0);
    }

    public CaddieGroupRes update(Long groupId, UpdateCaddieGroupReq req, AuthenticatedUser auth) {
        validateManager(auth);
        CaddieGroup group = findGroup(groupId, auth);
        group.update(req.name(), req.assignmentType());
        return CaddieGroupRes.from(group,
                caddieRepository.countByCaddieGroup_IdAndIsDeletedFalse(groupId));
    }

    // 소속 캐디가 남아 있으면 삭제를 거부한다 — 순번 이월/자동배정 기준이 사라지는 것을 방지.
    public void delete(Long groupId, AuthenticatedUser auth) {
        validateManager(auth);
        CaddieGroup group = findGroup(groupId, auth);
        if (caddieRepository.countByCaddieGroup_IdAndIsDeletedFalse(groupId) > 0) {
            throw new BusinessException(CaddieErrorCode.CADDIE_GROUP_HAS_CADDIES);
        }
        group.delete();
    }

    // 캐디 그룹 지정/해제 — groupId가 null이면 해제(자동배정 시 HOUSE 취급)
    public CaddieRes assignCaddieGroup(Long caddieId, AssignCaddieGroupReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        CaddieGroup group = req.groupId() != null ? findGroup(req.groupId(), auth) : null;
        caddie.assignGroup(group);
        return CaddieRes.from(caddie);
    }

    private CaddieGroup findGroup(Long groupId, AuthenticatedUser auth) {
        CaddieGroup group = caddieGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_GROUP_NOT_FOUND));
        validateGolfCourseAccess(group.getGolfCourse().getId(), auth);
        return group;
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long targetGolfCourseId, AuthenticatedUser auth) {
        if (!targetGolfCourseId.equals(auth.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
