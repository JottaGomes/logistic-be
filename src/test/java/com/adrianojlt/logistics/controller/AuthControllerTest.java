package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ApiResponse;
import com.adrianojlt.logistics.dto.LoginRequestDTO;
import com.adrianojlt.logistics.dto.LoginResponseDTO;
import com.adrianojlt.logistics.dto.RegisterRequestDTO;
import com.adrianojlt.logistics.entity.User;
import com.adrianojlt.logistics.repository.UserRepository;
import com.adrianojlt.logistics.security.AppSecurityProperties;
import com.adrianojlt.logistics.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AppSecurityProperties securityProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController controller;

    @Test
    void config_returnsLoginEnabledTrue() {
        when(securityProperties.isEnableLogin()).thenReturn(true);

        ResponseEntity<ApiResponse<Map<String, Boolean>>> response = controller.config();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().get("loginEnabled")).isTrue();
    }

    @Test
    void config_returnsLoginEnabledFalse() {
        when(securityProperties.isEnableLogin()).thenReturn(false);

        ResponseEntity<ApiResponse<Map<String, Boolean>>> response = controller.config();

        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().data().get("loginEnabled")).isFalse();
    }

    @Test
    void login_withValidCredentials_returns200WithTokenAndUsername() {
        when(jwtUtil.generateToken("adriano")).thenReturn("test.jwt.token");

        ResponseEntity<ApiResponse<LoginResponseDTO>> response =
                controller.login(new LoginRequestDTO("adriano", "tmp!pass"), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().getToken()).isEqualTo("test.jwt.token");
        assertThat(response.getBody().data().getUsername()).isEqualTo("adriano");
    }

    @Test
    void login_withInvalidCredentials_returns401() {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        ResponseEntity<ApiResponse<LoginResponseDTO>> response =
                controller.login(new LoginRequestDTO("adriano", "wrongpassword"), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void register_createsUserAndReturnsToken() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtUtil.generateToken("newuser")).thenReturn("a-token");

        ResponseEntity<ApiResponse<LoginResponseDTO>> response =
                controller.register(new RegisterRequestDTO("newuser", "secret123"), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().getToken()).isEqualTo("a-token");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("newuser");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("joao")).thenReturn(true);

        ResponseEntity<ApiResponse<LoginResponseDTO>> response =
                controller.register(new RegisterRequestDTO("joao", "secret123"), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isFalse();
        verify(userRepository, never()).save(any());
    }
}
