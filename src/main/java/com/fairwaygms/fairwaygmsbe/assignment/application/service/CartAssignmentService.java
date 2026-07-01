package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CartAssignReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CartAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartAssignmentService {

    private final CartAssignmentRepository cartAssignmentRepository;
    private final CartRepository cartRepository;
    private final TeeTimeRepository teeTimeRepository;
    private final GolfCourseRepository golfCourseRepository;

    // 카트-티타임 배정 — 같은 날짜+티타임에 동일 카트 중복 불가
    public CartAssignmentRes assignCart(CartAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Cart cart = cartRepository.findByIdAndIsDeletedFalse(req.cartId())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CART_NOT_FOUND));
        validateGolfCourseAccess(cart.getGolfCourse().getId(), auth);

        TeeTime teeTime = teeTimeRepository.findByIdAndIsDeletedFalse(req.teeTimeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // soft-delete 구조 보완 검증 (DB UNIQUE는 취소 행을 점유하므로 서비스 레이어에서 검증)
        if (cartAssignmentRepository.existsByCart_IdAndTeeTime_IdAndAssignmentDateAndIsDeletedFalse(
                req.cartId(), req.teeTimeId(), req.assignmentDate())) {
            throw new BusinessException(AssignmentErrorCode.CART_ALREADY_ASSIGNED);
        }

        CartAssignment cartAssignment = CartAssignment.create(golfCourse, cart, teeTime, req.assignmentDate());
        return CartAssignmentRes.from(cartAssignmentRepository.save(cartAssignment));
    }

    // 카트 반납 처리 — ASSIGNED → RETURNED 상태로 기록
    public CartAssignmentRes returnCart(Long cartAssignmentId, AuthenticatedUser auth) {
        validateManager(auth);
        CartAssignment cartAssignment = findCartAssignment(cartAssignmentId);
        validateGolfCourseAccess(cartAssignment.getGolfCourse().getId(), auth);

        cartAssignment.returnCart();
        return CartAssignmentRes.from(cartAssignment);
    }

    // 카트 배정 취소 — 소프트 삭제, 재배정 가능
    public void cancelCartAssignment(Long cartAssignmentId, AuthenticatedUser auth) {
        validateManager(auth);
        CartAssignment cartAssignment = findCartAssignment(cartAssignmentId);
        validateGolfCourseAccess(cartAssignment.getGolfCourse().getId(), auth);

        cartAssignment.cancel();
    }

    // 골프장+날짜 기준 카트 배정 목록 조회
    @Transactional(readOnly = true)
    public List<CartAssignmentRes> getCartAssignments(Long golfCourseId, LocalDate date, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = auth.isAdmin() ? golfCourseId : auth.getGolfCourseId();
        return cartAssignmentRepository.findByGolfCourseAndDate(targetId, date)
                .stream()
                .map(CartAssignmentRes::from)
                .toList();
    }

    private CartAssignment findCartAssignment(Long id) {
        return cartAssignmentRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CART_ASSIGNMENT_NOT_FOUND));
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

    private void validateGolfCourseAccess(Long resourceGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!resourceGolfCourseId.equals(auth.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
