package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.input.CreateCompanyInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCompanyInput;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @QueryMapping
    public List<CompanyEntity> companies() {
        return companyService.findAll();
    }

    @QueryMapping
    public CompanyEntity company(@Argument Integer id) {
        return companyService.findById(id).orElse(null);
    }

    @MutationMapping
    public CompanyEntity createCompany(@Argument CreateCompanyInput input) {
        return companyService.create(input);
    }

    @MutationMapping
    public CompanyEntity updateCompany(@Argument Integer id, @Argument UpdateCompanyInput input) {
        return companyService.update(id, input);
    }

    @MutationMapping
    public Boolean deleteCompany(@Argument Integer id) {
        return companyService.delete(id);
    }
}
