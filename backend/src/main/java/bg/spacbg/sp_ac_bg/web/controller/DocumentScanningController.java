package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.RecognizedInvoice;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import bg.spacbg.sp_ac_bg.service.DocumentScanningService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class DocumentScanningController {

    private final DocumentScanningService documentScanningService;
    private final CompanyService companyService;

    public DocumentScanningController(DocumentScanningService documentScanningService, CompanyService companyService) {
        this.documentScanningService = documentScanningService;
        this.companyService = companyService;
    }

    @MutationMapping
    public RecognizedInvoice recognizeInvoice(@Argument("file") MultipartFile file, @Argument("invoiceType") String invoiceType, @Argument("companyId") Integer companyId) throws IOException {
        return doRecognizeInvoice(file, invoiceType, companyId);
    }

    @PostMapping("/api/scan-invoice")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecognizedInvoice> scanInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("invoiceType") String invoiceType,
            @RequestParam("companyId") Integer companyId) throws IOException {
        return ResponseEntity.ok(doRecognizeInvoice(file, invoiceType, companyId));
    }

    private RecognizedInvoice doRecognizeInvoice(MultipartFile file, String invoiceType, Integer companyId) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        CompanyEntity company = companyService.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + companyId));

        String endpoint = company.getAzureFormRecognizerEndpoint();
        String key = company.getAzureFormRecognizerKey();

        if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException("Azure Form Recognizer credentials are not configured for this company.");
        }

        return documentScanningService.recognizeInvoice(file, invoiceType, endpoint, key, company);
    }
}
