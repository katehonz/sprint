package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.GenerateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.entity.VatRateEntity;
import bg.spacbg.sp_ac_bg.model.entity.VatReturnEntity;
import bg.spacbg.sp_ac_bg.service.VatService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class VatController {

    private final VatService vatService;

    public VatController(VatService vatService) {
        this.vatService = vatService;
    }

    // ========== VAT Rate Queries ==========

    @QueryMapping
    public List<VatRateEntity> vatRates(@Argument Integer companyId) {
        return vatService.findRatesByCompanyId(companyId);
    }

    @QueryMapping
    public VatRateEntity vatRate(@Argument Integer id) {
        return vatService.findRateById(id).orElse(null);
    }

    @QueryMapping
    public VatRateEntity vatRateByCode(@Argument String code) {
        return vatService.findRateByCode(code).orElse(null);
    }

    @QueryMapping
    public List<VatRateEntity> activeVatRates(@Argument Integer companyId) {
        return vatService.findActiveRatesByCompanyId(companyId);
    }

    // ========== VAT Rate Mutations ==========

    @MutationMapping
    public VatRateEntity createVatRate(@Argument CreateVatRateInput input) {
        return vatService.createRate(input);
    }

    @MutationMapping
    public VatRateEntity updateVatRate(@Argument Integer id, @Argument UpdateVatRateInput input) {
        return vatService.updateRate(id, input);
    }

    @MutationMapping
    public Boolean deleteVatRate(@Argument Integer id) {
        return vatService.deleteRate(id);
    }

    // ========== VAT Return Queries ==========

    @QueryMapping
    public List<VatReturnEntity> vatReturns(@Argument Integer companyId) {
        return vatService.findReturnsByCompanyId(companyId);
    }

    @QueryMapping
    public VatReturnEntity vatReturn(@Argument Integer id) {
        return vatService.findReturnById(id).orElse(null);
    }

    @QueryMapping
    public VatReturnEntity vatReturnByPeriod(@Argument Integer companyId, @Argument Integer year, @Argument Integer month) {
        return vatService.findReturnByPeriod(companyId, year, month).orElse(null);
    }

    // ========== VAT Return Mutations ==========

    @MutationMapping
    public VatReturnEntity generateVatReturn(@Argument GenerateVatReturnInput input) {
        Integer userId = getCurrentUserId();
        return vatService.generateReturn(input, userId);
    }

    @MutationMapping
    public VatReturnEntity submitVatReturn(@Argument Integer id) {
        Integer userId = getCurrentUserId();
        return vatService.submitReturn(id, userId);
    }

    @MutationMapping
    public VatReturnEntity updateVatReturn(@Argument Integer id, @Argument UpdateVatReturnInput input) {
        Integer userId = getCurrentUserId();
        return vatService.updateReturn(id, input, userId);
    }

    @MutationMapping
    public Boolean deleteVatReturn(@Argument Integer id) {
        return vatService.deleteReturn(id);
    }
    
    @MutationMapping
    public VatReturnEntity calculateVatReturn(@Argument GenerateVatReturnInput input) {
        Integer userId = getCurrentUserId();
        return vatService.generateReturn(input, userId);
    }

    @MutationMapping
    public String exportDeklar(@Argument Integer id) {
        return vatService.exportDeklar(id);
    }

    @MutationMapping
    public String exportPokupki(@Argument Integer id) {
        return vatService.exportPokupki(id);
    }

    @MutationMapping
    public String exportProdajbi(@Argument Integer id) {
        return vatService.exportProdajbi(id);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from authentication
        return 1; // Placeholder
    }
}

