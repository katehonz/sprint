package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.auth.LoginRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.RecoverPasswordRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.RegisterRequest;
import bg.spacbg.sp_ac_bg.model.dto.auth.ResetPasswordRequest;
import bg.spacbg.sp_ac_bg.model.entity.UserEntity;
import bg.spacbg.sp_ac_bg.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    public String login(LoginRequest loginRequest) {
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            // Set authentication context before logging
            SecurityContextHolder.getContext().setAuthentication(authentication);

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            // Log successful login
            auditLogService.logLogin(ipAddress, userAgent, true, null);

            return token;
        } catch (AuthenticationException e) {
            // Log failed login attempt
            auditLogService.logLogin(ipAddress, userAgent, false, "Грешен потребител или парола: " + loginRequest.getUsername());
            throw e;
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    public void register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(""); // Required field
        user.setLastName("");  // Required field
        user.setActive(true);

        // Set default periods
        LocalDate now = LocalDate.now();
        LocalDate yearEnd = now.plusYears(1);
        user.setDocumentPeriodStart(now);
        user.setDocumentPeriodEnd(yearEnd);
        user.setDocumentPeriodActive(true);
        user.setAccountingPeriodStart(now);
        user.setAccountingPeriodEnd(yearEnd);
        user.setAccountingPeriodActive(true);
        user.setVatPeriodStart(now);
        user.setVatPeriodEnd(yearEnd);
        user.setVatPeriodActive(true);

        userRepository.save(user);
    }

    public void recoverPassword(RecoverPasswordRequest recoverPasswordRequest) {
        UserEntity user = userRepository.findByEmail(recoverPasswordRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setRecoveryCodeHash(passwordEncoder.encode(token));
        user.setRecoveryCodeCreatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

        // TODO: Send email with token
        System.out.println("Password reset token for " + user.getEmail() + ": " + token);
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        // Find user by checking recovery code (simplified - in production use proper token storage)
        // For now, we'll need the email too
        throw new UnsupportedOperationException("Reset password requires email in request - TODO");
    }
}
