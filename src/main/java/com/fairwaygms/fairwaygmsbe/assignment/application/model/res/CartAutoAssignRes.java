package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import java.time.LocalDate;

public record CartAutoAssignRes(
        LocalDate assignmentDate,
        int assignedCount,
        int skippedCount
) {}
