package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.dto.report.*;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final EntryLineRepository entryLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final CounterpartRepository counterpartRepository;

    private static final String[] MONTH_NAMES_BG = {
        "Януари", "Февруари", "Март", "Април", "Май", "Юни",
        "Юли", "Август", "Септември", "Октомври", "Ноември", "Декември"
    };

    @Override
    public TurnoverSheet generateTurnoverSheet(TurnoverReportInput input) {
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<AccountEntity> accounts = input.accountId() != null
                ? accountRepository.findById(input.accountId()).map(List::of).orElse(List.of())
                : accountRepository.findByCompanyIdAndIsActiveTrue(input.companyId());

        accounts = accounts.stream()
                .sorted(Comparator.comparing(AccountEntity::getCode))
                .collect(Collectors.toList());

        Map<String, AccountAggregate> aggregates = new HashMap<>();

        for (AccountEntity account : accounts) {
            final String aggCode;
            final String aggName;

            if (input.accountCodeDepth() != null && account.getCode().length() > input.accountCodeDepth()) {
                aggCode = account.getCode().substring(0, input.accountCodeDepth());
                aggName = "Сметки " + aggCode;
            } else {
                aggCode = account.getCode();
                aggName = account.getName();
            }

            // Calculate opening balance (before start_date)
            List<EntryLineEntity> openingLines = entryLineRepository
                    .findByAccountIdAndPostedBeforeDate(account.getId(), input.companyId(), input.startDate());

            BigDecimal openingDebit = BigDecimal.ZERO;
            BigDecimal openingCredit = BigDecimal.ZERO;
            for (EntryLineEntity line : openingLines) {
                openingDebit = openingDebit.add(line.getDebitAmount());
                openingCredit = openingCredit.add(line.getCreditAmount());
            }

            // Calculate period turnovers
            List<EntryLineEntity> periodLines = entryLineRepository
                    .findByAccountIdAndPostedBetweenDates(account.getId(), input.companyId(),
                            input.startDate(), input.endDate());

            BigDecimal periodDebit = BigDecimal.ZERO;
            BigDecimal periodCredit = BigDecimal.ZERO;
            for (EntryLineEntity line : periodLines) {
                periodDebit = periodDebit.add(line.getDebitAmount());
                periodCredit = periodCredit.add(line.getCreditAmount());
            }

            // Aggregate
            AccountAggregate agg = aggregates.computeIfAbsent(aggCode,
                    k -> new AccountAggregate(aggCode, aggName));
            agg.addOpening(openingDebit, openingCredit);
            agg.addPeriod(periodDebit, periodCredit);
        }

        // Build entries
        List<TurnoverSheetEntry> entries = new ArrayList<>();
        BigDecimal totalOpeningDebit = BigDecimal.ZERO;
        BigDecimal totalOpeningCredit = BigDecimal.ZERO;
        BigDecimal totalPeriodDebit = BigDecimal.ZERO;
        BigDecimal totalPeriodCredit = BigDecimal.ZERO;
        BigDecimal totalClosingDebit = BigDecimal.ZERO;
        BigDecimal totalClosingCredit = BigDecimal.ZERO;

        for (AccountAggregate agg : aggregates.values().stream()
                .sorted(Comparator.comparing(a -> a.code))
                .toList()) {

            BigDecimal closingNet = agg.openingDebit.add(agg.periodDebit)
                    .subtract(agg.openingCredit).subtract(agg.periodCredit);
            BigDecimal closingDebit = closingNet.compareTo(BigDecimal.ZERO) > 0
                    ? closingNet : BigDecimal.ZERO;
            BigDecimal closingCredit = closingNet.compareTo(BigDecimal.ZERO) < 0
                    ? closingNet.abs() : BigDecimal.ZERO;

            // Skip zero balances if requested
            boolean showZero = input.showZeroBalances() == null || input.showZeroBalances();
            if (!showZero && agg.isAllZero()) {
                continue;
            }

            entries.add(new TurnoverSheetEntry(
                    0, agg.code, agg.name,
                    agg.openingDebit, agg.openingCredit,
                    agg.periodDebit, agg.periodCredit,
                    closingDebit, closingCredit
            ));

            totalOpeningDebit = totalOpeningDebit.add(agg.openingDebit);
            totalOpeningCredit = totalOpeningCredit.add(agg.openingCredit);
            totalPeriodDebit = totalPeriodDebit.add(agg.periodDebit);
            totalPeriodCredit = totalPeriodCredit.add(agg.periodCredit);
            totalClosingDebit = totalClosingDebit.add(closingDebit);
            totalClosingCredit = totalClosingCredit.add(closingCredit);
        }

        TurnoverSheetEntry totals = new TurnoverSheetEntry(
                0, "ОБЩО", "Общо за всички сметки",
                totalOpeningDebit, totalOpeningCredit,
                totalPeriodDebit, totalPeriodCredit,
                totalClosingDebit, totalClosingCredit
        );

        return new TurnoverSheet(
                company.getName(),
                input.startDate(),
                input.endDate(),
                entries,
                totals,
                OffsetDateTime.now()
        );
    }

    @Override
    public TransactionLog generateTransactionLog(TransactionLogInput input) {
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<EntryLineEntity> lines;
        if (input.accountId() != null) {
            lines = entryLineRepository.findByAccountIdAndPostedBetweenDates(
                    input.accountId(), input.companyId(), input.startDate(), input.endDate());
        } else {
            lines = entryLineRepository.findByCompanyIdAndPostedBetweenDates(
                    input.companyId(), input.startDate(), input.endDate());
        }

        List<TransactionLogEntry> entries = lines.stream()
                .filter(line -> line.getDebitAmount().compareTo(BigDecimal.ZERO) != 0
                        || line.getCreditAmount().compareTo(BigDecimal.ZERO) != 0)
                .map(line -> {
                    JournalEntryEntity je = line.getJournalEntry();
                    AccountEntity account = line.getAccount();
                    String counterpartName = line.getCounterpart() != null
                            ? line.getCounterpart().getName() : null;

                    return new TransactionLogEntry(
                            je.getAccountingDate(),
                            je.getEntryNumber(),
                            je.getDocumentNumber(),
                            line.getDescription() != null ? line.getDescription() : je.getDescription(),
                            account.getCode(),
                            account.getName(),
                            line.getDebitAmount(),
                            line.getCreditAmount(),
                            counterpartName
                    );
                })
                .sorted(Comparator.comparing(TransactionLogEntry::date)
                        .thenComparing(TransactionLogEntry::entryNumber))
                .collect(Collectors.toList());

        return new TransactionLog(
                company.getName(),
                input.startDate(),
                input.endDate(),
                entries,
                OffsetDateTime.now()
        );
    }

    @Override
    public ChronologicalReport generateChronologicalReport(ChronologicalReportInput input) {
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<JournalEntryEntity> journalEntries = journalEntryRepository
                .findByCompanyIdAndDateRange(input.companyId(), input.startDate(), input.endDate())
                .stream()
                .filter(JournalEntryEntity::isPosted)
                .sorted(Comparator.comparing(JournalEntryEntity::getAccountingDate)
                        .thenComparing(JournalEntryEntity::getEntryNumber))
                .toList();

        List<ChronologicalEntry> chronoEntries = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (JournalEntryEntity je : journalEntries) {
            List<EntryLineEntity> lines = entryLineRepository.findByJournalEntry_Id(je.getId());

            List<EntryLineEntity> debitLines = lines.stream()
                    .filter(l -> l.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            List<EntryLineEntity> creditLines = lines.stream()
                    .filter(l -> l.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            for (EntryLineEntity debitLine : debitLines) {
                for (EntryLineEntity creditLine : creditLines) {
                    // Filter by account if specified
                    if (input.accountId() != null
                            && !debitLine.getAccount().getId().equals(input.accountId())
                            && !creditLine.getAccount().getId().equals(input.accountId())) {
                        continue;
                    }

                    BigDecimal amount = debitLine.getDebitAmount()
                            .min(creditLine.getCreditAmount());
                    totalAmount = totalAmount.add(amount);

                    chronoEntries.add(new ChronologicalEntry(
                            je.getAccountingDate(),
                            debitLine.getAccount().getCode(),
                            debitLine.getAccount().getName(),
                            creditLine.getAccount().getCode(),
                            creditLine.getAccount().getName(),
                            amount,
                            debitLine.getCurrencyAmount(),
                            debitLine.getCurrencyCode(),
                            creditLine.getCurrencyAmount(),
                            creditLine.getCurrencyCode(),
                            je.getVatDocumentType(),
                            je.getDocumentDate(),
                            debitLine.getDescription() != null ? debitLine.getDescription()
                                    : (creditLine.getDescription() != null ? creditLine.getDescription()
                                    : je.getDescription())
                    ));
                }
            }
        }

        return new ChronologicalReport(
                company.getName(),
                input.startDate(),
                input.endDate(),
                chronoEntries,
                totalAmount,
                OffsetDateTime.now()
        );
    }

    @Override
    public GeneralLedger generateGeneralLedger(GeneralLedgerInput input) {
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<AccountEntity> accounts = input.accountId() != null
                ? accountRepository.findById(input.accountId()).map(List::of).orElse(List.of())
                : accountRepository.findByCompanyIdAndIsActiveTrue(input.companyId());

        accounts = accounts.stream()
                .sorted(Comparator.comparing(AccountEntity::getCode))
                .collect(Collectors.toList());

        List<GeneralLedgerAccount> ledgerAccounts = new ArrayList<>();

        for (AccountEntity account : accounts) {
            // Opening balance
            List<EntryLineEntity> openingLines = entryLineRepository
                    .findByAccountIdAndPostedBeforeDate(account.getId(), input.companyId(), input.startDate());

            BigDecimal openingBalance = BigDecimal.ZERO;
            for (EntryLineEntity line : openingLines) {
                openingBalance = openingBalance.add(line.getDebitAmount())
                        .subtract(line.getCreditAmount());
            }

            // Period transactions
            List<EntryLineEntity> periodLines = entryLineRepository
                    .findByAccountIdAndPostedBetweenDates(account.getId(), input.companyId(),
                            input.startDate(), input.endDate());

            BigDecimal runningBalance = openingBalance;
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            List<GeneralLedgerEntry> entries = new ArrayList<>();

            for (EntryLineEntity line : periodLines.stream()
                    .filter(l -> l.getDebitAmount().compareTo(BigDecimal.ZERO) != 0
                            || l.getCreditAmount().compareTo(BigDecimal.ZERO) != 0)
                    .sorted(Comparator.comparing(l -> l.getJournalEntry().getAccountingDate()))
                    .toList()) {

                JournalEntryEntity je = line.getJournalEntry();
                runningBalance = runningBalance.add(line.getDebitAmount())
                        .subtract(line.getCreditAmount());
                totalDebits = totalDebits.add(line.getDebitAmount());
                totalCredits = totalCredits.add(line.getCreditAmount());

                String counterpartName = line.getCounterpart() != null
                        ? line.getCounterpart().getName() : null;

                entries.add(new GeneralLedgerEntry(
                        je.getAccountingDate(),
                        je.getEntryNumber(),
                        je.getDocumentNumber(),
                        line.getDescription() != null ? line.getDescription() : je.getDescription(),
                        line.getDebitAmount(),
                        line.getCreditAmount(),
                        runningBalance,
                        counterpartName
                ));
            }

            // Only include accounts with activity
            if (openingBalance.compareTo(BigDecimal.ZERO) != 0
                    || totalDebits.compareTo(BigDecimal.ZERO) != 0
                    || totalCredits.compareTo(BigDecimal.ZERO) != 0) {

                ledgerAccounts.add(new GeneralLedgerAccount(
                        account.getId(),
                        account.getCode(),
                        account.getName(),
                        openingBalance,
                        runningBalance,
                        totalDebits,
                        totalCredits,
                        entries
                ));
            }
        }

        return new GeneralLedger(
                company.getName(),
                input.startDate(),
                input.endDate(),
                ledgerAccounts,
                OffsetDateTime.now()
        );
    }

    @Override
    public BgGeneralLedger generateBgGeneralLedger(GeneralLedgerInput input) {
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<JournalEntryEntity> journalEntries = journalEntryRepository
                .findByCompanyIdAndDateRange(input.companyId(), input.startDate(), input.endDate())
                .stream()
                .filter(JournalEntryEntity::isPosted)
                .toList();

        // Map: (debitAccountId, creditAccountId) -> totalAmount
        Map<String, BigDecimal> pairsMap = new HashMap<>();
        // Map: accountId -> (code, name)
        Map<Integer, String[]> accountInfo = new HashMap<>();

        for (JournalEntryEntity je : journalEntries) {
            List<EntryLineEntity> lines = entryLineRepository.findByJournalEntry_Id(je.getId());

            List<EntryLineEntity> debitLines = lines.stream()
                    .filter(l -> l.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            List<EntryLineEntity> creditLines = lines.stream()
                    .filter(l -> l.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            for (EntryLineEntity debitLine : debitLines) {
                for (EntryLineEntity creditLine : creditLines) {
                    if (input.accountId() != null
                            && !debitLine.getAccount().getId().equals(input.accountId())
                            && !creditLine.getAccount().getId().equals(input.accountId())) {
                        continue;
                    }

                    accountInfo.putIfAbsent(debitLine.getAccount().getId(),
                            new String[]{debitLine.getAccount().getCode(), debitLine.getAccount().getName()});
                    accountInfo.putIfAbsent(creditLine.getAccount().getId(),
                            new String[]{creditLine.getAccount().getCode(), creditLine.getAccount().getName()});

                    BigDecimal amount = debitLine.getDebitAmount().min(creditLine.getCreditAmount());
                    String key = debitLine.getAccount().getId() + "-" + creditLine.getAccount().getId();
                    pairsMap.merge(key, amount, BigDecimal::add);
                }
            }
        }

        // Group by debit
        Map<Integer, List<BgLedgerByDebitEntry>> byDebitMap = new HashMap<>();
        // Group by credit
        Map<Integer, List<BgLedgerByCreditEntry>> byCreditMap = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : pairsMap.entrySet()) {
            String[] ids = entry.getKey().split("-");
            int debitId = Integer.parseInt(ids[0]);
            int creditId = Integer.parseInt(ids[1]);
            BigDecimal amount = entry.getValue();

            String[] debitInfo = accountInfo.get(debitId);
            String[] creditInfo = accountInfo.get(creditId);

            byDebitMap.computeIfAbsent(debitId, k -> new ArrayList<>())
                    .add(new BgLedgerByDebitEntry(creditInfo[0], creditInfo[1], amount));

            byCreditMap.computeIfAbsent(creditId, k -> new ArrayList<>())
                    .add(new BgLedgerByCreditEntry(debitInfo[0], debitInfo[1], amount));
        }

        List<BgLedgerByDebit> byDebit = byDebitMap.entrySet().stream()
                .map(e -> {
                    String[] info = accountInfo.get(e.getKey());
                    BigDecimal total = e.getValue().stream()
                            .map(BgLedgerByDebitEntry::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new BgLedgerByDebit(info[0], info[1], e.getValue(), total);
                })
                .sorted(Comparator.comparing(BgLedgerByDebit::debitAccountCode))
                .toList();

        List<BgLedgerByCredit> byCredit = byCreditMap.entrySet().stream()
                .map(e -> {
                    String[] info = accountInfo.get(e.getKey());
                    BigDecimal total = e.getValue().stream()
                            .map(BgLedgerByCreditEntry::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new BgLedgerByCredit(info[0], info[1], e.getValue(), total);
                })
                .sorted(Comparator.comparing(BgLedgerByCredit::creditAccountCode))
                .toList();

        return new BgGeneralLedger(
                company.getName(),
                input.startDate(),
                input.endDate(),
                byDebit,
                byCredit,
                OffsetDateTime.now()
        );
    }

    @Override
    public List<MonthlyTransactionStats> generateMonthlyTransactionStats(MonthlyStatsInput input) {
        List<MonthlyTransactionStats> stats = new ArrayList<>();

        int currentYear = input.fromYear();
        int currentMonth = input.fromMonth();

        while (currentYear < input.toYear()
                || (currentYear == input.toYear() && currentMonth <= input.toMonth())) {

            YearMonth ym = YearMonth.of(currentYear, currentMonth);
            LocalDate startDate = ym.atDay(1);
            LocalDate endDate = ym.atEndOfMonth();

            // Count entries
            List<JournalEntryEntity> entries = journalEntryRepository
                    .findByCompanyIdAndDateRange(input.companyId(), startDate, endDate);

            long totalEntries = entries.size();
            long postedEntries = entries.stream().filter(JournalEntryEntity::isPosted).count();

            // Count entry lines
            long totalEntryLines = 0;
            long postedEntryLines = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal vatAmount = BigDecimal.ZERO;

            for (JournalEntryEntity je : entries) {
                List<EntryLineEntity> lines = entryLineRepository.findByJournalEntry_Id(je.getId());
                totalEntryLines += lines.size();
                if (je.isPosted()) {
                    postedEntryLines += lines.size();
                    if (je.getTotalAmount() != null) {
                        totalAmount = totalAmount.add(je.getTotalAmount());
                    }
                    if (je.getTotalVatAmount() != null) {
                        vatAmount = vatAmount.add(je.getTotalVatAmount());
                    }
                }
            }

            stats.add(new MonthlyTransactionStats(
                    currentYear,
                    currentMonth,
                    MONTH_NAMES_BG[currentMonth - 1],
                    totalEntries,
                    postedEntries,
                    totalEntryLines,
                    postedEntryLines,
                    totalAmount,
                    vatAmount
            ));

            // Move to next month
            if (currentMonth == 12) {
                currentYear++;
                currentMonth = 1;
            } else {
                currentMonth++;
            }
        }

        return stats;
    }

    @Override
    public ReportExport exportChronologicalReport(ChronologicalReportInput input, String format) {
        ChronologicalReport report = generateChronologicalReport(input);
        String filename = String.format("chronological_report_%s_%s_%s",
                report.periodStart(), report.periodEnd(), report.companyName().replace(" ", "_"));

        if ("XLSX".equalsIgnoreCase(format)) {
            byte[] content = generateXlsxChronological(report);
            return new ReportExport("XLSX", Base64.getEncoder().encodeToString(content),
                    filename + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("PDF".equalsIgnoreCase(format)) {
            byte[] content = generatePdfChronological(report);
            return new ReportExport("PDF", Base64.getEncoder().encodeToString(content),
                    filename + ".pdf",
                    "application/pdf");
        } else {
            throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    @Override
    public ReportExport exportTurnoverSheet(TurnoverReportInput input, String format) {
        TurnoverSheet report = generateTurnoverSheet(input);
        String filename = String.format("turnover_sheet_%s_%s_%s",
                report.periodStart(), report.periodEnd(), report.companyName().replace(" ", "_"));

        if ("XLSX".equalsIgnoreCase(format)) {
            byte[] content = generateXlsxTurnover(report);
            return new ReportExport("XLSX", Base64.getEncoder().encodeToString(content),
                    filename + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("PDF".equalsIgnoreCase(format)) {
            byte[] content = generatePdfTurnover(report);
            return new ReportExport("PDF", Base64.getEncoder().encodeToString(content),
                    filename + ".pdf",
                    "application/pdf");
        } else {
            throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    @Override
    public ReportExport exportGeneralLedger(GeneralLedgerInput input, String format) {
        GeneralLedger report = generateGeneralLedger(input);
        String filename = String.format("general_ledger_%s_%s_%s",
                report.periodStart(), report.periodEnd(), report.companyName().replace(" ", "_"));

        if ("XLSX".equalsIgnoreCase(format)) {
            byte[] content = generateXlsxGeneralLedger(report);
            return new ReportExport("XLSX", Base64.getEncoder().encodeToString(content),
                    filename + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("PDF".equalsIgnoreCase(format)) {
            byte[] content = generatePdfGeneralLedger(report);
            return new ReportExport("PDF", Base64.getEncoder().encodeToString(content),
                    filename + ".pdf",
                    "application/pdf");
        } else {
            throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    @Override
    public ReportExport exportBgGeneralLedger(GeneralLedgerInput input, String format) {
        BgGeneralLedger report = generateBgGeneralLedger(input);
        String filename = String.format("bg_general_ledger_%s_%s_%s",
                report.periodStart(), report.periodEnd(), report.companyName().replace(" ", "_"));

        if ("XLSX".equalsIgnoreCase(format)) {
            byte[] content = generateXlsxBgGeneralLedger(report);
            return new ReportExport("XLSX", Base64.getEncoder().encodeToString(content),
                    filename + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    @Override
    public ReportExport exportMonthlyStats(MonthlyStatsInput input, String format) {
        List<MonthlyTransactionStats> stats = generateMonthlyTransactionStats(input);
        CompanyEntity company = companyRepository.findById(input.companyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String filename = String.format("monthly_stats_%d_%02d_%d_%02d",
                input.fromYear(), input.fromMonth(), input.toYear(), input.toMonth());

        if ("XLSX".equalsIgnoreCase(format)) {
            byte[] content = generateXlsxMonthlyStats(stats, company.getName());
            return new ReportExport("XLSX", Base64.getEncoder().encodeToString(content),
                    filename + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    // XLSX generation helpers
    private byte[] generateXlsxTurnover(TurnoverSheet sheet) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet worksheet = workbook.createSheet("Оборотна ведомост");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle totalsStyle = createTotalsStyle(workbook);

            int rowNum = 0;

            // Title
            Row titleRow = worksheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Оборотна ведомост - " + sheet.companyName());
            worksheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Period
            Row periodRow = worksheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Период: " + sheet.periodStart() + " - " + sheet.periodEnd());
            worksheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
            rowNum++;

            // Headers
            Row headerRow = worksheet.createRow(rowNum++);
            String[] headers = {"Код", "Име", "Начално Дт", "Начално Кт",
                    "Обороти Дт", "Обороти Кт", "Крайно Дт", "Крайно Кт"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            for (TurnoverSheetEntry entry : sheet.entries()) {
                Row row = worksheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.accountCode());
                row.createCell(1).setCellValue(entry.accountName());
                createNumberCell(row, 2, entry.openingDebit(), numberStyle);
                createNumberCell(row, 3, entry.openingCredit(), numberStyle);
                createNumberCell(row, 4, entry.periodDebit(), numberStyle);
                createNumberCell(row, 5, entry.periodCredit(), numberStyle);
                createNumberCell(row, 6, entry.closingDebit(), numberStyle);
                createNumberCell(row, 7, entry.closingCredit(), numberStyle);
            }

            // Totals
            Row totalsRow = worksheet.createRow(rowNum);
            totalsRow.createCell(0).setCellValue(sheet.totals().accountCode());
            totalsRow.createCell(1).setCellValue(sheet.totals().accountName());
            createNumberCell(totalsRow, 2, sheet.totals().openingDebit(), totalsStyle);
            createNumberCell(totalsRow, 3, sheet.totals().openingCredit(), totalsStyle);
            createNumberCell(totalsRow, 4, sheet.totals().periodDebit(), totalsStyle);
            createNumberCell(totalsRow, 5, sheet.totals().periodCredit(), totalsStyle);
            createNumberCell(totalsRow, 6, sheet.totals().closingDebit(), totalsStyle);
            createNumberCell(totalsRow, 7, sheet.totals().closingCredit(), totalsStyle);

            // Auto-size columns
            for (int i = 0; i < 8; i++) {
                worksheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating XLSX", e);
        }
    }

    private byte[] generateXlsxChronological(ChronologicalReport report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet worksheet = workbook.createSheet("Хронологичен регистър");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;

            // Title
            Row titleRow = worksheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Хронологичен регистър - " + report.companyName());
            rowNum++;

            // Headers
            Row headerRow = worksheet.createRow(rowNum++);
            String[] headers = {"Дата", "Дебит код", "Дебит име", "Кредит код",
                    "Кредит име", "Сума", "Описание"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            for (ChronologicalEntry entry : report.entries()) {
                Row row = worksheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.date().toString());
                row.createCell(1).setCellValue(entry.debitAccountCode());
                row.createCell(2).setCellValue(entry.debitAccountName());
                row.createCell(3).setCellValue(entry.creditAccountCode());
                row.createCell(4).setCellValue(entry.creditAccountName());
                createNumberCell(row, 5, entry.amount(), numberStyle);
                row.createCell(6).setCellValue(entry.description());
            }

            for (int i = 0; i < 7; i++) {
                worksheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating XLSX", e);
        }
    }

    private byte[] generateXlsxGeneralLedger(GeneralLedger report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet worksheet = workbook.createSheet("Главна книга");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;

            // Title
            Row titleRow = worksheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Главна книга - " + report.companyName());
            rowNum++;

            for (GeneralLedgerAccount account : report.accounts()) {
                // Account header
                Row accountRow = worksheet.createRow(rowNum++);
                accountRow.createCell(0).setCellValue(account.accountCode() + " - " + account.accountName());
                accountRow.createCell(1).setCellValue("Начално: " + account.openingBalance());
                accountRow.createCell(2).setCellValue("Крайно: " + account.closingBalance());
                rowNum++;

                // Column headers
                Row headerRow = worksheet.createRow(rowNum++);
                String[] headers = {"Дата", "Документ", "Описание", "Дебит", "Кредит", "Салдо", "Контрагент"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Entries
                for (GeneralLedgerEntry entry : account.entries()) {
                    Row row = worksheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.date().toString());
                    row.createCell(1).setCellValue(entry.entryNumber() +
                            (entry.documentNumber() != null ? " (" + entry.documentNumber() + ")" : ""));
                    row.createCell(2).setCellValue(entry.description());
                    createNumberCell(row, 3, entry.debitAmount(), numberStyle);
                    createNumberCell(row, 4, entry.creditAmount(), numberStyle);
                    createNumberCell(row, 5, entry.balance(), numberStyle);
                    row.createCell(6).setCellValue(entry.counterpartName() != null ? entry.counterpartName() : "");
                }

                rowNum++;
            }

            for (int i = 0; i < 7; i++) {
                worksheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating XLSX", e);
        }
    }

    private byte[] generateXlsxBgGeneralLedger(BgGeneralLedger report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet by Debit
            Sheet sheetByDebit = workbook.createSheet("По дебит");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;
            Row titleRow = sheetByDebit.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Главна книга (БГ вариант) - " + report.companyName());
            rowNum++;

            for (BgLedgerByDebit debit : report.byDebit()) {
                Row accountRow = sheetByDebit.createRow(rowNum++);
                accountRow.createCell(0).setCellValue("Дт " + debit.debitAccountCode() + " - " + debit.debitAccountName());

                for (BgLedgerByDebitEntry entry : debit.entries()) {
                    Row row = sheetByDebit.createRow(rowNum++);
                    row.createCell(1).setCellValue("Кт " + entry.creditAccountCode());
                    row.createCell(2).setCellValue(entry.creditAccountName());
                    createNumberCell(row, 3, entry.amount(), numberStyle);
                }

                Row totalRow = sheetByDebit.createRow(rowNum++);
                totalRow.createCell(2).setCellValue("Общо:");
                createNumberCell(totalRow, 3, debit.totalAmount(), numberStyle);
                rowNum++;
            }

            // Sheet by Credit
            Sheet sheetByCredit = workbook.createSheet("По кредит");
            rowNum = 0;
            titleRow = sheetByCredit.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Главна книга (БГ вариант) - " + report.companyName());
            rowNum++;

            for (BgLedgerByCredit credit : report.byCredit()) {
                Row accountRow = sheetByCredit.createRow(rowNum++);
                accountRow.createCell(0).setCellValue("Кт " + credit.creditAccountCode() + " - " + credit.creditAccountName());

                for (BgLedgerByCreditEntry entry : credit.entries()) {
                    Row row = sheetByCredit.createRow(rowNum++);
                    row.createCell(1).setCellValue("Дт " + entry.debitAccountCode());
                    row.createCell(2).setCellValue(entry.debitAccountName());
                    createNumberCell(row, 3, entry.amount(), numberStyle);
                }

                Row totalRow = sheetByCredit.createRow(rowNum++);
                totalRow.createCell(2).setCellValue("Общо:");
                createNumberCell(totalRow, 3, credit.totalAmount(), numberStyle);
                rowNum++;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating XLSX", e);
        }
    }

    private byte[] generateXlsxMonthlyStats(List<MonthlyTransactionStats> stats, String companyName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet worksheet = workbook.createSheet("Месечна статистика");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;

            Row titleRow = worksheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("Месечна статистика - " + companyName);
            rowNum++;

            Row headerRow = worksheet.createRow(rowNum++);
            String[] headers = {"Период", "Документи", "Приключени",
                    "Редове", "Приключени редове", "Оборот", "ДДС"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (MonthlyTransactionStats stat : stats) {
                Row row = worksheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stat.monthName() + " " + stat.year());
                row.createCell(1).setCellValue(stat.totalEntries());
                row.createCell(2).setCellValue(stat.postedEntries());
                row.createCell(3).setCellValue(stat.totalEntryLines());
                row.createCell(4).setCellValue(stat.postedEntryLines());
                createNumberCell(row, 5, stat.totalAmount(), numberStyle);
                createNumberCell(row, 6, stat.vatAmount(), numberStyle);
            }

            for (int i = 0; i < 7; i++) {
                worksheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating XLSX", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createTotalsStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void createNumberCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    // Helper class for aggregation
    private static class AccountAggregate {
        String code;
        String name;
        BigDecimal openingDebit = BigDecimal.ZERO;
        BigDecimal openingCredit = BigDecimal.ZERO;
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;

        AccountAggregate(String code, String name) {
            this.code = code;
            this.name = name;
        }

        void addOpening(BigDecimal debit, BigDecimal credit) {
            openingDebit = openingDebit.add(debit);
            openingCredit = openingCredit.add(credit);
        }

        void addPeriod(BigDecimal debit, BigDecimal credit) {
            periodDebit = periodDebit.add(debit);
            periodCredit = periodCredit.add(credit);
        }

        boolean isAllZero() {
            return openingDebit.compareTo(BigDecimal.ZERO) == 0
                    && openingCredit.compareTo(BigDecimal.ZERO) == 0
                    && periodDebit.compareTo(BigDecimal.ZERO) == 0
                    && periodCredit.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    // ========== PDF Generation Methods ==========

    private com.lowagie.text.Font getPdfFont(int size, boolean bold) {
        try {
            BaseFont baseFont = BaseFont.createFont("fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new com.lowagie.text.Font(baseFont, size, bold ? com.lowagie.text.Font.BOLD : com.lowagie.text.Font.NORMAL);
        } catch (Exception e) {
            // Fallback to Helvetica if custom font not available
            return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, bold ? com.lowagie.text.Font.BOLD : com.lowagie.text.Font.NORMAL);
        }
    }

    private PdfPCell createPdfHeaderCell(String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createPdfCell(String text, com.lowagie.text.Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3);
        return cell;
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%,.2f", value);
    }

    private byte[] generatePdfTurnover(TurnoverSheet sheet) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = getPdfFont(14, true);
            com.lowagie.text.Font headerFont = getPdfFont(9, true);
            com.lowagie.text.Font dataFont = getPdfFont(8, false);
            com.lowagie.text.Font totalsFont = getPdfFont(9, true);

            // Title
            Paragraph title = new Paragraph("Оборотна ведомост - " + sheet.companyName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Период: " + sheet.periodStart() + " - " + sheet.periodEnd(), dataFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(15);
            document.add(period);

            // Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{10, 25, 10, 10, 10, 10, 10, 10});

            // Headers
            table.addCell(createPdfHeaderCell("Код", headerFont));
            table.addCell(createPdfHeaderCell("Наименование", headerFont));
            table.addCell(createPdfHeaderCell("Нач. Дт", headerFont));
            table.addCell(createPdfHeaderCell("Нач. Кт", headerFont));
            table.addCell(createPdfHeaderCell("Обор. Дт", headerFont));
            table.addCell(createPdfHeaderCell("Обор. Кт", headerFont));
            table.addCell(createPdfHeaderCell("Край. Дт", headerFont));
            table.addCell(createPdfHeaderCell("Край. Кт", headerFont));

            // Data
            for (TurnoverSheetEntry entry : sheet.entries()) {
                table.addCell(createPdfCell(entry.accountCode(), dataFont, Element.ALIGN_LEFT));
                table.addCell(createPdfCell(entry.accountName(), dataFont, Element.ALIGN_LEFT));
                table.addCell(createPdfCell(formatNumber(entry.openingDebit()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(formatNumber(entry.openingCredit()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(formatNumber(entry.periodDebit()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(formatNumber(entry.periodCredit()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(formatNumber(entry.closingDebit()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(formatNumber(entry.closingCredit()), dataFont, Element.ALIGN_RIGHT));
            }

            // Totals
            PdfPCell totalsLabel = createPdfCell("ОБЩО", totalsFont, Element.ALIGN_LEFT);
            totalsLabel.setColspan(2);
            totalsLabel.setBackgroundColor(new Color(220, 220, 220));
            table.addCell(totalsLabel);

            TurnoverSheetEntry totals = sheet.totals();
            for (BigDecimal val : Arrays.asList(totals.openingDebit(), totals.openingCredit(),
                    totals.periodDebit(), totals.periodCredit(),
                    totals.closingDebit(), totals.closingCredit())) {
                PdfPCell cell = createPdfCell(formatNumber(val), totalsFont, Element.ALIGN_RIGHT);
                cell.setBackgroundColor(new Color(220, 220, 220));
                table.addCell(cell);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private byte[] generatePdfChronological(ChronologicalReport report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = getPdfFont(14, true);
            com.lowagie.text.Font headerFont = getPdfFont(9, true);
            com.lowagie.text.Font dataFont = getPdfFont(8, false);

            // Title
            Paragraph title = new Paragraph("Хронологичен регистър - " + report.companyName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Период: " + report.periodStart() + " - " + report.periodEnd(), dataFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(15);
            document.add(period);

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12, 22, 22, 14, 30});

            // Headers
            table.addCell(createPdfHeaderCell("Дата", headerFont));
            table.addCell(createPdfHeaderCell("Дебит сметка", headerFont));
            table.addCell(createPdfHeaderCell("Кредит сметка", headerFont));
            table.addCell(createPdfHeaderCell("Сума", headerFont));
            table.addCell(createPdfHeaderCell("Описание", headerFont));

            // Data
            for (ChronologicalEntry entry : report.entries()) {
                table.addCell(createPdfCell(entry.date().toString(), dataFont, Element.ALIGN_LEFT));
                table.addCell(createPdfCell(entry.debitAccountCode() + " " + entry.debitAccountName(), dataFont, Element.ALIGN_LEFT));
                table.addCell(createPdfCell(entry.creditAccountCode() + " " + entry.creditAccountName(), dataFont, Element.ALIGN_LEFT));
                table.addCell(createPdfCell(formatNumber(entry.amount()), dataFont, Element.ALIGN_RIGHT));
                table.addCell(createPdfCell(entry.description() != null ? entry.description() : "", dataFont, Element.ALIGN_LEFT));
            }

            document.add(table);

            // Total
            Paragraph total = new Paragraph("Общо: " + formatNumber(report.totalAmount()), headerFont);
            total.setSpacingBefore(10);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private byte[] generatePdfGeneralLedger(GeneralLedger report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = getPdfFont(14, true);
            com.lowagie.text.Font headerFont = getPdfFont(9, true);
            com.lowagie.text.Font dataFont = getPdfFont(8, false);
            com.lowagie.text.Font accountFont = getPdfFont(10, true);

            // Title
            Paragraph title = new Paragraph("Главна книга - " + report.companyName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Период: " + report.periodStart() + " - " + report.periodEnd(), dataFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(15);
            document.add(period);

            // Each account
            for (GeneralLedgerAccount account : report.accounts()) {
                Paragraph accountTitle = new Paragraph(
                        account.accountCode() + " - " + account.accountName() +
                        " (Нач: " + formatNumber(account.openingBalance()) +
                        ", Край: " + formatNumber(account.closingBalance()) + ")",
                        accountFont);
                accountTitle.setSpacingBefore(10);
                accountTitle.setSpacingAfter(5);
                document.add(accountTitle);

                if (account.entries() != null && !account.entries().isEmpty()) {
                    PdfPTable table = new PdfPTable(6);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{12, 15, 30, 13, 13, 13});

                    table.addCell(createPdfHeaderCell("Дата", headerFont));
                    table.addCell(createPdfHeaderCell("Документ", headerFont));
                    table.addCell(createPdfHeaderCell("Описание", headerFont));
                    table.addCell(createPdfHeaderCell("Дебит", headerFont));
                    table.addCell(createPdfHeaderCell("Кредит", headerFont));
                    table.addCell(createPdfHeaderCell("Салдо", headerFont));

                    for (GeneralLedgerEntry entry : account.entries()) {
                        table.addCell(createPdfCell(entry.date().toString(), dataFont, Element.ALIGN_LEFT));
                        table.addCell(createPdfCell(entry.documentNumber() != null ? entry.documentNumber() : entry.entryNumber(), dataFont, Element.ALIGN_LEFT));
                        table.addCell(createPdfCell(entry.description() != null ? entry.description() : "", dataFont, Element.ALIGN_LEFT));
                        table.addCell(createPdfCell(formatNumber(entry.debitAmount()), dataFont, Element.ALIGN_RIGHT));
                        table.addCell(createPdfCell(formatNumber(entry.creditAmount()), dataFont, Element.ALIGN_RIGHT));
                        table.addCell(createPdfCell(formatNumber(entry.balance()), dataFont, Element.ALIGN_RIGHT));
                    }

                    document.add(table);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}
