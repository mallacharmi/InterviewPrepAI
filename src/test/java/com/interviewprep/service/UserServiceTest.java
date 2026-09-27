package com.interviewprep.service;

import com.interviewprep.dto.request.ProfileUpdateRequest;
import com.interviewprep.dto.request.RegisterRequest;
import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.entity.Role;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.ExperienceLevel;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.exception.UserAlreadyExistsException;
import com.interviewprep.mapper.UserMapper;
import com.interviewprep.repository.RoleRepository;
import com.interviewprep.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role userRole;
    private RegisterRequest registerRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setTargetRole("Developer");
        user.setExperienceLevel(ExperienceLevel.JUNIOR);
        user.setRoles(new HashSet<>());
        user.getRoles().add(userRole);

        userResponse = UserResponse.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .targetRole("Developer")
                .experienceLevel("JUNIOR")
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setTargetRole("Developer");
        registerRequest.setExperienceLevel("JUNIOR");
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse response = userService.registerUser(registerRequest);

        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals(user.getEmail(), response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByEmail_Found() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse response = userService.getUserByEmail("test@example.com");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getUserByEmail_NotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByEmail("test@example.com");
        });
    }

    @Test
    void updateProfile_Success() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Updated Name");
        request.setTargetRole("Senior Developer");
        request.setExperienceLevel("SENIOR");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse response = userService.updateProfile(1L, request);

        assertNotNull(response);
        assertEquals("Updated Name", user.getName());
        assertEquals("Senior Developer", user.getTargetRole());
        assertEquals(ExperienceLevel.SENIOR, user.getExperienceLevel());
        verify(userRepository).save(user);
    }
}
