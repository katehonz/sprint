package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.AuthResponse;
import bg.spacbg.sp_ac_bg.model.dto.auth.LoginRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.RegisterRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.RecoverPasswordRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.ResetPasswordRequest;
import bg.spacbg.sp_ac_bg.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/recover-password")
    public ResponseEntity<?> recoverPassword(@Valid @RequestBody RecoverPasswordRequest recoverPasswordRequest) {
        authService.recoverPassword(recoverPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok().build();
    }
}
