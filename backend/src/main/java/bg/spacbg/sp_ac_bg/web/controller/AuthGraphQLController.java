package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.AuthResponse;
import bg.spacbg.sp_ac_bg.model.dto.LoginInput;
import bg.spacbg.sp_ac_bg.service.JwtService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

@Controller
public class AuthGraphQLController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthGraphQLController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @MutationMapping
    public AuthResponse login(@Argument LoginInput loginInput) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginInput.getUsername(), loginInput.getPassword())
        );

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token);
    }
}
