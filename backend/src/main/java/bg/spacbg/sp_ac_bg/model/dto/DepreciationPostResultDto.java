package bg.spacbg.sp_ac_bg.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepreciationPostResultDto {
    private Integer journalEntryId;
    private BigDecimal totalAmount;
    private Integer assetsCount;

    public DepreciationPostResultDto(Integer journalEntryId, BigDecimal totalAmount, Integer assetsCount) {
        this.journalEntryId = journalEntryId;
        this.totalAmount = totalAmount;
        this.assetsCount = assetsCount;
    }
}
