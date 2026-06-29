package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import java.time.LocalDate;

public record InitializeQueueRes(
        int initializedCount,
        LocalDate queueDate
) {}
