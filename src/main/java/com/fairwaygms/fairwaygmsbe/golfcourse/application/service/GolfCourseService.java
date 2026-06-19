package com.fairwaygms.fairwaygmsbe.golfcourse.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.CreateCartRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.CreateCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.CreateGolfCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateCartRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateCartStatusRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateGolfCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response.CartResponse;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response.CourseResponse;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response.GolfCourseResponse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartType;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.exception.GolfCourseErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

// 골프장/코스/카트 비즈니스 로직 전체를 담당하는 서비스
@Service
@RequiredArgsConstructor
@Transactional
public class GolfCourseService {

    // 유효한 홀 수: 9홀, 18홀, 27홀만 허용
    private static final Set<Integer> VALID_HOLE_COUNTS = Set.of(9, 18, 27);

    private final GolfCourseRepository golfCourseRepository;
    private final CourseRepository courseRepository;
    private final CartRepository cartRepository;

    // API-201: 골프장 등록 — Admin 전용
    public GolfCourseResponse createGolfCourse(CreateGolfCourseRequest request, AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        GolfCourse golfCourse = GolfCourse.create(request.name(), request.address(), request.phone());
        return GolfCourseResponse.from(golfCourseRepository.save(golfCourse));
    }

    // API-202: 골프장 수정 — Admin 전용
    public GolfCourseResponse updateGolfCourse(Long golfCourseId, UpdateGolfCourseRequest request, AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        golfCourse.update(request.name(), request.address(), request.phone());
        return GolfCourseResponse.from(golfCourse);
    }

    // API-203: 골프장 목록 조회 — Admin은 전체, Manager는 본인 소속 골프장만 반환
    @Transactional(readOnly = true)
    public List<GolfCourseResponse> listGolfCourses(AuthenticatedUser user) {
        if (user.isAdmin()) {
            return golfCourseRepository.findAllByIsDeletedFalse().stream()
                    .map(GolfCourseResponse::from)
                    .toList();
        }
        // MANAGER: 본인 소속 골프장 1건만 반환
        GolfCourse golfCourse = findGolfCourseById(user.getGolfCourseId());
        return List.of(GolfCourseResponse.from(golfCourse));
    }

