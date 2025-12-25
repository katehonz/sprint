package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.*;
import bg.spacbg.sp_ac_bg.model.dto.report.*;

import java.util.List;

public interface ReportService {

    /**
     * Генерира оборотна ведомост (6 колони)
     */
    TurnoverSheet generateTurnoverSheet(TurnoverReportInput input);

    /**
     * Генерира дневник на операциите
     */
    TransactionLog generateTransactionLog(TransactionLogInput input);

    /**
     * Генерира хронологичен регистър
     */
    ChronologicalReport generateChronologicalReport(ChronologicalReportInput input);

    /**
     * Генерира главна книга
     */
    GeneralLedger generateGeneralLedger(GeneralLedgerInput input);

    /**
     * Генерира българска главна книга (групирана по дебит/кредит)
     */
    BgGeneralLedger generateBgGeneralLedger(GeneralLedgerInput input);

    /**
     * Генерира месечна статистика на транзакциите
     */
    List<MonthlyTransactionStats> generateMonthlyTransactionStats(MonthlyStatsInput input);

    /**
     * Експортира хронологичен регистър в XLSX или ODT формат
     */
    ReportExport exportChronologicalReport(ChronologicalReportInput input, String format);

    /**
     * Експортира оборотна ведомост в XLSX или ODT формат
     */
    ReportExport exportTurnoverSheet(TurnoverReportInput input, String format);

    /**
     * Експортира главна книга в XLSX или ODT формат
     */
    ReportExport exportGeneralLedger(GeneralLedgerInput input, String format);

    /**
     * Експортира българска главна книга в XLSX или ODT формат
     */
    ReportExport exportBgGeneralLedger(GeneralLedgerInput input, String format);

    /**
     * Експортира месечна статистика в XLSX или ODT формат
     */
    ReportExport exportMonthlyStats(MonthlyStatsInput input, String format);
}
