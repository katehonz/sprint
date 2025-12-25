package bg.spacbg.sp_ac_bg.model.dto.input;

import lombok.Data;

@Data
public class UpdateFixedAssetInput {
    private String name;
    private String description;
    private String documentNumber;
    private java.time.LocalDate documentDate;
    private String location;
    private String responsiblePerson;
    private String notes;
    private String status;
}
