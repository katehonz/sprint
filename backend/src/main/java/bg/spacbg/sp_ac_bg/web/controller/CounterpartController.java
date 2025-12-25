package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.ViesValidationResult;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.dto.input.UpdateCounterpartInput;
import bg.spacbg.sp_ac_bg.model.entity.CounterpartEntity;
import bg.spacbg.sp_ac_bg.service.CounterpartService;
import bg.spacbg.sp_ac_bg.service.ViesService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class CounterpartController {

    private final CounterpartService counterpartService;
    private final ViesService viesService;

    public CounterpartController(CounterpartService counterpartService, ViesService viesService) {
        this.counterpartService = counterpartService;
        this.viesService = viesService;
    }

    @QueryMapping
    public List<CounterpartEntity> counterparts(@Argument Integer companyId) {
        return counterpartService.findByCompanyId(companyId);
    }

    @QueryMapping
    public CounterpartEntity counterpart(@Argument Integer id) {
        return counterpartService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<CounterpartEntity> searchCounterparts(@Argument Integer companyId, @Argument String query) {
        return counterpartService.search(companyId, query);
    }

    @MutationMapping
    public CounterpartEntity createCounterpart(@Argument CreateCounterpartInput input) {
        return counterpartService.create(input);
    }

    @MutationMapping
    public CounterpartEntity updateCounterpart(@Argument Integer id, @Argument UpdateCounterpartInput input) {
        return counterpartService.update(id, input);
    }

    @MutationMapping
    public Boolean deleteCounterpart(@Argument Integer id) {
        return counterpartService.delete(id);
    }

    @MutationMapping
    public ViesValidationResult validateVat(@Argument String vatNumber) {
        return viesService.validateVat(vatNumber);
    }
}