    // API-204: 골프장 선택 — 존재/상태 검증 후 기본 정보 반환. Manager는 본인 골프장만 선택 가능.
    @Transactional(readOnly = true)
    public GolfCourseResponse selectGolfCourse(Long golfCourseId, AuthenticatedUser user) {
        // MANAGER는 본인 소속 골프장만 선택 가능
        if (user.isManager() && !golfCourseId.equals(user.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        return GolfCourseResponse.from(golfCourse);
    }

    // API-205: 코스 등록 — Admin, Manager 가능. Manager는 본인 골프장만 대상.
    public CourseResponse createCourse(Long golfCourseId, CreateCourseRequest request, AuthenticatedUser user) {
        validateGolfCourseAccess(user, golfCourseId);
        validateHoleCount(request.holeCount());
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        if (courseRepository.existsByGolfCourseAndNameAndIsDeletedFalse(golfCourse, request.name())) {
            throw new BusinessException(GolfCourseErrorCode.COURSE_NAME_DUPLICATED);
        }
        Course course = Course.create(golfCourse, request.name(), request.holeCount(), request.sortOrder());
        return CourseResponse.from(courseRepository.save(course));
    }

    // API-206: 코스 수정 — Admin, Manager 가능. Manager는 본인 골프장 코스만 수정 가능.
    public CourseResponse updateCourse(Long courseId, UpdateCourseRequest request, AuthenticatedUser user) {
        Course course = findCourseById(courseId);
        validateGolfCourseAccess(user, course.getGolfCourse().getId());
        validateHoleCount(request.holeCount());
        course.update(request.name(), request.holeCount(), request.sortOrder(), request.isActive());
        return CourseResponse.from(course);
    }

    // API-207: 코스 목록 조회 — sortOrder 오름차순 정렬
    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(Long golfCourseId, AuthenticatedUser user) {
        validateGolfCourseAccess(user, golfCourseId);
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        return courseRepository.findAllByGolfCourseAndIsDeletedFalse(golfCourse).stream()
                .sorted(Comparator.comparingInt(Course::getSortOrder))
                .map(CourseResponse::from)
                .toList();
    }

    // API-208: 카트 등록 — Admin, Manager 가능. Manager는 본인 골프장만 대상.
    public CartResponse createCart(Long golfCourseId, CreateCartRequest request, AuthenticatedUser user) {
        validateGolfCourseAccess(user, golfCourseId);
        CartType cartType = parseCartType(request.cartType());
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        if (cartRepository.existsByGolfCourseAndCartNumberAndIsDeletedFalse(golfCourse, request.cartNumber())) {
            throw new BusinessException(GolfCourseErrorCode.DUPLICATE_CART_NUMBER);
        }
        Cart cart = Cart.create(golfCourse, request.cartNumber(), cartType);
        return CartResponse.from(cartRepository.save(cart));
    }

    // API-209: 카트 목록 조회 — status 파라미터로 필터링 가능
    @Transactional(readOnly = true)
    public List<CartResponse> listCarts(Long golfCourseId, String statusParam, AuthenticatedUser user) {
        validateGolfCourseAccess(user, golfCourseId);
        GolfCourse golfCourse = findGolfCourseById(golfCourseId);
        List<Cart> carts;
        if (statusParam != null && !statusParam.isBlank()) {
            CartStatus statusFilter = parseCartStatus(statusParam);
            carts = cartRepository.findAllByGolfCourseAndStatusAndIsDeletedFalse(golfCourse, statusFilter);
        } else {
            carts = cartRepository.findAllByGolfCourseAndIsDeletedFalse(golfCourse);
        }
        return carts.stream().map(CartResponse::from).toList();
    }

    // API-210: 카트 수정 — 카트 번호 변경 시 중복 확인
    public CartResponse updateCart(Long cartId, UpdateCartRequest request, AuthenticatedUser user) {
        Cart cart = findCartById(cartId);
        validateGolfCourseAccess(user, cart.getGolfCourse().getId());
        CartType cartType = parseCartType(request.cartType());
        // 카트 번호가 변경되는 경우에만 중복 확인
        if (!request.cartNumber().equals(cart.getCartNumber())) {
            if (cartRepository.existsByGolfCourseAndCartNumberAndIsDeletedFalse(cart.getGolfCourse(), request.cartNumber())) {
                throw new BusinessException(GolfCourseErrorCode.DUPLICATE_CART_NUMBER);
            }
        }
        cart.update(request.cartNumber(), cartType, request.note());
        return CartResponse.from(cart);
    }

    // API-211: 카트 상태 변경 — Manager 전용
    public CartResponse updateCartStatus(Long cartId, UpdateCartStatusRequest request, AuthenticatedUser user) {
        if (!user.isManager()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Cart cart = findCartById(cartId);
        validateGolfCourseAccess(user, cart.getGolfCourse().getId());
        CartStatus status = parseCartStatus(request.status());
        cart.changeStatus(status);
        return CartResponse.from(cart);
    }

    // API-212: 카트 반납 — 배정 완료 이벤트 수신 시 Assignment 도메인에서 호출한다.
    public void returnCart(Long cartId) {
        Cart cart = findCartById(cartId);
        cart.markAvailable();
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private GolfCourse findGolfCourseById(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new BusinessException(GolfCourseErrorCode.COURSE_NOT_FOUND));
    }

    private Cart findCartById(Long cartId) {
        return cartRepository.findByIdAndIsDeletedFalse(cartId)
                .orElseThrow(() -> new BusinessException(GolfCourseErrorCode.CART_NOT_FOUND));
    }

    // ADMIN은 모든 골프장 접근 가능. MANAGER는 본인 소속 골프장만 허용.
    private void validateGolfCourseAccess(AuthenticatedUser user, Long golfCourseId) {
        if (user.isManager() && !golfCourseId.equals(user.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.GOLF_COURSE_FORBIDDEN);
        }
    }

    private void validateHoleCount(int holeCount) {
        if (!VALID_HOLE_COUNTS.contains(holeCount)) {
            throw new BusinessException(GolfCourseErrorCode.INVALID_HOLE_COUNT);
        }
    }

    private CartType parseCartType(String cartType) {
        try {
            return CartType.valueOf(cartType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(GolfCourseErrorCode.INVALID_STATUS,
                    "유효하지 않은 카트 타입입니다. 허용값: ELECTRIC, MANUAL");
        }
    }

    private CartStatus parseCartStatus(String status) {
        try {
            return CartStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(GolfCourseErrorCode.INVALID_STATUS,
                    "유효하지 않은 카트 상태입니다. 허용값: AVAILABLE, MAINTENANCE, DISABLED");
        }
    }
}
