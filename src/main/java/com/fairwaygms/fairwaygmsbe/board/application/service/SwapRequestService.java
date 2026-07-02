package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.SwapQueueReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentService;
import com.fairwaygms.fairwaygmsbe.board.application.event.SwapRequestProcessedEvent;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.RejectSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestSummaryRes;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.SwapRequest;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.SwapRequestRepository;
import com.fairwaygms.fairwaygmsbe.board.exception.BoardErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SwapRequestService {

    private final SwapRequestRepository swapRequestRepository;
    private final CaddieRepository caddieRepository;
    private final AssignmentService assignmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SwapRequestRes createSwapRequest(CreateSwapRequestReq req, AuthenticatedUser auth) {
        Caddie requester = caddieRepository.findByUser_IdAndIsDeletedFalse(auth.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (requester.getId().equals(req.targetCaddieId())) {
            throw new BusinessException(BoardErrorCode.SELF_SWAP_NOT_ALLOWED);
        }

        SwapRequest swapRequest = swapRequestRepository.save(
                SwapRequest.create(auth.getGolfCourseId(), requester.getId(),
                        req.targetCaddieId(), req.requestDate(), req.reason()));

        return SwapRequestRes.from(swapRequest);
    }

    // 매니저용: 골프장 내 요청 목록 조회 (상태 필터 선택)
    @Transactional(readOnly = true)
    public Page<SwapRequestSummaryRes> getSwapRequests(SwapRequestStatus status, int page, int size,
                                                        AuthenticatedUser auth) {
        Page<SwapRequest> requests = swapRequestRepository
                .findByGolfCourseAndStatus(auth.getGolfCourseId(), status, PageRequest.of(page, size));

        // 이름 조회를 위한 caddie ID 수집
        List<Long> caddieIds = requests.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getRequesterCaddieId(), r.getTargetCaddieId()))
                .distinct()
                .toList();

        Map<Long, String> nameMap = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(auth.getGolfCourseId())
                .stream()
                .filter(c -> caddieIds.contains(c.getId()))
                .collect(Collectors.toMap(Caddie::getId, Caddie::getName));

        return requests.map(r -> SwapRequestSummaryRes.of(r,
                nameMap.getOrDefault(r.getRequesterCaddieId(), ""),
                nameMap.getOrDefault(r.getTargetCaddieId(), "")));
    }

    @Transactional
    public void approveSwapRequest(Long requestId, AuthenticatedUser auth) {
        SwapRequest request = getActiveRequest(requestId, auth.getGolfCourseId());
        if (!request.isPending()) {
            throw new BusinessException(BoardErrorCode.SWAP_REQUEST_ALREADY_PROCESSED);
        }

        request.approve(auth.getUserId());

        // 큐 순번 교환 실행
        assignmentService.swapQueue(
                new SwapQueueReq(request.getRequesterCaddieId(), request.getTargetCaddieId(),
                        request.getRequestDate()),
                auth);

        eventPublisher.publishEvent(
                new SwapRequestProcessedEvent(this, requestId, request.getRequesterCaddieId(),
                        SwapRequestStatus.APPROVED, null));
    }

    @Transactional
    public void rejectSwapRequest(Long requestId, RejectSwapRequestReq req, AuthenticatedUser auth) {
        SwapRequest request = getActiveRequest(requestId, auth.getGolfCourseId());
        if (!request.isPending()) {
            throw new BusinessException(BoardErrorCode.SWAP_REQUEST_ALREADY_PROCESSED);
        }

        request.reject(req.rejectReason(), auth.getUserId());

        eventPublisher.publishEvent(
                new SwapRequestProcessedEvent(this, requestId, request.getRequesterCaddieId(),
                        SwapRequestStatus.REJECTED, req.rejectReason()));
    }

    // 캐디용: 내 요청 목록 조회
    @Transactional(readOnly = true)
    public Page<SwapRequestRes> getMySwapRequests(int page, int size, AuthenticatedUser auth) {
        Caddie caddie = caddieRepository.findByUser_IdAndIsDeletedFalse(auth.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        return swapRequestRepository
                .findByRequesterCaddieIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        caddie.getId(), PageRequest.of(page, size))
                .map(SwapRequestRes::from);
    }

    private SwapRequest getActiveRequest(Long requestId, Long golfCourseId) {
        SwapRequest request = swapRequestRepository.findById(requestId)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new BusinessException(BoardErrorCode.SWAP_REQUEST_NOT_FOUND));
        if (!request.getGolfCourseId().equals(golfCourseId)) {
            throw new BusinessException(BoardErrorCode.SWAP_REQUEST_NOT_FOUND);
        }
        return request;
    }
}
