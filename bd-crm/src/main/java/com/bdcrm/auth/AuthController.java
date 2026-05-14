package com.bdcrm.auth;

import com.bdcrm.audit.AuditEventService;
import com.bdcrm.common.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final DatabaseUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditEventService auditEventService;

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            userDetailsService.assertLoginAllowed(request.username());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            auditEventService.log(null, "LOGIN_SUCCESS", "USER", user.id(), "User logged in", "{\"username\":\"" + request.username() + "\"}");
            return AuthSessionResponse.from(user.withToken(jwtService.generateToken(user)));
        } catch (AuthenticationException exception) {
            auditEventService.log(null, "LOGIN_FAILURE", "USER", null, "Login failed", "{\"username\":\"" + request.username() + "\"}");
            throw toApiException(exception);
        }
    }

    @GetMapping("/me")
    public AuthResponse currentUser(Authentication authentication) {
        return AuthResponse.from((AuthenticatedUser) authentication.getPrincipal());
    }

    private ApiException toApiException(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (exception instanceof LockedException) {
            return new ApiException(HttpStatus.FORBIDDEN, exception.getMessage());
        }
        if (exception instanceof AuthenticationServiceException) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "Unable to authenticate user");
        }
        return new ApiException(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }
}
