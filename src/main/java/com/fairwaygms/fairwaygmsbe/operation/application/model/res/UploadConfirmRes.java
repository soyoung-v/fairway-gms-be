package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import java.util.List;

public record UploadConfirmRes(
        int successCount,
        int failCount,
        List<UploadPreviewRowRes> failedRows
) {}
