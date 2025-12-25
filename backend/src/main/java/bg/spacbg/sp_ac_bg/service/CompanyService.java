package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCompanyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCompanyInput;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;

import java.util.List;
import java.util.Optional;

public interface CompanyService {
    List<CompanyEntity> findAll();
    Optional<CompanyEntity> findById(Integer id);
    Optional<CompanyEntity> findByEik(String eik);
    CompanyEntity create(CreateCompanyInput input);
    CompanyEntity update(Integer id, UpdateCompanyInput input);
    boolean delete(Integer id);
}
