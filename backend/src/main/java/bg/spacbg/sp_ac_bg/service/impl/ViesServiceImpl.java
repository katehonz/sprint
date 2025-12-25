package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.ViesValidationResult;
import bg.spacbg.sp_ac_bg.service.ViesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Имплементация на ViesService за валидация на ДДС номера чрез EU VIES системата.
 *
 * <h2>Описание</h2>
 * <p>VIES (VAT Information Exchange System) е система на ЕС за проверка на валидността
 * на ДДС номера на фирми, регистрирани в държави-членки на ЕС.</p>
 *
 * <h2>Функционалност</h2>
 * <ul>
 *   <li>Валидира ДДС номера срещу официалната VIES база данни</li>
 *   <li>Извлича име на фирмата и адрес (ако са налични)</li>
 *   <li>Поддържа всички EU държави-членки</li>
 *   <li>Автоматичен fallback от REST към SOAP API при проблеми</li>
 * </ul>
 *
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>REST: https://ec.europa.eu/taxation_customs/vies/rest-api/ms/{countryCode}/vat/{vatNumber}</li>
 *   <li>SOAP: https://ec.europa.eu/taxation_customs/vies/services/checkVatService</li>
 * </ul>
 *
 * <h2>Формат на ДДС номер</h2>
 * <p>ДДС номерът трябва да започва с 2-буквен код на държавата (ISO 3166-1 alpha-2),
 * последван от национален идентификатор. Примери:</p>
 * <ul>
 *   <li>BG123456789 - България</li>
 *   <li>DE123456789 - Германия</li>
 *   <li>FR12345678901 - Франция</li>
 * </ul>
 *
 * <h2>Резултат</h2>
 * <p>При успешна валидация връща {@link ViesValidationResult} съдържащ:</p>
 * <ul>
 *   <li><b>valid</b> - дали номерът е валиден</li>
 *   <li><b>name</b> - име на фирмата (ако е публично достъпно)</li>
 *   <li><b>longAddress</b> - пълен адрес като един string (формат на VIES)</li>
 *   <li><b>countryCode</b> - код на държавата</li>
 *   <li><b>source</b> - източник (REST или SOAP)</li>
 * </ul>
 *
 * <h2>Важни бележки</h2>
 * <ul>
 *   <li>Някои държави не предоставят име и адрес (напр. Германия)</li>
 *   <li>Адресът се връща като един дълъг string и се записва в полето longAddress</li>
 *   <li>VIES системата понякога е недостъпна за поддръжка</li>
 *   <li>Timeout е 30 секунди за всяка заявка</li>
 * </ul>
 *
 * @author SP-AC-BG Team
 * @see ViesService
 * @see ViesValidationResult
 * @see <a href="https://ec.europa.eu/taxation_customs/vies/">VIES официален сайт</a>
 */
@Service
public class ViesServiceImpl implements ViesService {

    private static final Logger log = LoggerFactory.getLogger(ViesServiceImpl.class);

    private static final String VIES_REST_URL_TEMPLATE =
            "https://ec.europa.eu/taxation_customs/vies/rest-api/ms/%s/vat/%s";

    private static final String VIES_SOAP_URL =
            "https://ec.europa.eu/taxation_customs/vies/services/checkVatService";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ViesServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ViesValidationResult validateVat(String vatNumber) {
        if (vatNumber == null || vatNumber.trim().length() < 3) {
            return ViesValidationResult.invalid(vatNumber, "ДДС номерът е твърде кратък");
        }

        String normalized = normalizeVatNumber(vatNumber);
        String countryCode = normalized.substring(0, 2);
        String numericPart = normalized.substring(2);

        // Try REST API first
        try {
            ViesValidationResult restResult = checkViaRest(countryCode, numericPart);
            if (restResult != null) {
                return restResult;
            }
        } catch (Exception e) {
            log.warn("VIES REST API failed, falling back to SOAP: {}", e.getMessage());
        }

        // Fall back to SOAP
        try {
            return checkViaSoap(countryCode, numericPart);
        } catch (Exception e) {
            log.error("VIES SOAP API also failed: {}", e.getMessage());
            return ViesValidationResult.invalid(normalized, "Грешка при валидация: " + e.getMessage());
        }
    }

    private ViesValidationResult checkViaRest(String countryCode, String vatNumber) throws IOException, InterruptedException {
        String url = String.format(VIES_REST_URL_TEMPLATE, countryCode, vatNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            return ViesValidationResult.builder()
                    .valid(false)
                    .vatNumber(countryCode + vatNumber)
                    .countryCode(countryCode)
                    .source("REST")
                    .errorMessage("ДДС номерът не е намерен")
                    .build();
        }

        if (response.statusCode() != 200) {
            log.warn("VIES REST returned status {}", response.statusCode());
            return null; // Fall back to SOAP
        }

        JsonNode json = objectMapper.readTree(response.body());

        boolean isValid = json.has("isValid") && json.get("isValid").asBoolean();
        String name = json.has("name") ? json.get("name").asText(null) : null;
        String address = json.has("address") ? json.get("address").asText(null) : null;
        String responseCountryCode = json.has("countryCode") ? json.get("countryCode").asText(countryCode) : countryCode;

        return ViesValidationResult.builder()
                .valid(isValid)
                .vatNumber(countryCode + vatNumber)
                .countryCode(responseCountryCode)
                .name(cleanViesString(name))
                .longAddress(cleanViesString(address))
                .source("REST")
                .build();
    }

    private ViesValidationResult checkViaSoap(String countryCode, String vatNumber) throws Exception {
        String soapRequest = buildSoapRequest(countryCode, vatNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIES_SOAP_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(soapRequest))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("VIES SOAP returned status " + response.statusCode());
        }

        return parseSoapResponse(countryCode, vatNumber, response.body());
    }

    private String buildSoapRequest(String countryCode, String vatNumber) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:urn=\"urn:ec.europa.eu:taxud:vies:services:checkVat\">" +
                "<soap:Header/>" +
                "<soap:Body>" +
                "<urn:checkVat>" +
                "<urn:countryCode>" + countryCode + "</urn:countryCode>" +
                "<urn:vatNumber>" + vatNumber + "</urn:vatNumber>" +
                "</urn:checkVat>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }

    private ViesValidationResult parseSoapResponse(String countryCode, String vatNumber, String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        boolean valid = false;
        String name = null;
        String address = null;

        NodeList validNodes = doc.getElementsByTagName("valid");
        if (validNodes.getLength() > 0) {
            valid = Boolean.parseBoolean(validNodes.item(0).getTextContent());
        }

        NodeList nameNodes = doc.getElementsByTagName("name");
        if (nameNodes.getLength() > 0) {
            name = nameNodes.item(0).getTextContent();
        }

        NodeList addressNodes = doc.getElementsByTagName("address");
        if (addressNodes.getLength() > 0) {
            address = addressNodes.item(0).getTextContent();
        }

        return ViesValidationResult.builder()
                .valid(valid)
                .vatNumber(countryCode + vatNumber)
                .countryCode(countryCode)
                .name(cleanViesString(name))
                .longAddress(cleanViesString(address))
                .source("SOAP")
                .build();
    }

    private String normalizeVatNumber(String vatNumber) {
        return vatNumber.trim().toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * Cleans strings returned by VIES - removes extra whitespace and newlines.
     */
    private String cleanViesString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // VIES sometimes returns strings with newlines, clean them up
        return value.replaceAll("\\s+", " ").trim();
    }
}
