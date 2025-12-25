package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.GenerateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatRateInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateVatReturnInput;
import bg.spacbg.sp_ac_bg.model.entity.VatRateEntity;
import bg.spacbg.sp_ac_bg.model.entity.VatReturnEntity;

import java.util.List;
import java.util.Optional;

public interface VatService {
    // VAT Rate operations
    List<VatRateEntity> findRatesByCompanyId(Integer companyId);
    List<VatRateEntity> findActiveRatesByCompanyId(Integer companyId);
    Optional<VatRateEntity> findRateById(Integer id);
    Optional<VatRateEntity> findRateByCode(String code);
    VatRateEntity createRate(CreateVatRateInput input);
    VatRateEntity updateRate(Integer id, UpdateVatRateInput input);
    boolean deleteRate(Integer id);

    // VAT Return operations
    List<VatReturnEntity> findReturnsByCompanyId(Integer companyId);
    Optional<VatReturnEntity> findReturnById(Integer id);
    Optional<VatReturnEntity> findReturnByPeriod(Integer companyId, Integer year, Integer month);
    VatReturnEntity generateReturn(GenerateVatReturnInput input, Integer userId);
    VatReturnEntity submitReturn(Integer id, Integer userId);
    VatReturnEntity updateReturn(Integer id, UpdateVatReturnInput input, Integer userId);
    boolean deleteReturn(Integer id);

    // VAT Export operations
    String exportDeklar(Integer returnId);
    String exportPokupki(Integer returnId);
    String exportProdajbi(Integer returnId);
}
