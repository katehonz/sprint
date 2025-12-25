package bg.spacbg.sp_ac_bg.model.dto;

import lombok.Data;

@Data
public class UserDto {
    private Integer id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserGroupDto group;
    private boolean isActive;
}
