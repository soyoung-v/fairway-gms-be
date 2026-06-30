package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;

import java.util.List;

public record OperationSettingRes(
        Long settingId,
        String yearMonth,
        List<OperationPeriodRes> periods
) {
    public static OperationSettingRes of(OperationSetting setting, List<OperationPeriod> periods) {
        return new OperationSettingRes(
                setting.getId(),
                setting.getYearMonth(),
                periods.stream().map(OperationPeriodRes::from).toList()
        );
    }
}
