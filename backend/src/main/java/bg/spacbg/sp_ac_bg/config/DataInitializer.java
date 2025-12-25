package bg.spacbg.sp_ac_bg.config;

import bg.spacbg.sp_ac_bg.model.dto.ChartOfAccountsDto;
import bg.spacbg.sp_ac_bg.model.dto.input.CreateCompanyInput;
import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import bg.spacbg.sp_ac_bg.model.enums.RateProvider;
import bg.spacbg.sp_ac_bg.repository.CompanyRepository;
import bg.spacbg.sp_ac_bg.service.AccountService;
import bg.spacbg.sp_ac_bg.service.CompanyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String TEST_COMPANY_EIK = "999999999";
    private static final String TEST_COMPANY_NAME = "Тестова Фирма ЕООД";

    @Bean
    CommandLineRunner initTestData(
            CompanyRepository companyRepository,
            CompanyService companyService,
            AccountService accountService,
            ObjectMapper objectMapper) {

        return args -> {
            // Check if test company already exists
            Optional<CompanyEntity> existing = companyRepository.findByEik(TEST_COMPANY_EIK);

            if (existing.isPresent()) {
                log.info("Тестовата фирма вече съществува: {} (ID: {})", TEST_COMPANY_NAME, existing.get().getId());
                return;
            }

            log.info("Създаване на тестова фирма...");

            // Create test company
            CreateCompanyInput input = new CreateCompanyInput();
            input.setName(TEST_COMPANY_NAME);
            input.setEik(TEST_COMPANY_EIK);
            input.setVatNumber("BG" + TEST_COMPANY_EIK);
            input.setAddress("ул. Тестова 1");
            input.setCity("София");
            input.setCountry("BG");
            input.setPhone("+359888123456");
            input.setEmail("test@testfirma.bg");
            input.setContactPerson("Иван Тестов");
            input.setManagerName("Иван Тестов");
            input.setAuthorizedPerson("Иван Тестов");
            input.setEnableViesValidation(false);
            input.setEnableAiMapping(false);
            input.setAutoValidateOnImport(false);
            input.setPreferredRateProvider(RateProvider.ECB);

            CompanyEntity testCompany = companyService.create(input);
            log.info("Създадена тестова фирма: {} (ID: {})", testCompany.getName(), testCompany.getId());

            // Try to load chart of accounts
            List<ChartOfAccountsDto> chartOfAccounts = loadChartOfAccounts(objectMapper);

            if (chartOfAccounts != null && !chartOfAccounts.isEmpty()) {
                int importedCount = accountService.importChartOfAccounts(testCompany.getId(), chartOfAccounts, false);
                log.info("Импортирани {} сметки в тестовата фирма", importedCount);
            } else {
                log.warn("Не е намерен сметкоплан за импорт. Тестовата фирма е създадена без сметкоплан.");
            }
        };
    }

    private List<ChartOfAccountsDto> loadChartOfAccounts(ObjectMapper objectMapper) {
        // Try multiple locations for chart of accounts file

        // 1. Try external file in chart/ folder (relative to working directory)
        Path chartDir = Paths.get("chart");
        if (Files.exists(chartDir) && Files.isDirectory(chartDir)) {
            try {
                Optional<Path> chartFile = Files.list(chartDir)
                        .filter(p -> p.toString().endsWith(".json"))
                        .findFirst();

                if (chartFile.isPresent()) {
                    log.info("Зареждане на сметкоплан от: {}", chartFile.get());
                    return objectMapper.readValue(
                            chartFile.get().toFile(),
                            new TypeReference<List<ChartOfAccountsDto>>() {}
                    );
                }
            } catch (Exception e) {
                log.warn("Грешка при зареждане на сметкоплан от chart/: {}", e.getMessage());
            }
        }

        // 2. Try classpath resource
        try {
            Resource resource = new ClassPathResource("data/chart-of-accounts.json");
            if (resource.exists()) {
                log.info("Зареждане на сметкоплан от classpath: data/chart-of-accounts.json");
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readValue(is, new TypeReference<List<ChartOfAccountsDto>>() {});
                }
            }
        } catch (Exception e) {
            log.warn("Грешка при зареждане на сметкоплан от classpath: {}", e.getMessage());
        }

        // 3. Try default Bulgarian chart of accounts from classpath
        try {
            Resource resource = new ClassPathResource("data/bg-national-chart-of-accounts.json");
            if (resource.exists()) {
                log.info("Зареждане на Национален сметкоплан от classpath");
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readValue(is, new TypeReference<List<ChartOfAccountsDto>>() {});
                }
            }
        } catch (Exception e) {
            log.warn("Грешка при зареждане на Национален сметкоплан: {}", e.getMessage());
        }

        return null;
    }
}
