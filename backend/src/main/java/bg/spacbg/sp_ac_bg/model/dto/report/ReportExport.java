package bg.spacbg.sp_ac_bg.model.dto.report;

public record ReportExport(
    String format,
    String content,  // Base64 encoded
    String filename,
    String mimeType
) {}
