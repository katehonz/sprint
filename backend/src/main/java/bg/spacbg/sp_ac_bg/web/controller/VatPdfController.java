package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.service.VatPdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vat")
public class VatPdfController {

    private final VatPdfExportService vatPdfExportService;

    public VatPdfController(VatPdfExportService vatPdfExportService) {
        this.vatPdfExportService = vatPdfExportService;
    }

    @GetMapping("/pokupki-pdf/{returnId}")
    public ResponseEntity<byte[]> exportPokupkiPdf(@PathVariable Integer returnId) {
        byte[] pdfContent = vatPdfExportService.exportPokupkiPdf(returnId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "pokupki-" + returnId + ".pdf");
        headers.setContentLength(pdfContent.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    @GetMapping("/prodajbi-pdf/{returnId}")
    public ResponseEntity<byte[]> exportProdajbiPdf(@PathVariable Integer returnId) {
        byte[] pdfContent = vatPdfExportService.exportProdajbiPdf(returnId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "prodajbi-" + returnId + ".pdf");
        headers.setContentLength(pdfContent.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }
}
