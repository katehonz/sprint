package bg.spacbg.sp_ac_bg.model.dto;

import bg.spacbg.sp_ac_bg.model.entity.DepreciationJournalEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class DepreciationJournalDto {
    private Integer id;
    private Integer fixedAssetId;
    private String fixedAssetName;
    private String fixedAssetInventoryNumber;
    private LocalDate period;
    private Integer companyId;
    private BigDecimal accountingDepreciationAmount;
    private BigDecimal accountingBookValueBefore;
    private BigDecimal accountingBookValueAfter;
    private BigDecimal taxDepreciationAmount;
    private BigDecimal taxBookValueBefore;
    private BigDecimal taxBookValueAfter;
    private boolean isPosted;
    private Integer journalEntryId;
    private OffsetDateTime postedAt;
    private OffsetDateTime createdAt;

    public static DepreciationJournalDto fromEntity(DepreciationJournalEntity entity) {
        DepreciationJournalDto dto = new DepreciationJournalDto();
        dto.setId(entity.getId());
        dto.setFixedAssetId(entity.getFixedAsset().getId());
        dto.setFixedAssetName(entity.getFixedAsset().getName());
        dto.setFixedAssetInventoryNumber(entity.getFixedAsset().getInventoryNumber());
        dto.setPeriod(entity.getPeriod());
        dto.setCompanyId(entity.getCompany().getId());
        dto.setAccountingDepreciationAmount(entity.getAccountingDepreciationAmount());
        dto.setAccountingBookValueBefore(entity.getAccountingBookValueBefore());
        dto.setAccountingBookValueAfter(entity.getAccountingBookValueAfter());
        dto.setTaxDepreciationAmount(entity.getTaxDepreciationAmount());
        dto.setTaxBookValueBefore(entity.getTaxBookValueBefore());
        dto.setTaxBookValueAfter(entity.getTaxBookValueAfter());
        dto.setPosted(entity.isPosted());
        dto.setJournalEntryId(entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null);
        dto.setPostedAt(entity.getPostedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
