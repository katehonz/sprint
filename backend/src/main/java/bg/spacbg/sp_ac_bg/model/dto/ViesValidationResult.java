package bg.spacbg.sp_ac_bg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of VIES VAT validation.
 * Contains the company name and address as returned by VIES.
 * The address is stored in longAddress as a single string (VIES returns it that way).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViesValidationResult {

    /**
     * Whether the VAT number is valid.
     */
    private boolean valid;

    /**
     * The validated VAT number (normalized).
     */
    private String vatNumber;

    /**
     * Country code (e.g., "BG", "DE").
     */
    private String countryCode;

    /**
     * Company name as returned by VIES.
     */
    private String name;

    /**
     * Full address as a single string (VIES returns the address as one line).
     * This is stored in the longAddress field of the counterpart.
     */
    private String longAddress;

    /**
     * Error message if validation failed.
     */
    private String errorMessage;

    /**
     * Source of validation (REST or SOAP).
     */
    private String source;

    public static ViesValidationResult invalid(String vatNumber, String errorMessage) {
        return ViesValidationResult.builder()
                .valid(false)
                .vatNumber(vatNumber)
                .errorMessage(errorMessage)
                .build();
    }
}
