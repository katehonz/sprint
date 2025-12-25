package bg.spacbg.sp_ac_bg.service;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for validating EU VAT numbers via the VIES (VAT Information Exchange System) API.
 * Uses the new REST API: https://ec.europa.eu/taxation_customs/vies/rest-api/check-vat-number
 */
@Service
public class ViesValidationService {

    private static final Logger log = LoggerFactory.getLogger(ViesValidationService.class);
    private static final String VIES_API_URL = "https://ec.europa.eu/taxation_customs/vies/rest-api/ms/{countryCode}/vat/{vatNumber}";

    // Pattern to extract country code and VAT number
    private static final Pattern VAT_PATTERN = Pattern.compile("^([A-Z]{2})([0-9A-Z]+)$");

    private final RestTemplate restTemplate;

    public ViesValidationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Validate a VAT number via VIES.
     * @param vatNumber Full VAT number including country code (e.g., "BG123456789")
     * @return Validation result with details
     */
    public ViesValidationResult validateVatNumber(String vatNumber) {
        if (vatNumber == null || vatNumber.isBlank()) {
            return ViesValidationResult.invalid("ДДС номерът е празен");
        }

        // Normalize VAT number
        String normalized = vatNumber.toUpperCase().replaceAll("[\\s\\-\\.]+", "");

        // Extract country code and number
        Matcher matcher = VAT_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return ViesValidationResult.invalid("Невалиден формат на ДДС номер. Очаква се: XX123456789");
        }

        String countryCode = matcher.group(1);
        String number = matcher.group(2);

        // Check if country is EU member
        if (!isEuCountry(countryCode)) {
            return ViesValidationResult.notApplicable("Държавата " + countryCode + " не е член на ЕС");
        }

        try {
            log.info("Validating VAT number via VIES: {} {}", countryCode, number);

            String url = VIES_API_URL
                    .replace("{countryCode}", countryCode)
                    .replace("{vatNumber}", number);

            ResponseEntity<ViesApiResponse> responseEntity = restTemplate.getForEntity(url, ViesApiResponse.class);
            ViesApiResponse response = responseEntity.getBody();

            if (response == null) {
                return ViesValidationResult.error("Няма отговор от VIES сървъра");
            }

            if (response.isValid()) {
                return ViesValidationResult.valid(
                        response.getName(),
                        response.getAddress(),
                        countryCode + number
                );
            } else {
                return ViesValidationResult.invalid("ДДС номерът не е валиден в системата VIES");
            }

        } catch (HttpClientErrorException e) {
            log.error("VIES API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                return ViesValidationResult.invalid("Невалиден ДДС номер");
            }
            return ViesValidationResult.error("Грешка при връзка с VIES: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error validating VAT number via VIES", e);
            return ViesValidationResult.error("Грешка при валидация: " + e.getMessage());
        }
    }

    private boolean isEuCountry(String countryCode) {
        return switch (countryCode) {
            case "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                 "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
                 "PL", "PT", "RO", "SK", "SI", "ES", "SE" -> true;
            default -> false;
        };
    }

    /**
     * Result of VIES validation
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViesValidationResult {
        private boolean valid;
        private String status;  // VALID, INVALID, NOT_APPLICABLE, ERROR
        private String message;
        private String companyName;
        private String companyAddress;
        private String validatedVatNumber;

        public static ViesValidationResult valid(String name, String address, String vatNumber) {
            ViesValidationResult result = new ViesValidationResult();
            result.setValid(true);
            result.setStatus("VALID");
            result.setMessage("ДДС номерът е валиден");
            result.setCompanyName(name);
            result.setCompanyAddress(address);
            result.setValidatedVatNumber(vatNumber);
            return result;
        }

        public static ViesValidationResult invalid(String message) {
            ViesValidationResult result = new ViesValidationResult();
            result.setValid(false);
            result.setStatus("INVALID");
            result.setMessage(message);
            return result;
        }

        public static ViesValidationResult notApplicable(String message) {
            ViesValidationResult result = new ViesValidationResult();
            result.setValid(false);
            result.setStatus("NOT_APPLICABLE");
            result.setMessage(message);
            return result;
        }

        public static ViesValidationResult error(String message) {
            ViesValidationResult result = new ViesValidationResult();
            result.setValid(false);
            result.setStatus("ERROR");
            result.setMessage(message);
            return result;
        }
    }

    /**
     * Response from VIES REST API
     */
    @Data
    public static class ViesApiResponse {
        private boolean isValid;
        private String name;
        private String address;
        private String requestDate;
        private String countryCode;
        private String vatNumber;

        public boolean isValid() {
            return isValid;
        }
    }
}
