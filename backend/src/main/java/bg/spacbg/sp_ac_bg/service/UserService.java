package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.UserDto;
import bg.spacbg.sp_ac_bg.model.dto.UserGroupDto;
import bg.spacbg.sp_ac_bg.model.dto.CurrentUserDto;
import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();
    UserDto getUserById(Integer id);
    List<UserGroupDto> getAllGroups();
    CurrentUserDto getCurrentUser(String username);
    UserDto createUser(String username, String email, String password, String firstName, String lastName, Integer groupId, Boolean isActive);
    UserDto updateUser(Integer id, String username, String email, String firstName, String lastName, Integer groupId, Boolean isActive);
    boolean deleteUser(Integer id);
    CurrentUserDto updateProfile(String username, String email, String firstName, String lastName);
    boolean changePassword(String username, String currentPassword, String newPassword);
    boolean resetUserPassword(Integer id, String newPassword);
}
