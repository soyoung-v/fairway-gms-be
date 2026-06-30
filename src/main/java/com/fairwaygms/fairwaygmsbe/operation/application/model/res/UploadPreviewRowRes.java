package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

public record UploadPreviewRowRes(
        int rowNumber,
        String status,       // OK, ERROR, WARN
        String playDate,
        String courseName,
        String teeTime,
        String bookerName,
        Integer playerCount,
        String errorMessage
) {
    public static UploadPreviewRowRes ok(int rowNumber, String playDate, String courseName,
                                         String teeTime, String bookerName, Integer playerCount) {
        return new UploadPreviewRowRes(rowNumber, "OK", playDate, courseName, teeTime, bookerName, playerCount, null);
    }

    public static UploadPreviewRowRes warn(int rowNumber, String playDate, String courseName,
                                           String teeTime, String bookerName, Integer playerCount, String message) {
        return new UploadPreviewRowRes(rowNumber, "WARN", playDate, courseName, teeTime, bookerName, playerCount, message);
    }

    public static UploadPreviewRowRes error(int rowNumber, String playDate, String courseName,
                                            String teeTime, String bookerName, String message) {
        return new UploadPreviewRowRes(rowNumber, "ERROR", playDate, courseName, teeTime, bookerName, null, message);
    }
}
