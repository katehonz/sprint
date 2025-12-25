package bg.spacbg.sp_ac_bg.model.dto;

import bg.spacbg.sp_ac_bg.service.DepreciationService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DepreciationCalculationResultDto {
    private List<DepreciationJournalDto> calculated;
    private List<DepreciationErrorDto> errors;
    private BigDecimal totalAccountingAmount;
    private BigDecimal totalTaxAmount;

    public static DepreciationCalculationResultDto fromResult(DepreciationService.DepreciationCalculationResult result) {
        DepreciationCalculationResultDto dto = new DepreciationCalculationResultDto();
        dto.setCalculated(result.calculated().stream()
                .map(DepreciationJournalDto::fromEntity)
                .collect(Collectors.toList()));
        dto.setErrors(result.errors().stream()
                .map(DepreciationErrorDto::fromError)
                .collect(Collectors.toList()));
        dto.setTotalAccountingAmount(result.totalAccountingAmount());
        dto.setTotalTaxAmount(result.totalTaxAmount());
        return dto;
    }

    @Data
    public static class DepreciationErrorDto {
        private Integer fixedAssetId;
        private String assetName;
        private String errorMessage;

        public static DepreciationErrorDto fromError(DepreciationService.DepreciationError error) {
            DepreciationErrorDto dto = new DepreciationErrorDto();
            dto.setFixedAssetId(error.fixedAssetId());
            dto.setAssetName(error.assetName());
            dto.setErrorMessage(error.errorMessage());
            return dto;
        }
    }
}
