package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetCategoryInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateFixedAssetInput;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetCategoryEntity;
import bg.spacbg.sp_ac_bg.model.entity.FixedAssetEntity;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.repository.FixedAssetCategoryRepository;
import bg.spacbg.sp_ac_bg.repository.FixedAssetRepository;
import bg.spacbg.sp_ac_bg.service.FixedAssetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FixedAssetServiceImpl implements FixedAssetService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISPOSED = "DISPOSED";
    private static final String DEPRECIATION_METHOD_LINEAR = "LINEAR";

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetCategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;

    public FixedAssetServiceImpl(
            FixedAssetRepository fixedAssetRepository,
            FixedAssetCategoryRepository categoryRepository,
            CompanyRepository companyRepository) {
        this.fixedAssetRepository = fixedAssetRepository;
        this.categoryRepository = categoryRepository;
        this.companyRepository = companyRepository;
    }

    // ========== Fixed Asset Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<FixedAssetEntity> findAssetsByCompanyId(Integer companyId) {
        return fixedAssetRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FixedAssetEntity> findAssetsByCategoryId(Integer categoryId) {
        return fixedAssetRepository.findByCategoryId(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FixedAssetEntity> findAssetById(Integer id) {
        return fixedAssetRepository.findById(id);
    }

    @Override
    public FixedAssetEntity createAsset(CreateFixedAssetInput input) {
        if (fixedAssetRepository.existsByInventoryNumber(input.getInventoryNumber())) {
            throw new IllegalArgumentException("Дълготраен актив с инвентарен номер " +
                    input.getInventoryNumber() + " вече съществува");
        }

        FixedAssetCategoryEntity category = categoryRepository.findById(input.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена: " + input.getCategoryId()));

        CompanyEntity company = companyRepository.findById(input.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Компанията не е намерена: " + input.getCompanyId()));

        FixedAssetEntity asset = new FixedAssetEntity();
        asset.setInventoryNumber(input.getInventoryNumber());
        asset.setName(input.getName());
        asset.setDescription(input.getDescription());
        asset.setCategory(category);
        asset.setCompany(company);
        asset.setAcquisitionCost(input.getAcquisitionCost());
        asset.setAcquisitionDate(input.getAcquisitionDate());
        asset.setDocumentNumber(input.getDocumentNumber());
        asset.setDocumentDate(input.getDocumentDate());
        asset.setPutIntoServiceDate(input.getPutIntoServiceDate());
        asset.setLocation(input.getLocation());
        asset.setResponsiblePerson(input.getResponsiblePerson());
        asset.setSerialNumber(input.getSerialNumber());
        asset.setNotes(input.getNotes());
        asset.setStatus(STATUS_ACTIVE);

        // Set depreciation parameters from category
        int usefulLife = category.getMinUsefulLife() != null ? category.getMinUsefulLife() : 5;
        asset.setAccountingUsefulLife(usefulLife);
        asset.setAccountingDepreciationRate(category.getDefaultAccountingDepreciationRate() != null
                ? category.getDefaultAccountingDepreciationRate()
                : BigDecimal.valueOf(100).divide(BigDecimal.valueOf(usefulLife), 4, RoundingMode.HALF_UP));
        asset.setAccountingDepreciationMethod(DEPRECIATION_METHOD_LINEAR);
        asset.setAccountingSalvageValue(BigDecimal.ZERO);
        asset.setAccountingAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setAccountingBookValue(input.getAcquisitionCost());

        // Tax depreciation
        asset.setTaxDepreciationRate(category.getMaxTaxDepreciationRate());
        asset.setTaxAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setTaxBookValue(input.getAcquisitionCost());
        asset.setNewFirstTimeInvestment(false);

        return fixedAssetRepository.save(asset);
    }

    @Override
    public FixedAssetEntity updateAsset(Integer id, UpdateFixedAssetInput input) {
        FixedAssetEntity asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Дълготрайният актив не е намерен: " + id));

        if (input.getName() != null) asset.setName(input.getName());
        if (input.getDescription() != null) asset.setDescription(input.getDescription());
        if (input.getDocumentNumber() != null) asset.setDocumentNumber(input.getDocumentNumber());
        if (input.getDocumentDate() != null) asset.setDocumentDate(input.getDocumentDate());
        if (input.getLocation() != null) asset.setLocation(input.getLocation());
        if (input.getResponsiblePerson() != null) asset.setResponsiblePerson(input.getResponsiblePerson());
        if (input.getNotes() != null) asset.setNotes(input.getNotes());
        if (input.getStatus() != null) asset.setStatus(input.getStatus());

        return fixedAssetRepository.save(asset);
    }

    @Override
    public boolean deleteAsset(Integer id) {
        if (!fixedAssetRepository.existsById(id)) {
            throw new IllegalArgumentException("Дълготрайният актив не е намерен: " + id);
        }
        fixedAssetRepository.deleteById(id);
        return true;
    }

    @Override
    public FixedAssetEntity disposeAsset(Integer id, LocalDate disposalDate, BigDecimal disposalAmount) {
        FixedAssetEntity asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Дълготрайният актив не е намерен: " + id));

        if (STATUS_DISPOSED.equals(asset.getStatus())) {
            throw new IllegalStateException("Активът вече е бракуван");
        }

        asset.setStatus(STATUS_DISPOSED);
        asset.setDisposalDate(disposalDate);
        asset.setDisposalAmount(disposalAmount);

        return fixedAssetRepository.save(asset);
    }

    @Override
    public List<FixedAssetEntity> calculateDepreciation(Integer companyId, LocalDate toDate) {
        List<FixedAssetEntity> assets = fixedAssetRepository.findActiveAssetsForDepreciation(companyId, toDate);
        List<FixedAssetEntity> updatedAssets = new ArrayList<>();

        for (FixedAssetEntity asset : assets) {
            if (asset.getPutIntoServiceDate() == null) {
                continue;
            }

            // Calculate accounting depreciation (linear method)
            BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(
                    asset.getAcquisitionCost(),
                    asset.getAccountingSalvageValue(),
                    asset.getAccountingUsefulLife());

            long monthsInService = ChronoUnit.MONTHS.between(asset.getPutIntoServiceDate(), toDate);
            BigDecimal totalAccountingDepreciation = monthlyDepreciation
                    .multiply(BigDecimal.valueOf(monthsInService))
                    .min(asset.getAcquisitionCost().subtract(asset.getAccountingSalvageValue()));

            asset.setAccountingAccumulatedDepreciation(totalAccountingDepreciation);
            asset.setAccountingBookValue(asset.getAcquisitionCost().subtract(totalAccountingDepreciation));

            // Calculate tax depreciation
            BigDecimal taxDepreciationForYear = asset.getAcquisitionCost()
                    .multiply(asset.getTaxDepreciationRate())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            int yearsInService = (int) ChronoUnit.YEARS.between(asset.getPutIntoServiceDate(), toDate);
            BigDecimal totalTaxDepreciation = taxDepreciationForYear
                    .multiply(BigDecimal.valueOf(yearsInService))
                    .min(asset.getAcquisitionCost());

            asset.setTaxAccumulatedDepreciation(totalTaxDepreciation);
            asset.setTaxBookValue(asset.getAcquisitionCost().subtract(totalTaxDepreciation));

            updatedAssets.add(fixedAssetRepository.save(asset));
        }

        return updatedAssets;
    }

    private BigDecimal calculateMonthlyDepreciation(BigDecimal cost, BigDecimal salvageValue, int usefulLifeYears) {
        BigDecimal depreciableAmount = cost.subtract(salvageValue);
        int totalMonths = usefulLifeYears * 12;
        return depreciableAmount.divide(BigDecimal.valueOf(totalMonths), 4, RoundingMode.HALF_UP);
    }

    // ========== Fixed Asset Category Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<FixedAssetCategoryEntity> findAllCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FixedAssetCategoryEntity> findCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    @Override
    public FixedAssetCategoryEntity createCategory(CreateFixedAssetCategoryInput input) {
        if (categoryRepository.existsByCode(input.getCode())) {
            throw new IllegalArgumentException("Категория с код " + input.getCode() + " вече съществува");
        }

        FixedAssetCategoryEntity category = new FixedAssetCategoryEntity();
        category.setCode(input.getCode());
        category.setName(input.getName());
        category.setDescription(input.getDescription());
        category.setTaxCategory(input.getTaxCategory());
        category.setMaxTaxDepreciationRate(input.getMaxTaxDepreciationRate());
        category.setDefaultAccountingDepreciationRate(input.getDefaultAccountingDepreciationRate());
        category.setMinUsefulLife(input.getMinUsefulLife());
        category.setMaxUsefulLife(input.getMaxUsefulLife());
        category.setAssetAccountCode(input.getAssetAccountCode());
        category.setDepreciationAccountCode(input.getDepreciationAccountCode());
        category.setExpenseAccountCode(input.getExpenseAccountCode());
        category.setActive(true);

        return categoryRepository.save(category);
    }

    @Override
    public FixedAssetCategoryEntity updateCategory(Integer id, UpdateFixedAssetCategoryInput input) {
        FixedAssetCategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена: " + id));

        if (input.getName() != null) category.setName(input.getName());
        if (input.getDescription() != null) category.setDescription(input.getDescription());
        if (input.getMaxTaxDepreciationRate() != null) category.setMaxTaxDepreciationRate(input.getMaxTaxDepreciationRate());
        if (input.getDefaultAccountingDepreciationRate() != null) category.setDefaultAccountingDepreciationRate(input.getDefaultAccountingDepreciationRate());
        if (input.getMinUsefulLife() != null) category.setMinUsefulLife(input.getMinUsefulLife());
        if (input.getMaxUsefulLife() != null) category.setMaxUsefulLife(input.getMaxUsefulLife());
        if (input.getAssetAccountCode() != null) category.setAssetAccountCode(input.getAssetAccountCode());
        if (input.getDepreciationAccountCode() != null) category.setDepreciationAccountCode(input.getDepreciationAccountCode());
        if (input.getExpenseAccountCode() != null) category.setExpenseAccountCode(input.getExpenseAccountCode());
        if (input.getIsActive() != null) category.setActive(input.getIsActive());

        return categoryRepository.save(category);
    }
}
