package booking.controller;

import booking.dtos.AuthResponse;
import booking.dtos.LoginRequest;
import booking.dtos.MessageResponse;
import booking.security.jwt.JwtUtils;
import booking.security.jwt.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication);

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .username(authentication.getName())
                .role(role)
                .expiresIn(jwtUtils.getExpirationMs())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            tokenBlacklistService.blacklistToken(token);
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
