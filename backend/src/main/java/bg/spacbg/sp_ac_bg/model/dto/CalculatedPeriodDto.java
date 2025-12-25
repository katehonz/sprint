package bg.spacbg.sp_ac_bg.model.dto;

import bg.spacbg.sp_ac_bg.service.DepreciationService;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculatedPeriodDto {
    private Integer year;
    private Integer month;
    private String periodDisplay;
    private boolean isPosted;
    private BigDecimal totalAccountingAmount;
    private BigDecimal totalTaxAmount;
    private Integer assetsCount;

    public static CalculatedPeriodDto fromRecord(DepreciationService.CalculatedPeriod period) {
        CalculatedPeriodDto dto = new CalculatedPeriodDto();
        dto.setYear(period.year());
        dto.setMonth(period.month());
        dto.setPeriodDisplay(period.periodDisplay());
        dto.setPosted(period.isPosted());
        dto.setTotalAccountingAmount(period.totalAccountingAmount());
        dto.setTotalTaxAmount(period.totalTaxAmount());
        dto.setAssetsCount(period.assetsCount());
        return dto;
    }
}
