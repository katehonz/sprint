package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.GenerateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.entity.*;
import bg.spacbg.sp_ac_bg.model.enums.VatReturnStatus;
import bg.spacbg.sp_ac_bg.repository.*;
import bg.spacbg.sp_ac_bg.service.VatService;
import bg.spacbg.sp_ac_bg.service.util.VatExportFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VatServiceImpl implements VatService {

    private static final Logger log = LoggerFactory.getLogger(VatServiceImpl.class);

    private final VatRateRepository vatRateRepository;
    private final VatReturnRepository vatReturnRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JournalEntryRepository journalEntryRepository;

    public VatServiceImpl(
            VatRateRepository vatRateRepository,
            VatReturnRepository vatReturnRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            JournalEntryRepository journalEntryRepository) {
        this.vatRateRepository = vatRateRepository;
        this.vatReturnRepository = vatReturnRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    // ========== VAT Rate Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<VatRateEntity> findRatesByCompanyId(Integer companyId) {
        return vatRateRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VatRateEntity> findActiveRatesByCompanyId(Integer companyId) {
        return vatRateRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VatRateEntity> findRateById(Integer id) {
        return vatRateRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VatRateEntity> findRateByCode(String code) {
        return vatRateRepository.findByCode(code);
    }

    @Override
    public VatRateEntity createRate(CreateVatRateInput input) {
        if (vatRateRepository.existsByCode(input.getCode())) {
            throw new IllegalArgumentException("ДДС ставка с код " + input.getCode() + " вече съществува");
        }

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        VatRateEntity rate = new VatRateEntity();
        rate.setCode(input.getCode());
        rate.setName(input.getName());
        rate.setRate(input.getRate());
        rate.setVatDirection(input.getVatDirection());
        rate.setValidFrom(input.getValidFrom());
        rate.setValidTo(input.getValidTo());
        rate.setCompany(company);
        rate.setActive(true);

        return vatRateRepository.save(rate);
    }

    @Override
    public VatRateEntity updateRate(Integer id, UpdateVatRateInput input) {
        VatRateEntity rate = vatRateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ДДС ставката не е намерена: " + id));

        if (input.getName() != null) rate.setName(input.getName());
        if (input.getRate() != null) rate.setRate(input.getRate());
        if (input.getVatDirection() != null) rate.setVatDirection(input.getVatDirection());
        if (input.getValidTo() != null) rate.setValidTo(input.getValidTo());
        if (input.getIsActive() != null) rate.setActive(input.getIsActive());

        return vatRateRepository.save(rate);
    }

    @Override
    public boolean deleteRate(Integer id) {
        if (!vatRateRepository.existsById(id)) {
            return false;
        }
        vatRateRepository.deleteById(id);
        return true;
    }

    // ========== VAT Return Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<VatReturnEntity> findReturnsByCompanyId(Integer companyId) {
        return vatReturnRepository.findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VatReturnEntity> findReturnById(Integer id) {
        return vatReturnRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VatReturnEntity> findReturnByPeriod(Integer companyId, Integer year, Integer month) {
        return vatReturnRepository.findByCompanyIdAndPeriodYearAndPeriodMonth(companyId, year, month);
    }

    @Override
    public VatReturnEntity generateReturn(GenerateVatReturnInput input, Integer userId) {
        Optional<VatReturnEntity> existingReturnOpt = vatReturnRepository.findByCompanyIdAndPeriodYearAndPeriodMonth(
                input.getCompanyId(), input.getPeriodYear(), input.getPeriodMonth());

        VatReturnEntity vatReturn;
        if (existingReturnOpt.isPresent()) {
            vatReturn = existingReturnOpt.get();
            if (vatReturn.getStatus() != VatReturnStatus.DRAFT) {
                throw new IllegalStateException("Декларацията не е в статус 'чернова' и не може да се преизчисли.");
            }
        } else {
            CompanyEntity company = companyRepository.findById(input.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));
            UserEntity user = userRepository.findById(userId).orElse(null);

            vatReturn = new VatReturnEntity();
            vatReturn.setCompany(company);
            vatReturn.setPeriodYear(input.getPeriodYear());
            vatReturn.setPeriodMonth(input.getPeriodMonth());
            vatReturn.setCreatedBy(user);
        }

        YearMonth yearMonth = YearMonth.of(input.getPeriodYear(), input.getPeriodMonth());
        LocalDate periodFrom = yearMonth.atDay(1);
        LocalDate periodTo = yearMonth.atEndOfMonth();
        LocalDate dueDate = periodTo.plusDays(14);

        vatReturn.setPeriodFrom(periodFrom);
        vatReturn.setPeriodTo(periodTo);
        vatReturn.setDueDate(dueDate);
        vatReturn.setStatus(VatReturnStatus.DRAFT);
        
        // Calculate and fill amounts
        calculateAndFillVatAmounts(vatReturn, periodFrom, periodTo);

        return vatReturnRepository.save(vatReturn);
    }

    private void calculateAndFillVatAmounts(VatReturnEntity vatReturn, LocalDate periodFrom, LocalDate periodTo) {
        List<JournalEntryEntity> entries = journalEntryRepository.findByCompanyIdAndVatDateBetween(
                vatReturn.getCompany().getId(), periodFrom, periodTo
        );

        // Sales accumulators
        BigDecimal salesBase20 = BigDecimal.ZERO;
        BigDecimal salesVat20 = BigDecimal.ZERO;
        BigDecimal salesBase9 = BigDecimal.ZERO;
        BigDecimal salesVat9 = BigDecimal.ZERO;
        BigDecimal salesBaseVop = BigDecimal.ZERO;
        BigDecimal salesVatVop = BigDecimal.ZERO;
        BigDecimal salesBase0Export = BigDecimal.ZERO;
        BigDecimal salesBase0Vod = BigDecimal.ZERO;
        BigDecimal salesBase0Art3 = BigDecimal.ZERO;
        BigDecimal salesBaseExempt = BigDecimal.ZERO;
        BigDecimal salesBaseArt21 = BigDecimal.ZERO;
        BigDecimal salesBaseArt69 = BigDecimal.ZERO;
        BigDecimal salesVatPersonalUse = BigDecimal.ZERO;
        int salesDocCount = 0;

        // Purchase accumulators
        BigDecimal purchaseBaseFullCredit = BigDecimal.ZERO;
        BigDecimal purchaseVatFullCredit = BigDecimal.ZERO;
        BigDecimal purchaseBasePartialCredit = BigDecimal.ZERO;
        BigDecimal purchaseVatPartialCredit = BigDecimal.ZERO;
        BigDecimal purchaseBaseNoCredit = BigDecimal.ZERO;
        int purchaseDocCount = 0;

        for (JournalEntryEntity entry : entries) {
            BigDecimal totalBase = entry.getEntryLines().stream().map(EntryLineEntity::getBaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalVat = entry.getEntryLines().stream().map(EntryLineEntity::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Sales Ledger
            if (entry.getVatSalesOperation() != null) {
                salesDocCount++;
                switch (entry.getVatSalesOperation()) {
                    case "про11": // ДО 20%
                        salesBase20 = salesBase20.add(totalBase);
                        salesVat20 = salesVat20.add(totalVat);
                        break;
                    case "про12": // ДО 9%
                        salesBase9 = salesBase9.add(totalBase);
                        salesVat9 = salesVat9.add(totalVat);
                        break;
                    case "про13": // ДО 0% по глава 3
                        salesBase0Art3 = salesBase0Art3.add(totalBase);
                        break;
                    case "про15": // Износ
                        salesBase0Export = salesBase0Export.add(totalBase);
                        break;
                    case "про16": // ВОД
                        salesBase0Vod = salesBase0Vod.add(totalBase);
                        break;
                    case "про17": // Доставки по чл.69 ал.2
                        salesBaseArt69 = salesBaseArt69.add(totalBase);
                        break;
                    case "про19": // Освободени доставки
                        salesBaseExempt = salesBaseExempt.add(totalBase);
                        break;
                    case "про14": // Услуги по чл.21
                        salesBaseArt21 = salesBaseArt21.add(totalBase);
                        break;
                    case "про20": // Лични нужди
                        salesVatPersonalUse = salesVatPersonalUse.add(totalVat);
                        break;
                }
            }

            // Purchase Ledger
            if (entry.getVatPurchaseOperation() != null) {
                purchaseDocCount++;
                switch (entry.getVatPurchaseOperation()) {
                    case "пок30": // Покупки с ДК 20%
                        purchaseBaseFullCredit = purchaseBaseFullCredit.add(totalBase);
                        purchaseVatFullCredit = purchaseVatFullCredit.add(totalVat);
                        break;
                    case "пок32": // Покупки с ДК 9%
                        purchaseBaseFullCredit = purchaseBaseFullCredit.add(totalBase);
                        purchaseVatFullCredit = purchaseVatFullCredit.add(totalVat);
                        break;
                    case "пок09": // ВОП стоки
                        salesBaseVop = salesBaseVop.add(totalBase);
                        salesVatVop = salesVatVop.add(totalVat);
                        // This is also a purchase, so it goes into the purchase ledger as well
                        purchaseBaseFullCredit = purchaseBaseFullCredit.add(totalBase);
                        purchaseVatFullCredit = purchaseVatFullCredit.add(totalVat);
                        break;
                    case "пок31": // Частичен ДК
                        purchaseBasePartialCredit = purchaseBasePartialCredit.add(totalBase);
                        purchaseVatPartialCredit = purchaseVatPartialCredit.add(totalVat);
                        break;
                    case "пок40": // Без право на ДК
                        purchaseBaseNoCredit = purchaseBaseNoCredit.add(totalBase);
                        break;
                }
            }
        }

        vatReturn.setSalesBase20(salesBase20.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesVat20(salesVat20.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBase9(salesBase9.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesVat9(salesVat9.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBaseVop(salesBaseVop.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesVatVop(salesVatVop.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBase0Export(salesBase0Export.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBase0Vod(salesBase0Vod.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBase0Art3(salesBase0Art3.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBaseExempt(salesBaseExempt.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBaseArt21(salesBaseArt21.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesBaseArt69(salesBaseArt69.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesVatPersonalUse(salesVatPersonalUse.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setSalesDocumentCount(salesDocCount);

        vatReturn.setPurchaseBaseFullCredit(purchaseBaseFullCredit.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setPurchaseVatFullCredit(purchaseVatFullCredit.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setPurchaseBasePartialCredit(purchaseBasePartialCredit.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setPurchaseVatPartialCredit(purchaseVatPartialCredit.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setPurchaseBaseNoCredit(purchaseBaseNoCredit.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setPurchaseVatAnnualAdjustment(BigDecimal.ZERO);
        vatReturn.setPurchaseDocumentCount(purchaseDocCount);

        // Final calculation
        BigDecimal totalOutputVat = salesVat20.add(salesVat9).add(salesVatVop).add(salesVatPersonalUse);
        BigDecimal totalInputVat = purchaseVatFullCredit.add(purchaseVatPartialCredit);

        vatReturn.setOutputVatAmount(totalOutputVat.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setInputVatAmount(totalInputVat.setScale(2, RoundingMode.HALF_UP));

        // Set legacy/summary fields
        vatReturn.setBaseAmount20(salesBase20.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setVatAmount20(salesVat20.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setBaseAmount9(salesBase9.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setVatAmount9(salesVat9.setScale(2, RoundingMode.HALF_UP));
        vatReturn.setBaseAmount0(salesBase0Export.add(salesBase0Vod).add(salesBase0Art3).setScale(2, RoundingMode.HALF_UP));
        vatReturn.setExemptAmount(salesBaseExempt.setScale(2, RoundingMode.HALF_UP));

        // Credit coefficient (default 1.0 for full credit)
        vatReturn.setCreditCoefficient(BigDecimal.ONE);
        vatReturn.setTotalDeductibleVat(totalInputVat.setScale(2, RoundingMode.HALF_UP));

        BigDecimal result = totalOutputVat.subtract(totalInputVat);
        if (result.compareTo(BigDecimal.ZERO) > 0) {
            vatReturn.setVatToPay(result.setScale(2, RoundingMode.HALF_UP));
            vatReturn.setVatToRefund(BigDecimal.ZERO);
        } else {
            vatReturn.setVatToPay(BigDecimal.ZERO);
            vatReturn.setVatToRefund(result.abs().setScale(2, RoundingMode.HALF_UP));
        }

        vatReturn.setStatus(VatReturnStatus.CALCULATED);
        vatReturn.setCalculatedAt(OffsetDateTime.now());
    }


    @Override
    public VatReturnEntity submitReturn(Integer id, Integer userId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + id));

        if (vatReturn.getStatus() == VatReturnStatus.SUBMITTED ||
            vatReturn.getStatus() == VatReturnStatus.ACCEPTED) {
            throw new IllegalStateException("ДДС декларацията вече е подадена");
        }

        UserEntity user = userRepository.findById(userId).orElse(null);

        vatReturn.setStatus(VatReturnStatus.SUBMITTED);
        vatReturn.setSubmittedAt(OffsetDateTime.now());
        vatReturn.setSubmittedBy(user);

        return vatReturnRepository.save(vatReturn);
    }

    @Override
    public VatReturnEntity updateReturn(Integer id, UpdateVatReturnInput input, Integer userId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + id));

        if (vatReturn.getStatus() != VatReturnStatus.DRAFT && vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("Декларацията може да се редактира само в статус 'чернова' или 'изчислена'");
        }

        log.info("Updating VAT Return {} with manual inputs by user {}", id, userId);

        if (input.getVatToPay() != null) {
            vatReturn.setVatToPay(input.getVatToPay());
        }
        if (input.getVatToRefund() != null) {
            vatReturn.setVatToRefund(input.getVatToRefund());
        }
        if (input.getEffectiveVatToPay() != null) {
            vatReturn.setEffectiveVatToPay(input.getEffectiveVatToPay());
        }
        if (input.getVatForDeduction() != null) {
            vatReturn.setVatForDeduction(input.getVatForDeduction());
        }
        if (input.getVatRefundArt92() != null) {
            vatReturn.setVatRefundArt92(input.getVatRefundArt92());
        }
        if (input.getNotes() != null) {
            vatReturn.setNotes(input.getNotes());
        }

        return vatReturnRepository.save(vatReturn);
    }

    @Override
    public boolean deleteReturn(Integer id) {
        Optional<VatReturnEntity> vatReturn = vatReturnRepository.findById(id);
        if (vatReturn.isEmpty()) {
            return false;
        }

        if (vatReturn.get().getStatus() == VatReturnStatus.SUBMITTED ||
            vatReturn.get().getStatus() == VatReturnStatus.ACCEPTED) {
            throw new IllegalStateException("Не може да се изтрие подадена ДДС декларация");
        }

        vatReturnRepository.deleteById(id);
        return true;
    }

    // ========== VAT Export methods ==========
    @Override
    @Transactional
    public String exportDeklar(Integer returnId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + returnId));

        if (vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("ДДС декларацията трябва да е в статус 'Изчислена'");
        }
        
        CompanyEntity company = vatReturn.getCompany();
        String period = String.format("%d%02d", vatReturn.getPeriodYear(), vatReturn.getPeriodMonth());
        
        StringBuilder sb = new StringBuilder();

        // 00-01: Идентификационен номер по ДДС на лицето (15 symbolic)
        sb.append(VatExportFormatter.formatText(company.getVatNumber(), 15));
        // 00-02: Наименование на лицето (50 symbolic)
        sb.append(VatExportFormatter.formatText(company.getName(), 50));
        // 00-03: Данъчен период (6 symbolic YYYYMM)
        sb.append(VatExportFormatter.formatText(period, 6));
        // 00-04: Лице, подаващо данните (50 symbolic) - Placeholder, assuming the company manager for now
        sb.append(VatExportFormatter.formatText(company.getManagerName() != null ? company.getManagerName() : company.getName(), 50));
        // 00-05: Брой документи в дневника за продажби (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(new BigDecimal(vatReturn.getSalesDocumentCount()), 15));
        // 00-06: Брой документи в дневника за покупки (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(new BigDecimal(vatReturn.getPurchaseDocumentCount()), 15));

        // Sales fields (*01-XX)
        // *01-01: Общ размер на данъчните основи за облагане с ДДС (15 numeric)
        // This is a sum of multiple fields. Need to derive it.
        BigDecimal totalSalesBase = vatReturn.getSalesBase20()
                                    .add(vatReturn.getSalesBase9())
                                    .add(vatReturn.getSalesBase0Art3())
                                    .add(vatReturn.getSalesBase0Vod())
                                    .add(vatReturn.getSalesBase0Export())
                                    .add(vatReturn.getSalesBaseArt21())
                                    .add(vatReturn.getSalesBaseArt69())
                                    .add(vatReturn.getSalesBaseExempt())
                                    .add(vatReturn.getSalesBaseVop()); // Assuming VOP is also part of total base for sales
        sb.append(VatExportFormatter.formatAmountWithDecimal(totalSalesBase, 15));
        // *01-20: Всичко начислен ДДС (15 numeric)
        BigDecimal totalAccruedVat = vatReturn.getSalesVat20()
                                        .add(vatReturn.getSalesVat9())
                                        .add(vatReturn.getSalesVatVop())
                                        .add(vatReturn.getSalesVatPersonalUse());
        sb.append(VatExportFormatter.formatAmountWithDecimal(totalAccruedVat, 15));
        // *01-11: Данъчна основа на облагаемите доставки със ставка 20 % (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBase20(), 15));
        // *01-21: Начислен ДДС 20 % (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesVat20(), 15));
        // *01-12: Данъчна основа на ВОП и данъчна основа на получени доставки по чл. 82, ал. 2 - 6 от ЗДДС (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBaseVop(), 15));
        // *01-22: Начислен данък за ВОП и за получени доставки по чл. 82, ал. 2 - 6 от ЗДДС (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesVatVop(), 15));
        // *01-23: Начислен данък за доставки на стоки и услуги за лични нужди (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesVatPersonalUse(), 15));
        // *01-13: Данъчна основа на облагаемите доставки със ставка 9 % (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBase9(), 15));
        // *01-24: Начислен ДДС 9 % (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesVat9(), 15));
        // *01-14: Данъчна основа, подлежаща на облагане със ставка 0 % по глава трета от ЗДДС (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBase0Art3(), 15));
        // *01-15: Данъчна основа на доставките със ставка 0 % за ВОД на стоки (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBase0Vod(), 15));
        // *01-16: Данъчна основа на доставки, подлежаща на облагане с 0 % по чл. 140, 146 и чл. 173 ЗДДС (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBase0Export(), 15)); // Assuming salesBase0Export maps to this. Check definition.
        // *01-17: Данъчна основа на доставки на услуги по чл. 21, ал. 2 ЗДДС с място на изпълнение на територията на друга държава членка (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBaseArt21(), 15));
        // *01-18: Данъчна основа на доставки по чл. 69, ал. 2 ЗДДС, вкл. дистанционни продажби с място на изпълнение на територията на друга държава членка, както и на доставки като посредник в тристранна операция (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBaseArt69(), 15));
        // *01-19: Данъчна основа на освободени доставки и освободените ВОП (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getSalesBaseExempt(), 15));

        // Purchase fields (*01-XX)
        // *01-30: Данъчна основа и данък на получените доставки, ВОП, получените доставки по чл. 82, ал. 2 - 6 от ЗДДС и вносът без право на данъчен кредит или без данък (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseBaseNoCredit(), 15));
        // *01-31: Данъчна основа на получените доставки, ВОП, получените доставки по чл. 82, ал. 2 - 6 от ЗДДС, вносът, както и данъчната основа на получените доставки, използвани за извършване на доставки по чл. 69, ал. 2 ЗДДС с право на пълен данъчен кредит (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseBaseFullCredit(), 15));
        // *01-41: Начислен ДДС с право на пълен данъчен кредит (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseVatFullCredit(), 15));
        // *01-32: Данъчна основа на получените доставки, ВОП, получените доставки по чл. 82, ал. 2 - 6 от ЗДДС, вносът, както и данъчната основа на получените доставки, използвани за извършване на доставки по чл. 69, ал. 2 ЗДДС с право на частичен данъчен кредит (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseBasePartialCredit(), 15));
        // *01-42: Начислен ДДС с право на частичен данъчен кредит (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseVatPartialCredit(), 15));
        // *01-43: Годишна корекция по чл. 73, ал. 8 (+/-) ЗДДС (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getPurchaseVatAnnualAdjustment(), 15));

        // Result fields (01-XX)
        // 01-33: Коефициент по чл. 73, ал. 5 ЗДДС (4 numeric) - format 1.000 (3 digits after decimal point)
        sb.append(String.format("%04.3f", vatReturn.getCreditCoefficient()));
        // 01-40: Общо данъчен кредит (15 numeric)
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getTotalDeductibleVat(), 15));
        // 01-50: ДДС за внасяне (15 numeric) - кл. 50
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getVatToPay(), 15));
        // 01-60: ДДС за възстановяване (15 numeric) - кл. 60
        sb.append(VatExportFormatter.formatAmountWithDecimal(vatReturn.getVatToRefund(), 15));
        // 01-70: ДДС за внасяне (ръчно) - кл. 70
        sb.append(VatExportFormatter.formatAmountWithDecimal(
            vatReturn.getVatToPay() != null ? vatReturn.getVatToPay() : BigDecimal.ZERO, 15));
        // 01-71: ДДС за възстановяване (ръчно) - кл. 71
        sb.append(VatExportFormatter.formatAmountWithDecimal(
            vatReturn.getVatToRefund() != null ? vatReturn.getVatToRefund() : BigDecimal.ZERO, 15));
        // 01-80: Ефективно внесен ДДС - кл. 80
        sb.append(VatExportFormatter.formatAmountWithDecimal(
            vatReturn.getEffectiveVatToPay() != null ? vatReturn.getEffectiveVatToPay() : BigDecimal.ZERO, 15));
        // 01-81: ДДС за приспадане - кл. 81
        sb.append(VatExportFormatter.formatAmountWithDecimal(
            vatReturn.getVatForDeduction() != null ? vatReturn.getVatForDeduction() : BigDecimal.ZERO, 15));
        // 01-82: ДДС за възстановяване по чл. 92 - кл. 82
        sb.append(VatExportFormatter.formatAmountWithDecimal(
            vatReturn.getVatRefundArt92() != null ? vatReturn.getVatRefundArt92() : BigDecimal.ZERO, 15));

        return VatExportFormatter.toBase64Windows1251(sb.toString());
    }

    @Override
    @Transactional
    public String exportPokupki(Integer returnId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + returnId));

        if (vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("ДДС декларацията трябва да е в статус 'Изчислена'");
        }

        List<JournalEntryEntity> entries = journalEntryRepository.findByCompanyIdAndVatDateBetween(
            vatReturn.getCompany().getId(), vatReturn.getPeriodFrom(), vatReturn.getPeriodTo()
        ).stream().filter(e -> e.getVatPurchaseOperation() != null).toList();

        StringBuilder result = new StringBuilder();
        int lineNum = 1;
        for (JournalEntryEntity entry : entries) {
            // Get counterpart from journal entry first, then from entry lines
            CounterpartEntity counterpart = entry.getCounterpart();
            if (counterpart == null) {
                counterpart = entry.getEntryLines().stream()
                    .map(EntryLineEntity::getCounterpart).filter(c -> c != null).findFirst().orElse(null);
            }

            if(counterpart == null) continue;

            // Use journal entry totals directly (not sum of lines)
            BigDecimal totalVat = entry.getTotalVatAmount() != null ? entry.getTotalVatAmount() : BigDecimal.ZERO;
            BigDecimal totalBase = entry.getTotalAmount() != null ?
                entry.getTotalAmount().subtract(totalVat) : BigDecimal.ZERO;

            String period = String.format("%d%02d", vatReturn.getPeriodYear(), vatReturn.getPeriodMonth());
            
            result.append(VatExportFormatter.formatText(vatReturn.getCompany().getVatNumber(), 13));
            result.append(VatExportFormatter.formatText(period, 6));
            result.append("0"); // вид на дневника
            result.append(VatExportFormatter.formatText(String.valueOf(lineNum++), 15));
            result.append(VatExportFormatter.formatText(entry.getVatDocumentType(), 2));
            result.append(VatExportFormatter.formatText(entry.getDocumentNumber(), 20));
            result.append(VatExportFormatter.formatDate(entry.getDocumentDate()));
            result.append(VatExportFormatter.formatText(counterpart.getVatNumber(), 14));
            result.append(VatExportFormatter.formatText(counterpart.getName(), 30));
            result.append(VatExportFormatter.formatText(entry.getDescription(), 30));
            
            // This part is complex and depends on the operation type.
            // Simplified logic:
            if("пок30".equals(entry.getVatPurchaseOperation()) || "пок32".equals(entry.getVatPurchaseOperation())) {
                 result.append(VatExportFormatter.formatAmountWithDecimal(totalBase, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(totalVat, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            } else {
                 result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(totalBase, 15));
                 result.append(VatExportFormatter.formatAmountWithDecimal(totalVat, 15));
            }

            result.append(System.lineSeparator());
        }

        return VatExportFormatter.toBase64Windows1251(result.toString());
    }

    @Override
    @Transactional
    public String exportProdajbi(Integer returnId) {
        VatReturnEntity vatReturn = vatReturnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("ДДС декларацията не е намерена: " + returnId));

        if (vatReturn.getStatus() != VatReturnStatus.CALCULATED) {
            throw new IllegalStateException("ДДС декларацията трябва да е в статус 'Изчислена'");
        }

        List<JournalEntryEntity> entries = journalEntryRepository.findByCompanyIdAndVatDateBetween(
            vatReturn.getCompany().getId(), vatReturn.getPeriodFrom(), vatReturn.getPeriodTo()
        ).stream().filter(e -> e.getVatSalesOperation() != null).toList();
        
        StringBuilder result = new StringBuilder();
        int lineNum = 1;
        for (JournalEntryEntity entry : entries) {
            // Get counterpart from journal entry first, then from entry lines
            CounterpartEntity counterpart = entry.getCounterpart();
            if (counterpart == null) {
                counterpart = entry.getEntryLines().stream()
                    .map(EntryLineEntity::getCounterpart).filter(c -> c != null).findFirst().orElse(null);
            }

            if(counterpart == null) continue;

            // Use journal entry totals directly (not sum of lines)
            BigDecimal totalVat = entry.getTotalVatAmount() != null ? entry.getTotalVatAmount() : BigDecimal.ZERO;
            BigDecimal totalBase = entry.getTotalAmount() != null ?
                entry.getTotalAmount().subtract(totalVat) : BigDecimal.ZERO;

            String period = String.format("%d%02d", vatReturn.getPeriodYear(), vatReturn.getPeriodMonth());

            result.append(VatExportFormatter.formatText(vatReturn.getCompany().getVatNumber(), 13));
            result.append(VatExportFormatter.formatText(period, 6));
            result.append("0"); // вид на дневника
            result.append(VatExportFormatter.formatText(String.valueOf(lineNum++), 15));
            result.append(VatExportFormatter.formatText(entry.getVatDocumentType(), 2));
            result.append(VatExportFormatter.formatText(entry.getDocumentNumber(), 10));
            result.append(VatExportFormatter.formatDate(entry.getDocumentDate()));
            result.append(VatExportFormatter.formatText(counterpart.getVatNumber(), 14));
            result.append(VatExportFormatter.formatText(counterpart.getName(), 20));
            result.append(VatExportFormatter.formatText(entry.getDescription(), 20));
            
            // Simplified logic based on operation type
            result.append(VatExportFormatter.formatAmountWithDecimal(totalBase, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(totalVat, 15));
            // ... add other fields based on operation, simplified for now
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));
            result.append(VatExportFormatter.formatAmountWithDecimal(BigDecimal.ZERO, 15));

            result.append(System.lineSeparator());
        }

        return VatExportFormatter.toBase64Windows1251(result.toString());
    }
}
