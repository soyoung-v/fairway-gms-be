package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.UploadConfirmRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.UploadPreviewRowRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.ReservationTeamUploadService;
import com.fairwaygms.fairwaygmsbe.common.config.AdminScopeApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@AdminScopeApi
@Tag(name = "예약팀")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation/reservation-teams/upload")
public class ReservationTeamUploadController {

    private final ReservationTeamUploadService uploadService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @AuthenticationPrincipal AuthenticatedUser auth
    ) {
        byte[] file = uploadService.downloadTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("reservation_team_template.xlsx", StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(file);
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<List<UploadPreviewRowRes>>> preview(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(uploadService.preview(file, auth)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<UploadConfirmRes>> confirm(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(uploadService.confirm(file, auth)));
    }
}
