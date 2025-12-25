package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.model.enums.VatReturnStatus;
import bg.spacbg.sp_ac_bg.repository.JournalEntryRepository;
import bg.spacbg.sp_ac_bg.repository.VatReturnRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class VatPdfExportService {

    private static final Color HEADER_BG_COLOR = new Color(0, 150, 180);
    private static final Color HEADER_TEXT_COLOR = Color.WHITE;
    private static final Color ROW_ALT_COLOR = new Color(245, 245, 245);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VatReturnRepository vatReturnRepository;
    private final JournalEntryRepository journalEntryRepository;
    private Font headerFont;
    private Font titleFont;
    private Font cellFont;
    private Font totalFont;

    public VatPdfExportService(VatReturnRepository vatReturnRepository,
                               JournalEntryRepository journalEntryRepository) {
        this.vatReturnRepository = vatReturnRepository;
        this.journalEntryRepository = journalEntryRepository;
        initFonts();
    }

    private void initFonts() {
        try {
            // Use CP1251 encoding for Cyrillic support
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, "CP1251", BaseFont.NOT_EMBEDDED);
            titleFont = new Font(bf, 16, Font.BOLD);
            headerFont = new Font(bf, 8, Font.BOLD, HEADER_TEXT_COLOR);
            cellFont = new Font(bf, 7, Font.NORMAL);
            totalFont = new Font(bf, 8, Font.BOLD);
        } catch (Exception e) {
            // Fallback
            titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, HEADER_TEXT_COLOR);
            cellFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
            totalFont = new Font(Font.HELVETICA, 8, Font.BOLD);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportPokupkiPdf(Integer returnId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + returnId));

        if (vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("ДДС декларацията трябва да е в статус 'Изчислена'");
        }

        List<JournalEntryEntity> entries = journalEntryRepository.findByCompanyIdAndVatDateBetween(
                vatReturn.getCompany().getId(), vatReturn.getPeriodFrom(), vatReturn.getPeriodTo()
        ).stream().filter(e -> e.getVatPurchaseOperation() != null).toList();

        return generatePokupkiPdf(vatReturn, entries);
    }

    @Transactional(readOnly = true)
    public byte[] exportProdajbiPdf(Integer returnId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + returnId));

        if (vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("ДДС декларацията трябва да е в статус 'Изчислена'");
        }

        List<JournalEntryEntity> entries = journalEntryRepository.findByCompanyIdAndVatDateBetween(
                vatReturn.getCompany().getId(), vatReturn.getPeriodFrom(), vatReturn.getPeriodTo()
        ).stream().filter(e -> e.getVatSalesOperation() != null).toList();

        return generateProdajbiPdf(vatReturn, entries);
    }

    private byte[] generatePokupkiPdf(VatReturnEntity vatReturn, List<JournalEntryEntity> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            CompanyEntity company = vatReturn.getCompany();
            String period = vatReturn.getPeriodMonth() + "/" + vatReturn.getPeriodYear();

            // Title
            Paragraph title = new Paragraph("Дневник за Покупки", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph companyInfo = new Paragraph(company.getName() + " " + company.getVatNumber(), titleFont);
            companyInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(companyInfo);

            Paragraph periodPara = new Paragraph(period, titleFont);
            periodPara.setAlignment(Element.ALIGN_CENTER);
            periodPara.setSpacingAfter(15);
            document.add(periodPara);

            // Analyze which columns have non-zero values
            List<PokupkiRow> rows = new ArrayList<>();
            BigDecimal totalCol9 = BigDecimal.ZERO;
            BigDecimal totalCol10 = BigDecimal.ZERO;
            BigDecimal totalCol11 = BigDecimal.ZERO;

            int lineNum = 1;
            for (JournalEntryEntity entry : entries) {
                CounterpartEntity counterpart = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getCounterpart).filter(Objects::nonNull).findFirst().orElse(null);

                if (counterpart == null) continue;

                BigDecimal baseAmount = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getBaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal vatAmount = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                PokupkiRow row = new PokupkiRow();
                row.lineNum = lineNum++;
                row.docType = entry.getVatDocumentType() != null ? entry.getVatDocumentType() : "01";
                row.docNumber = entry.getDocumentNumber();
                row.docDate = entry.getDocumentDate() != null ? entry.getDocumentDate().format(DATE_FMT) : "";
                row.vatNumber = counterpart.getVatNumber() != null ? counterpart.getVatNumber() : counterpart.getEik();
                row.name = counterpart.getName();
                row.description = entry.getDescription();

                // Determine which columns based on operation type
                String op = entry.getVatPurchaseOperation();
                if ("пок30".equals(op) || "пок32".equals(op)) {
                    row.col9 = baseAmount;
                    row.col10 = BigDecimal.ZERO;
                    row.col11 = BigDecimal.ZERO;
                } else {
                    row.col9 = BigDecimal.ZERO;
                    row.col10 = baseAmount;
                    row.col11 = vatAmount;
                }

                totalCol9 = totalCol9.add(row.col9);
                totalCol10 = totalCol10.add(row.col10);
                totalCol11 = totalCol11.add(row.col11);

                rows.add(row);
            }

            // Determine which columns to show (non-zero totals)
            boolean showCol9 = totalCol9.compareTo(BigDecimal.ZERO) != 0;
            boolean showCol10 = totalCol10.compareTo(BigDecimal.ZERO) != 0;
            boolean showCol11 = totalCol11.compareTo(BigDecimal.ZERO) != 0;

            // Build dynamic column widths
            List<Float> widths = new ArrayList<>(Arrays.asList(25f, 25f, 70f, 55f, 80f, 150f, 70f));
            List<String> headers = new ArrayList<>(Arrays.asList(
                    "кл. 1\n№ по ред", "кл. 3\nВид", "кл. 4\nДокумент номер", "кл. 5\nДата",
                    "кл. 6\nНомер на контр.доставчик", "кл. 7\nИме на контрагента(доставчик)",
                    "кл. 8\nВид на стоката услугата"
            ));

            if (showCol9) {
                widths.add(60f);
                headers.add("кл. 9\nДО без право на дан.кред");
            }
            if (showCol10) {
                widths.add(60f);
                headers.add("кл. 10\nДО с право на ПДК");
            }
            if (showCol11) {
                widths.add(50f);
                headers.add("кл. 11\nДДС с право на пълен данъчен кредит");
            }

            float[] widthArray = new float[widths.size()];
            for (int i = 0; i < widths.size(); i++) widthArray[i] = widths.get(i);

            PdfPTable table = new PdfPTable(widthArray);
            table.setWidthPercentage(100);

            // Header row
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_BG_COLOR);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                table.addCell(cell);
            }

            // Data rows
            int rowIndex = 0;
            for (PokupkiRow row : rows) {
                Color rowColor = (rowIndex++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

                addCell(table, String.valueOf(row.lineNum), cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.docType, cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.docNumber, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.docDate, cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.vatNumber, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.name, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.description, cellFont, rowColor, Element.ALIGN_LEFT);

                if (showCol9) addCell(table, formatAmount(row.col9), cellFont, rowColor, Element.ALIGN_RIGHT);
                if (showCol10) addCell(table, formatAmount(row.col10), cellFont, rowColor, Element.ALIGN_RIGHT);
                if (showCol11) addCell(table, formatAmount(row.col11), cellFont, rowColor, Element.ALIGN_RIGHT);
            }

            // Total row
            int colspan = 7;
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("", totalFont));
            totalLabelCell.setColspan(colspan);
            totalLabelCell.setBorderWidth(0);
            table.addCell(totalLabelCell);

            if (showCol9) addCell(table, formatAmount(totalCol9), totalFont, Color.WHITE, Element.ALIGN_RIGHT);
            if (showCol10) addCell(table, formatAmount(totalCol10), totalFont, Color.WHITE, Element.ALIGN_RIGHT);
            if (showCol11) addCell(table, formatAmount(totalCol11), totalFont, Color.WHITE, Element.ALIGN_RIGHT);

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Грешка при генериране на PDF", e);
        }

        return baos.toByteArray();
    }

    private byte[] generateProdajbiPdf(VatReturnEntity vatReturn, List<JournalEntryEntity> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            CompanyEntity company = vatReturn.getCompany();
            String period = vatReturn.getPeriodMonth() + "/" + vatReturn.getPeriodYear();

            // Title
            Paragraph title = new Paragraph("Дневник за Продажби", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph companyInfo = new Paragraph(company.getName() + " " + company.getVatNumber(), titleFont);
            companyInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(companyInfo);

            Paragraph periodPara = new Paragraph(period, titleFont);
            periodPara.setAlignment(Element.ALIGN_CENTER);
            periodPara.setSpacingAfter(15);
            document.add(periodPara);

            // Analyze which columns have non-zero values
            List<ProdajbiRow> rows = new ArrayList<>();
            BigDecimal totalCol9 = BigDecimal.ZERO;
            BigDecimal totalCol10 = BigDecimal.ZERO;
            BigDecimal totalCol11 = BigDecimal.ZERO;
            BigDecimal totalCol12 = BigDecimal.ZERO;

            int lineNum = 1;
            for (JournalEntryEntity entry : entries) {
                CounterpartEntity counterpart = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getCounterpart).filter(Objects::nonNull).findFirst().orElse(null);

                if (counterpart == null) continue;

                BigDecimal baseAmount = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getBaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal vatAmount = entry.getEntryLines().stream()
                        .map(EntryLineEntity::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                ProdajbiRow row = new ProdajbiRow();
                row.lineNum = lineNum++;
                row.docType = entry.getVatDocumentType() != null ? entry.getVatDocumentType() : "01";
                row.docNumber = entry.getDocumentNumber();
                row.docDate = entry.getDocumentDate() != null ? entry.getDocumentDate().format(DATE_FMT) : "";
                row.vatNumber = counterpart.getVatNumber() != null ? counterpart.getVatNumber() : counterpart.getEik();
                row.name = counterpart.getName();
                row.description = entry.getDescription();

                // For prodajbi - columns 9, 10 are totals, 11, 12 are 20% base and vat
                row.col9 = baseAmount;
                row.col10 = vatAmount;
                row.col11 = baseAmount;  // Assuming all are 20%
                row.col12 = vatAmount;

                totalCol9 = totalCol9.add(row.col9);
                totalCol10 = totalCol10.add(row.col10);
                totalCol11 = totalCol11.add(row.col11);
                totalCol12 = totalCol12.add(row.col12);

                rows.add(row);
            }

            // Determine which columns to show
            boolean showCol9 = totalCol9.compareTo(BigDecimal.ZERO) != 0;
            boolean showCol10 = totalCol10.compareTo(BigDecimal.ZERO) != 0;
            boolean showCol11 = totalCol11.compareTo(BigDecimal.ZERO) != 0;
            boolean showCol12 = totalCol12.compareTo(BigDecimal.ZERO) != 0;

            // Build dynamic column widths
            List<Float> widths = new ArrayList<>(Arrays.asList(25f, 25f, 65f, 55f, 75f, 140f, 60f));
            List<String> headers = new ArrayList<>(Arrays.asList(
                    "кл. 1\n№ по ред", "кл. 3\nВид", "кл. 4\nДокумент номер", "кл. 5\nДата",
                    "кл. 6\nНомер на контр.доставчик", "кл. 7\nИме на контрагента(получател)",
                    "кл. 8\nВид на стоката услугата"
            ));

            if (showCol9) {
                widths.add(55f);
                headers.add("кл. 9\nОбщ размер на ДО за облагане с ДДС");
            }
            if (showCol10) {
                widths.add(50f);
                headers.add("кл. 10\nВсичко начислен ДДС");
            }
            if (showCol11) {
                widths.add(55f);
                headers.add("кл. 11\nДО на обл.дост. 20%");
            }
            if (showCol12) {
                widths.add(50f);
                headers.add("кл. 12\nНачислен ДДС 20%");
            }

            float[] widthArray = new float[widths.size()];
            for (int i = 0; i < widths.size(); i++) widthArray[i] = widths.get(i);

            PdfPTable table = new PdfPTable(widthArray);
            table.setWidthPercentage(100);

            // Header row
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_BG_COLOR);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                table.addCell(cell);
            }

            // Data rows
            int rowIndex = 0;
            for (ProdajbiRow row : rows) {
                Color rowColor = (rowIndex++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

                addCell(table, String.valueOf(row.lineNum), cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.docType, cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.docNumber, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.docDate, cellFont, rowColor, Element.ALIGN_CENTER);
                addCell(table, row.vatNumber, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.name, cellFont, rowColor, Element.ALIGN_LEFT);
                addCell(table, row.description, cellFont, rowColor, Element.ALIGN_LEFT);

                if (showCol9) addCell(table, formatAmount(row.col9), cellFont, rowColor, Element.ALIGN_RIGHT);
                if (showCol10) addCell(table, formatAmount(row.col10), cellFont, rowColor, Element.ALIGN_RIGHT);
                if (showCol11) addCell(table, formatAmount(row.col11), cellFont, rowColor, Element.ALIGN_RIGHT);
                if (showCol12) addCell(table, formatAmount(row.col12), cellFont, rowColor, Element.ALIGN_RIGHT);
            }

            // Total row
            int colspan = 7;
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("", totalFont));
            totalLabelCell.setColspan(colspan);
            totalLabelCell.setBorderWidth(0);
            table.addCell(totalLabelCell);

            if (showCol9) addCell(table, formatAmount(totalCol9), totalFont, Color.WHITE, Element.ALIGN_RIGHT);
            if (showCol10) addCell(table, formatAmount(totalCol10), totalFont, Color.WHITE, Element.ALIGN_RIGHT);
            if (showCol11) addCell(table, formatAmount(totalCol11), totalFont, Color.WHITE, Element.ALIGN_RIGHT);
            if (showCol12) addCell(table, formatAmount(totalCol12), totalFont, Color.WHITE, Element.ALIGN_RIGHT);

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Грешка при генериране на PDF", e);
        }

        return baos.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }

    // Inner classes for row data
    private static class PokupkiRow {
        int lineNum;
        String docType;
        String docNumber;
        String docDate;
        String vatNumber;
        String name;
        String description;
        BigDecimal col9 = BigDecimal.ZERO;
        BigDecimal col10 = BigDecimal.ZERO;
        BigDecimal col11 = BigDecimal.ZERO;
    }

    private static class ProdajbiRow {
        int lineNum;
        String docType;
        String docNumber;
        String docDate;
        String vatNumber;
        String name;
        String description;
        BigDecimal col9 = BigDecimal.ZERO;
        BigDecimal col10 = BigDecimal.ZERO;
        BigDecimal col11 = BigDecimal.ZERO;
        BigDecimal col12 = BigDecimal.ZERO;
    }
}
