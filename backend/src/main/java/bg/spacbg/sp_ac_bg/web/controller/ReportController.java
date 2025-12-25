package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.dto.report.*;
import bg.spacbg.sp_ac_bg.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ============= QUERIES =============

    /**
     * Генерира оборотна ведомост (6 колони)
     */
    @QueryMapping
    public TurnoverSheet turnoverSheet(@Argument TurnoverReportInput input) {
        return reportService.generateTurnoverSheet(input);
    }

    /**
     * Генерира дневник на операциите
     */
    @QueryMapping
    public TransactionLog transactionLog(@Argument TransactionLogInput input) {
        return reportService.generateTransactionLog(input);
    }

    /**
     * Генерира хронологичен регистър
     */
    @QueryMapping
    public ChronologicalReport chronologicalReport(@Argument ChronologicalReportInput input) {
        return reportService.generateChronologicalReport(input);
    }

    /**
     * Генерира главна книга
     */
    @QueryMapping
    public GeneralLedger generalLedger(@Argument GeneralLedgerInput input) {
        return reportService.generateGeneralLedger(input);
    }

    /**
     * Генерира българска главна книга (групирана по дебит/кредит)
     */
    @QueryMapping
    public BgGeneralLedger bgGeneralLedger(@Argument GeneralLedgerInput input) {
        return reportService.generateBgGeneralLedger(input);
    }

    /**
     * Генерира месечна статистика на транзакциите
     */
    @QueryMapping
    public List<MonthlyTransactionStats> monthlyTransactionStats(@Argument MonthlyStatsInput input) {
        return reportService.generateMonthlyTransactionStats(input);
    }

    // ============= MUTATIONS (EXPORTS) =============

    /**
     * Експортира хронологичен регистър
     */
    @MutationMapping
    public ReportExport exportChronologicalReport(
            @Argument ChronologicalReportInput input,
            @Argument String format) {
        return reportService.exportChronologicalReport(input, format);
    }

    /**
     * Експортира оборотна ведомост
     */
    @MutationMapping
    public ReportExport exportTurnoverSheet(
            @Argument TurnoverReportInput input,
            @Argument String format) {
        return reportService.exportTurnoverSheet(input, format);
    }

    /**
     * Експортира главна книга
     */
    @MutationMapping
    public ReportExport exportGeneralLedger(
            @Argument GeneralLedgerInput input,
            @Argument String format) {
        return reportService.exportGeneralLedger(input, format);
    }

    /**
     * Експортира българска главна книга
     */
    @MutationMapping
    public ReportExport exportBgGeneralLedger(
            @Argument GeneralLedgerInput input,
            @Argument String format) {
        return reportService.exportBgGeneralLedger(input, format);
    }

    /**
     * Експортира месечна статистика
     */
    @MutationMapping
    public ReportExport exportMonthlyStats(
            @Argument MonthlyStatsInput input,
            @Argument String format) {
        return reportService.exportMonthlyStats(input, format);
    }
}
