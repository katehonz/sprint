package bg.spacbg.sp_ac_bg.model.dto;

import lombok.Data;

@Data
public class CurrentUserDto {
    private Integer id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserGroupDto group;
}
