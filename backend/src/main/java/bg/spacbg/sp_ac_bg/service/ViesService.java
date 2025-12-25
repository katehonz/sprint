package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.ViesValidationResult;

/**
 * Service for validating VAT numbers against the EU VIES (VAT Information Exchange System).
 */
public interface ViesService {

    /**
     * Validates a VAT number against VIES and returns company information.
     *
     * @param vatNumber The VAT number to validate (e.g., "BG123456789" or "DE123456789")
     * @return ViesValidationResult containing validation status and company data
     */
    ViesValidationResult validateVat(String vatNumber);
}
