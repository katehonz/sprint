package bg.spacbg.sp_ac_bg.model.dto.saltedge;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class SaltEdgeAccount {
    private String id;

    @JsonProperty("connection_id")
    private String connectionId;

    private String name;
    private String nature; // account, bonus, card, checking, credit, credit_card, debit_card, ewallet, insurance, investment, loan, mortgage, savings

    @JsonProperty("currency_code")
    private String currencyCode;

    private BigDecimal balance;

    @JsonProperty("available_amount")
    private BigDecimal availableAmount;

    @JsonProperty("credit_limit")
    private BigDecimal creditLimit;

    private String iban;
    private String swift;

    @JsonProperty("sort_code")
    private String sortCode;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("card_type")
    private String cardType;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    private Map<String, Object> extra;
}
