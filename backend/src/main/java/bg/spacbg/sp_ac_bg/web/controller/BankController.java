package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateBankProfileInput;
import bg.spacbg.sp_ac_bg.model.entity.BankImportEntity;
import bg.spacbg.sp_ac_bg.model.entity.BankProfileEntity;
import bg.spacbg.sp_ac_bg.service.BankService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // ========== Bank Profile Queries ==========

    @QueryMapping
    public List<BankProfileEntity> bankProfiles(@Argument Integer companyId) {
        return bankService.findProfilesByCompanyId(companyId);
    }

    @QueryMapping
    public BankProfileEntity bankProfile(@Argument Integer id) {
        return bankService.findProfileById(id).orElse(null);
    }

    @QueryMapping
    public BankProfileEntity bankProfileByIban(@Argument String iban) {
        return bankService.findProfileByIban(iban).orElse(null);
    }

    // ========== Bank Profile Mutations ==========

    @MutationMapping
    public BankProfileEntity createBankProfile(@Argument CreateBankProfileInput input) {
        Integer userId = getCurrentUserId();
        return bankService.createProfile(input, userId);
    }

    @MutationMapping
    public BankProfileEntity updateBankProfile(@Argument Integer id, @Argument UpdateBankProfileInput input) {
        return bankService.updateProfile(id, input);
    }

    @MutationMapping
    public Boolean deleteBankProfile(@Argument Integer id) {
        return bankService.deleteProfile(id);
    }

    // ========== Bank Import Queries ==========

    @QueryMapping
    public List<BankImportEntity> bankImports(@Argument Integer companyId) {
        return bankService.findImportsByCompanyId(companyId);
    }

    @QueryMapping
    public BankImportEntity bankImport(@Argument Integer id) {
        return bankService.findImportById(id).orElse(null);
    }

    @QueryMapping
    public List<BankImportEntity> bankImportsByProfile(@Argument Integer bankProfileId) {
        return bankService.findImportsByProfileId(bankProfileId);
    }

    // ========== Bank Import Mutations ==========

    @MutationMapping
    public BankImportEntity processBankImport(@Argument Integer bankProfileId, @Argument String fileKey) {
        Integer userId = getCurrentUserId();
        return bankService.processFileImport(bankProfileId, fileKey, userId);
    }

    @MutationMapping
    public Boolean deleteBankImport(@Argument Integer id) {
        return bankService.deleteImport(id);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Extract user ID from authentication
        return 1; // Placeholder
    }
}
