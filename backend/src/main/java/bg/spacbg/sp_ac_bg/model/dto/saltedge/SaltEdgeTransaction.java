package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class SaltEdgeTransaction {
    private String id;

    @JsonProperty("account_id")
    private String accountId;

    private String duplicated; // boolean as string in Salt Edge API

    private String mode; // normal, fee, transfer

    private String status; // posted, pending

    @JsonProperty("made_on")
    private LocalDate madeOn;

    private BigDecimal amount;

    @JsonProperty("currency_code")
    private String currencyCode;

    private String description;

    private String category;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    private Map<String, Object> extra;

    // Helper method to determine if credit or debit
    public boolean isCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
}
