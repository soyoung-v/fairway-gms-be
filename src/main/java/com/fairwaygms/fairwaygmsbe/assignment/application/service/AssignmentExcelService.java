package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentExcelService {

    private static final String[] HEADERS = {"조", "캐디명", "코스", "티업시간", "출근시간", "카트", "비고"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AssignmentRepository assignmentRepository;
    private final CartAssignmentRepository cartAssignmentRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // API-516: 배정표 엑셀 다운로드 — 게시판 시간표와 동일하게 부/조 단위로 구성 (FR-523)
    @Transactional(readOnly = true)
    public byte[] exportDailySchedule(LocalDate assignmentDate, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);

        List<Assignment> assignments = assignmentRepository
                .findByGolfCourseAndDateWithDetails(golfCourseId, assignmentDate);
        if (assignments.isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        Map<Long, CartAssignment> cartByTeeTimeId = cartAssignmentRepository
                .findActiveByGolfCourseAndDate(golfCourseId, assignmentDate)
                .stream()
                .collect(Collectors.toMap(
                        ca -> ca.getTeeTime().getId(),
                        ca -> ca,
                        (a, b) -> a));

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildScheduleSheet(workbook, assignmentDate, assignments, cartByTeeTimeId);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void buildScheduleSheet(XSSFWorkbook workbook, LocalDate date,
                                    List<Assignment> assignments,
                                    Map<Long, CartAssignment> cartByTeeTimeId) {
        Sheet sheet = workbook.createSheet("배정표");

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle periodStyle = createPeriodStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook, false);
        CellStyle groupStyle = createBodyStyle(workbook, true);

        int lastCol = HEADERS.length - 1;

        // 제목 — 날짜 + 요일
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(date + " (" + dayOfWeek + ") 캐디 배정표");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

        // 부 번호 기준 오름차순 그룹핑 — 게시판 시간표와 동일한 기준
        Map<Integer, List<Assignment>> byPeriod = new TreeMap<>(
                assignments.stream().collect(
                        Collectors.groupingBy(a -> a.getReservationTeam().getTeeTime()
                                .getOperationPeriod().getPeriodNumber())));

        int rowIdx = 2;
        for (Map.Entry<Integer, List<Assignment>> periodEntry : byPeriod.entrySet()) {
            // 부 구분 행
            Row periodRow = sheet.createRow(rowIdx);
            periodRow.setHeightInPoints(22);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(periodEntry.getKey() + "부");
            periodCell.setCellStyle(periodStyle);
            for (int c = 1; c <= lastCol; c++) {
                periodRow.createCell(c).setCellStyle(periodStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, lastCol));
            rowIdx++;

            // 컬럼 헤더 행
            Row headerRow = sheet.createRow(rowIdx);
            for (int c = 0; c <= lastCol; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }
            rowIdx++;

            // 조별 그룹핑 — 티타임 오름차순 정렬 후 조 이름 삽입 순서 유지
            Map<String, List<Assignment>> byGroup = new LinkedHashMap<>();
            periodEntry.getValue().stream()
                    .sorted(Comparator
                            .comparing((Assignment a) -> a.getReservationTeam().getTeeTime().getStartTime())
                            .thenComparing(a -> a.getReservationTeam().getTeeTime().getCourse().getName()))
                    .forEach(a -> {
                        String groupName = a.getCaddie().getCaddieGroup() != null
                                ? a.getCaddie().getCaddieGroup().getName()
                                : "미편성";
                        byGroup.computeIfAbsent(groupName, k -> new ArrayList<>()).add(a);
                    });

            for (Map.Entry<String, List<Assignment>> groupEntry : byGroup.entrySet()) {
                int groupStartRow = rowIdx;
                for (Assignment a : groupEntry.getValue()) {
                    TeeTime tt = a.getReservationTeam().getTeeTime();
                    CartAssignment cart = cartByTeeTimeId.get(tt.getId());

                    Row row = sheet.createRow(rowIdx);
                    writeCell(row, 0, groupEntry.getKey(), groupStyle);
                    writeCell(row, 1, a.getCaddie().getName(), bodyStyle);
                    writeCell(row, 2, tt.getCourse().getName(), bodyStyle);
                    writeCell(row, 3, tt.getStartTime().format(TIME_FMT), bodyStyle);
                    // 출근시간은 티업 1시간 전 — 게시판 시간표와 동일 기준
                    writeCell(row, 4, tt.getStartTime().minusHours(1).format(TIME_FMT), bodyStyle);
                    writeCell(row, 5, cart != null ? cart.getCart().getCartNumber() + "호" : "", bodyStyle);
                    writeCell(row, 6, Boolean.TRUE.equals(a.getIsHalfBack()) ? "투근무" : "", bodyStyle);
                    rowIdx++;
                }
                if (rowIdx - 1 > groupStartRow) {
                    sheet.addMergedRegion(new CellRangeAddress(groupStartRow, rowIdx - 1, 0, 0));
                }
            }
            rowIdx++; // 부 사이 빈 행
        }

        int[] widths = {2200, 3200, 3200, 2800, 2800, 2400, 2800};
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, widths[c]);
        }
    }

    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createPeriodStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        if (bold) {
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
