package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.SwapQueueReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentService;
import com.fairwaygms.fairwaygmsbe.board.application.event.SwapRequestProcessedEvent;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.RejectSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestRes;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.SwapRequest;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.SwapRequestRepository;
import com.fairwaygms.fairwaygmsbe.board.exception.BoardErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SwapRequestServiceTest {

    @Mock private SwapRequestRepository swapRequestRepository;
    @Mock private CaddieRepository caddieRepository;
    @Mock private AssignmentService assignmentService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SwapRequestService swapRequestService;

    private AuthenticatedUser caddyAuth;
    private AuthenticatedUser managerAuth;
    private Caddie requesterCaddie;

    @BeforeEach
    void setUp() {
        swapRequestService = new SwapRequestService(
                swapRequestRepository, caddieRepository, assignmentService, eventPublisher);

        caddyAuth = new AuthenticatedUser(10L, UserRole.CADDY, 1L);
        managerAuth = new AuthenticatedUser(20L, UserRole.MANAGER, 1L);

        requesterCaddie = mock(Caddie.class);
        when(requesterCaddie.getId()).thenReturn(1L);
        when(requesterCaddie.getName()).thenReturn("홍길동");

        when(caddieRepository.findByUser_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(requesterCaddie));
    }

    @Test
    void createSwapRequest_자기_자신과_교환_불가() {
        // given — targetCaddieId가 본인 caddie ID와 동일
        CreateSwapRequestReq req = new CreateSwapRequestReq(1L, LocalDate.now(), "이유");

        // when / then
        assertThatThrownBy(() -> swapRequestService.createSwapRequest(req, caddyAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.SELF_SWAP_NOT_ALLOWED.getMessage());
    }

    @Test
    void createSwapRequest_성공() {
        // given
        CreateSwapRequestReq req = new CreateSwapRequestReq(2L, LocalDate.now(), "이유");
        SwapRequest swapRequest = SwapRequest.create(1L, 1L, 2L, LocalDate.now(), "이유");
        ReflectionTestUtils.setField(swapRequest, "id", 100L);

        when(swapRequestRepository.save(any(SwapRequest.class))).thenReturn(swapRequest);

        // when
        SwapRequestRes result = swapRequestService.createSwapRequest(req, caddyAuth);

        // then
        assertThat(result.requestId()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo("REQUESTED");
    }

    @Test
    void approveSwapRequest_이미_처리된_요청_예외() {
        // given
        SwapRequest approved = SwapRequest.create(1L, 1L, 2L, LocalDate.now(), "이유");
        ReflectionTestUtils.setField(approved, "id", 100L);
        approved.approve(20L);

        when(swapRequestRepository.findById(100L)).thenReturn(Optional.of(approved));

        // when / then
        assertThatThrownBy(() -> swapRequestService.approveSwapRequest(100L, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.SWAP_REQUEST_ALREADY_PROCESSED.getMessage());
    }

    @Test
    void approveSwapRequest_성공_swapQueue_호출_및_이벤트_발행() {
        // given
        LocalDate requestDate = LocalDate.now();
        SwapRequest swapRequest = SwapRequest.create(1L, 1L, 2L, requestDate, "이유");
        ReflectionTestUtils.setField(swapRequest, "id", 100L);

        when(swapRequestRepository.findById(100L)).thenReturn(Optional.of(swapRequest));
        doNothing().when(assignmentService).swapQueue(any(SwapQueueReq.class), any(AuthenticatedUser.class));

        // when
        swapRequestService.approveSwapRequest(100L, managerAuth);

        // then
        assertThat(swapRequest.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);

        ArgumentCaptor<SwapQueueReq> queueCaptor = ArgumentCaptor.forClass(SwapQueueReq.class);
        verify(assignmentService).swapQueue(queueCaptor.capture(), eq(managerAuth));
        assertThat(queueCaptor.getValue().caddieAId()).isEqualTo(1L);
        assertThat(queueCaptor.getValue().caddieBId()).isEqualTo(2L);

        ArgumentCaptor<SwapRequestProcessedEvent> eventCaptor = ArgumentCaptor.forClass(SwapRequestProcessedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
    }

    @Test
    void rejectSwapRequest_성공_이벤트_발행() {
        // given
        SwapRequest swapRequest = SwapRequest.create(1L, 1L, 2L, LocalDate.now(), "이유");
        ReflectionTestUtils.setField(swapRequest, "id", 100L);

        when(swapRequestRepository.findById(100L)).thenReturn(Optional.of(swapRequest));

        RejectSwapRequestReq req = new RejectSwapRequestReq("거절 사유");

        // when
        swapRequestService.rejectSwapRequest(100L, req, managerAuth);

        // then
        assertThat(swapRequest.getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(swapRequest.getRejectReason()).isEqualTo("거절 사유");

        ArgumentCaptor<SwapRequestProcessedEvent> captor = ArgumentCaptor.forClass(SwapRequestProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(captor.getValue().getRejectReason()).isEqualTo("거절 사유");
    }
}
