package com.alexis.paymybuddy.Service;

import com.alexis.paymybuddy.DTO.ProfileUpdateDTO;
import com.alexis.paymybuddy.Model.User;
import com.alexis.paymybuddy.Repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User("Kevin", "kevin@example.com", "$2a$encodedPassword");
        user.setId(1L);
        user.setActive(true);
        user.setBalance(BigDecimal.ZERO);
    }

    @Test
    void authenticate_success() {

        when(userRepository.findByEmail("kevin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(eq("password"), anyString()))
                .thenReturn(true);

        boolean result = userService.userAuthenticate("kevin@example.com", "password");

        assertTrue(result);
    }

    @Test
    void authenticate_invalid_password() {

        when(userRepository.findByEmail("kevin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(eq("wrong"), anyString()))
                .thenReturn(false);

        boolean result = userService.userAuthenticate("kevin@example.com", "wrong");

        assertFalse(result);
    }

    @Test
    void authenticate_invalid_email() {

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        boolean result = userService.userAuthenticate("unknown@example.com", "password");

        assertFalse(result);
    }

    @Test
    void update_profile_success() {

        ProfileUpdateDTO dto = new ProfileUpdateDTO(
                "Didier",
                "didier@example.com",
                "123"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode("123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateProfile(1L, dto);

        assertEquals("Didier", updated.getUsername());
        assertEquals("didier@example.com", updated.getEmail());

        verify(userRepository).save(updated);
    }

    @Test
    void add_connection_success() {

        User friend = new User("Alice", "alice@example.com", "pass");
        friend.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));

        userService.addConnection(1L, 2L);

        assertTrue(user.getConnections().contains(friend));
        verify(userRepository).save(user);
    }

    @Test
    void recharge_account_success() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.rechargeAccount(1L, new BigDecimal("50"));

        assertEquals(new BigDecimal("50"), user.getBalance());

        verify(userRepository).save(user);
    }

    @Test
    void recharge_account_invalid_amount() {

        // ❌ PAS DE MOCK ICI → inutile

        assertThrows(IllegalArgumentException.class,
                () -> userService.rechargeAccount(1L, BigDecimal.ZERO));
    }

    @Test
    void deactivate_user_success() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void activate_user_success() {

        user.setActive(false);

        // ⚠️ IMPORTANT : matcher la vraie méthode du service
        when(userRepository.findByIdAndActiveFalse(1L))
                .thenReturn(Optional.of(user));

        userService.activateUser(1L);

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }
}