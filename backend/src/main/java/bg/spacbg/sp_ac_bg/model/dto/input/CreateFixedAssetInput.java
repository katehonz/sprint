package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateFixedAssetInput {
    private String inventoryNumber;
    private String name;
    private String description;
    private Integer categoryId;
    private Integer companyId;
    private BigDecimal acquisitionCost;
    private LocalDate acquisitionDate;
    private String documentNumber;
    private LocalDate documentDate;
    private LocalDate putIntoServiceDate;
    private String location;
    private String responsiblePerson;
    private String serialNumber;
    private String notes;
}
