package com.adrianojlt.logistics.security;

import com.adrianojlt.logistics.entity.User;
import com.adrianojlt.logistics.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_withExistingUser_returnsUserDetails() {
        User user = User.builder()
                .id(1L)
                .username("adriano")
                .password("$2a$10$hashedPassword")
                .build();

        when(userRepository.findByUsername("adriano")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("adriano");

        assertThat(result.getUsername()).isEqualTo("adriano");
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsername_withUnknownUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }
}
