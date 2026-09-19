package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ApiResponse;
import com.adrianojlt.logistics.dto.LoginRequestDTO;
import com.adrianojlt.logistics.dto.LoginResponseDTO;
import com.adrianojlt.logistics.dto.RegisterRequestDTO;
import com.adrianojlt.logistics.entity.User;
import com.adrianojlt.logistics.repository.UserRepository;
import com.adrianojlt.logistics.security.AppSecurityProperties;
import com.adrianojlt.logistics.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adrianojlt.logistics.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSecurityProperties securityProperties;
    private final AuthenticationManager authenticationManager;

    public AuthController(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppSecurityProperties securityProperties,
            AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> config() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("loginEnabled", securityProperties.isEnableLogin())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {

        String ip = WebUtil.resolveClientIp(httpRequest);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("login_failed username={} ip={}", request.getUsername(), ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid credentials"));
        }

        log.info("login_success username={} ip={}", request.getUsername(), ip);
        String token = jwtUtil.generateToken(request.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(new LoginResponseDTO(token, request.getUsername())));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletRequest httpRequest) {

        String ip = WebUtil.resolveClientIp(httpRequest);
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            log.warn("register_rejected username={} ip={} reason=taken", username, ip);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Username already taken"));
        }

        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .build());

        log.info("register_success username={} ip={}", username, ip);

        // sign the new user straight in, so they do not have to log in again
        String token = jwtUtil.generateToken(username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new LoginResponseDTO(token, username)));
    }

}
