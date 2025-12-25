package bg.spacbg.sp_ac_bg.model.dto.input;

import bg.spacbg.sp_ac_bg.model.enums.CounterpartType;
import lombok.Data;

@Data
public class UpdateCounterpartInput {
    private String name;
    private String eik;
    private String vatNumber;
    private String address;
    private String longAddress;
    private String city;
    private String country;
    private CounterpartType counterpartType;
    private Boolean isVatRegistered;
    private Boolean isActive;
}
