package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.UploadConfirmRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.UploadPreviewRowRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationTeamUploadService {

    private static final String[] TEMPLATE_HEADERS = {"날짜", "코스명", "티업시간", "예약자", "인원", "지정캐디", "메모"};

    private static final DateTimeFormatter DATE_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final TeeTimeRepository teeTimeRepository;
    private final ReservationTeamRepository reservationTeamRepository;
    private final CaddieRepository caddieRepository;
    private final CourseRepository courseRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final GolfCourseContextResolver contextResolver;

    public byte[] downloadTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("예약팀업로드");

            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
            }

            // 예시 행
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("2025-06-01");
            example.createCell(1).setCellValue("A코스");
            example.createCell(2).setCellValue("08:00");
            example.createCell(3).setCellValue("홍길동");
            example.createCell(4).setCellValue(4);
            example.createCell(5).setCellValue("");
            example.createCell(6).setCellValue("");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public List<UploadPreviewRowRes> preview(MultipartFile file, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Map<String, Course> courseMap = buildCourseMap(golfCourse);

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<UploadPreviewRowRes> results = new ArrayList<>();

            // 0행은 헤더이므로 1행부터 처리
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                results.add(validateRow(row, i + 1, golfCourseId, courseMap));
            }
            return results;
        } catch (IOException e) {
            throw new BusinessException(OperationErrorCode.INVALID_FILE_FORMAT);
        }
    }

    @Transactional
    public UploadConfirmRes confirm(MultipartFile file, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Map<String, Course> courseMap = buildCourseMap(golfCourse);

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;
            List<UploadPreviewRowRes> failedRows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                UploadPreviewRowRes preview = validateRow(row, i + 1, golfCourseId, courseMap);
                if ("ERROR".equals(preview.status())) {
                    failedRows.add(preview);
                    continue;
                }

                createTeamFromRow(row, golfCourse, golfCourseId, courseMap);
                successCount++;
            }

            return new UploadConfirmRes(successCount, failedRows.size(), failedRows);
        } catch (IOException e) {
            throw new BusinessException(OperationErrorCode.INVALID_FILE_FORMAT);
        }
    }

    private UploadPreviewRowRes validateRow(Row row, int rowNumber, Long golfCourseId, Map<String, Course> courseMap) {
        String playDateStr = getCellString(row, 0);
        String courseName = getCellString(row, 1);
        String teeTimeStr = getCellString(row, 2);
        String bookerName = getCellString(row, 3);
        String playerCountStr = getCellString(row, 4);

        // 필수 필드 공백 검사
        if (playDateStr.isBlank() || courseName.isBlank() || teeTimeStr.isBlank() || bookerName.isBlank() || playerCountStr.isBlank()) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, "필수 값이 누락되었습니다.");
        }

        // 날짜 파싱
        LocalDate playDate = parseDate(playDateStr);
        if (playDate == null) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, "날짜 형식이 올바르지 않습니다. (예: 2025-06-01)");
        }

        // 시간 파싱
        LocalTime startTime = parseTime(teeTimeStr);
        if (startTime == null) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, "티업시간 형식이 올바르지 않습니다. (예: 08:00)");
        }

        // 인원 파싱
        Integer playerCount = parsePlayerCount(playerCountStr);
        if (playerCount == null || playerCount < 1) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, "인원은 1 이상의 숫자여야 합니다.");
        }

        // 코스 조회
        Course course = courseMap.get(courseName);
        if (course == null) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, "존재하지 않는 코스명입니다: " + courseName);
        }

        // 티타임 조회
        Optional<TeeTime> teeTime = teeTimeRepository.findByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                golfCourseId, course.getId(), playDate, startTime);
        if (teeTime.isEmpty()) {
            return UploadPreviewRowRes.error(rowNumber, playDateStr, courseName, teeTimeStr, bookerName,
                    "해당 날짜/코스/시간에 티타임이 존재하지 않습니다.");
        }

        // 지정캐디 (선택) — 이름 불일치 시 WARN
        String caddieName = getCellString(row, 5);
        if (!caddieName.isBlank()) {
            Optional<Caddie> caddie = caddieRepository.findByGolfCourse_IdAndNameAndIsDeletedFalse(golfCourseId, caddieName);
            if (caddie.isEmpty()) {
                return UploadPreviewRowRes.warn(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, playerCount,
                        "지정캐디를 찾을 수 없습니다. 캐디 미지정으로 등록됩니다: " + caddieName);
            }
        }

        return UploadPreviewRowRes.ok(rowNumber, playDateStr, courseName, teeTimeStr, bookerName, playerCount);
    }

    private void createTeamFromRow(Row row, GolfCourse golfCourse, Long golfCourseId, Map<String, Course> courseMap) {
        LocalDate playDate = parseDate(getCellString(row, 0));
        String courseName = getCellString(row, 1);
        LocalTime startTime = parseTime(getCellString(row, 2));
        String bookerName = getCellString(row, 3);
        int playerCount = parsePlayerCount(getCellString(row, 4));
        String caddieName = getCellString(row, 5);
        String memo = getCellString(row, 6);

        Course course = courseMap.get(courseName);
        TeeTime teeTime = teeTimeRepository.findByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                golfCourseId, course.getId(), playDate, startTime).orElseThrow();

        ReservationTeam team = ReservationTeam.create(golfCourse, teeTime, null, bookerName, playerCount, memo);

        if (!caddieName.isBlank()) {
            caddieRepository.findByGolfCourse_IdAndNameAndIsDeletedFalse(golfCourseId, caddieName)
                    .ifPresent(team::setDesignatedCaddie);
        }

        reservationTeamRepository.save(team);
    }

    private Map<String, Course> buildCourseMap(GolfCourse golfCourse) {
        return courseRepository.findAllByGolfCourseAndIsDeletedFalse(golfCourse)
                .stream()
                .collect(Collectors.toMap(Course::getName, c -> c));
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            return (val == Math.floor(val)) ? String.valueOf((int) val) : String.valueOf(val);
        }
        return cell.getStringCellValue().trim();
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (!getCellString(row, i).isBlank()) return false;
        }
        return true;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_DASH);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(value, DATE_SLASH);
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.length() == 4 ? "0" + value : value, TIME_FORMAT);
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    private Integer parsePlayerCount(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private void validateManager(AuthenticatedUser auth) {
        if (!auth.isManager() && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }
}
