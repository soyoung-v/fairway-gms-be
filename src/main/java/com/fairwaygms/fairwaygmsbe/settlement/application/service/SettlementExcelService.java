package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.AssignmentRecord;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlementCaddie;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.AssignmentRecordRepository;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.MonthlySettlementCaddieRepository;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.MonthlySettlementRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementExcelService {

    // 국세청 과세자료(용역제공자) 제출 양식 컬럼 — 주민등록번호/내외국인은 시스템 미보관으로 수기 기입
    private static final String[] TAX_HEADERS = {
            "제출의무자 상호", "귀속연도", "귀속월", "주민등록번호\n(용역제공자)",
            "내외국인\n'1' 또는 '9' 기재", "성명", "용역구분코드",
            "용역제공기간 개시일", "용역제공기간 종료일", "용역제공일수", "용역제공횟수", "용역제공대가"
    };
    // 국세청 용역구분코드 — 골프장 캐디는 11
    private static final String CADDIE_SERVICE_CODE = "11";

    private static final String[] SETTLEMENT_HEADERS = {
            "캐디번호", "성명", "제공일수", "근무횟수", "캐디피 합계", "조정 금액", "최종 지급액", "조정 사유"
    };
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AssignmentRecordRepository assignmentRecordRepository;
    private final MonthlySettlementRepository settlementRepository;
    private final MonthlySettlementCaddieRepository settlementCaddieRepository;
    private final CaddieRepository caddieRepository;
    private final GolfCourseRepository golfCourseRepository;

    // API-610: 과세자료 관리대장 다운로드 — 국세청 제출 양식 (FR-611)
    @Transactional(readOnly = true)
    public byte[] exportInsurance(String yearMonth, AuthenticatedUser auth) {
        YearMonth ym = parseYearMonth(yearMonth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Map<Long, List<AssignmentRecord>> byCaddie = assignmentRecordRepository
                .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .stream()
                .collect(Collectors.groupingBy(AssignmentRecord::getCaddieId));
        Map<Long, Caddie> caddieMap = buildCaddieMap(golfCourseId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("과세자료");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            Row header = sheet.createRow(0);
            header.setHeightInPoints(30);
            for (int c = 0; c < TAX_HEADERS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(TAX_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            // 성명 가나다순 정렬
            List<Long> sortedCaddieIds = byCaddie.keySet().stream()
                    .sorted(Comparator.comparing(id -> caddieName(caddieMap, id)))
                    .toList();

            for (Long caddieId : sortedCaddieIds) {
                List<AssignmentRecord> records = byCaddie.get(caddieId);
                LocalDate startDate = records.stream().map(AssignmentRecord::getPlayDate)
                        .min(LocalDate::compareTo).orElse(ym.atDay(1));
                LocalDate endDate = records.stream().map(AssignmentRecord::getPlayDate)
                        .max(LocalDate::compareTo).orElse(ym.atEndOfMonth());
                long workDays = records.stream().map(AssignmentRecord::getPlayDate).distinct().count();
                BigDecimal totalFee = records.stream().map(AssignmentRecord::getFeeAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Row row = sheet.createRow(rowIdx++);
                writeText(row, 0, golfCourse.getName(), bodyStyle);
                writeText(row, 1, String.valueOf(ym.getYear()), bodyStyle);
                writeText(row, 2, String.valueOf(ym.getMonthValue()), bodyStyle);
                writeText(row, 3, "", bodyStyle);  // 주민등록번호 — 수기 기입
                writeText(row, 4, "", bodyStyle);  // 내외국인 — 수기 기입
                writeText(row, 5, caddieName(caddieMap, caddieId), bodyStyle);
                writeText(row, 6, CADDIE_SERVICE_CODE, bodyStyle);
                writeText(row, 7, startDate.format(COMPACT_DATE), bodyStyle);
                writeText(row, 8, endDate.format(COMPACT_DATE), bodyStyle);
                writeNumber(row, 9, workDays, numberStyle);
                writeNumber(row, 10, records.size(), numberStyle);
                writeNumber(row, 11, totalFee.longValue(), numberStyle);
            }

            int[] widths = {4000, 2200, 2000, 4200, 3600, 3000, 2800, 4200, 4200, 3000, 3000, 3600};
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c]);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // API-611: 정산 자료 다운로드 — 캐디별 일수/횟수/캐디피/조정액 (FR-612)
    @Transactional(readOnly = true)
    public byte[] exportSettlement(String yearMonth, AuthenticatedUser auth) {
        parseYearMonth(yearMonth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        List<Object[]> rows = assignmentRecordRepository.aggregateByCaddie(golfCourseId, yearMonth);
        Map<Long, Caddie> caddieMap = buildCaddieMap(golfCourseId);

        // 월 마감 확정이 있으면 조정액/사유 반영
        Map<Long, MonthlySettlementCaddie> adjustedMap = settlementRepository
                .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .map(s -> settlementCaddieRepository.findByMonthlySettlementId(s.getId())
                        .stream()
                        .collect(Collectors.toMap(MonthlySettlementCaddie::getCaddieId, Function.identity())))
                .orElse(Map.of());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("정산자료");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(yearMonth + " 캐디피 정산 자료 — " + golfCourse.getName());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, SETTLEMENT_HEADERS.length - 1));

            Row header = sheet.createRow(2);
            for (int c = 0; c < SETTLEMENT_HEADERS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(SETTLEMENT_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            List<Object[]> sortedRows = rows.stream()
                    .sorted(Comparator.comparing(r -> caddieName(caddieMap, (Long) r[0])))
                    .toList();

            int rowIdx = 3;
            long totalDays = 0;
            long totalRounds = 0;
            BigDecimal totalFeeSum = BigDecimal.ZERO;
            BigDecimal finalFeeSum = BigDecimal.ZERO;

            for (Object[] r : sortedRows) {
                Long caddieId = (Long) r[0];
                long workDays = ((Number) r[1]).longValue();
                long roundCount = ((Number) r[2]).longValue();
                BigDecimal totalFee = (BigDecimal) r[3];

                MonthlySettlementCaddie adjusted = adjustedMap.get(caddieId);
                BigDecimal finalFee = adjusted != null ? adjusted.getAdjustedFee() : totalFee;
                BigDecimal adjustAmount = adjusted != null
                        ? adjusted.getAdjustedFee().subtract(totalFee) : BigDecimal.ZERO;
                String adjustReason = adjusted != null && adjusted.getAdjustmentReason() != null
                        ? adjusted.getAdjustmentReason() : "";

                Caddie caddie = caddieMap.get(caddieId);
                Row row = sheet.createRow(rowIdx++);
                writeText(row, 0, caddie != null && caddie.getCaddieNumber() != null
                        ? caddie.getCaddieNumber() : "", bodyStyle);
                writeText(row, 1, caddieName(caddieMap, caddieId), bodyStyle);
                writeNumber(row, 2, workDays, numberStyle);
                writeNumber(row, 3, roundCount, numberStyle);
                writeNumber(row, 4, totalFee.longValue(), numberStyle);
                writeNumber(row, 5, adjustAmount.longValue(), numberStyle);
                writeNumber(row, 6, finalFee.longValue(), numberStyle);
                writeText(row, 7, adjustReason, bodyStyle);

                totalDays += workDays;
                totalRounds += roundCount;
                totalFeeSum = totalFeeSum.add(totalFee);
                finalFeeSum = finalFeeSum.add(finalFee);
            }

            // 합계 행
            Row sumRow = sheet.createRow(rowIdx);
            writeText(sumRow, 0, "합계", headerStyle);
            writeText(sumRow, 1, "", headerStyle);
            writeNumber(sumRow, 2, totalDays, numberStyle);
            writeNumber(sumRow, 3, totalRounds, numberStyle);
            writeNumber(sumRow, 4, totalFeeSum.longValue(), numberStyle);
            writeNumber(sumRow, 5, finalFeeSum.subtract(totalFeeSum).longValue(), numberStyle);
            writeNumber(sumRow, 6, finalFeeSum.longValue(), numberStyle);
            writeText(sumRow, 7, "", bodyStyle);

            int[] widths = {2800, 3000, 2600, 2600, 3600, 3600, 3600, 5000};
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c]);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Map<Long, Caddie> buildCaddieMap(Long golfCourseId) {
        return caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId).stream()
                .collect(Collectors.toMap(Caddie::getId, Function.identity()));
    }

    private String caddieName(Map<Long, Caddie> caddieMap, Long caddieId) {
        Caddie caddie = caddieMap.get(caddieId);
        return caddie != null ? caddie.getName() : "알 수 없음";
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private void writeText(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        applyBorders(style);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
