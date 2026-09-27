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
import com.interviewprep.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse registerUser(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setTargetRole(request.getTargetRole());
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
            try {
                user.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                user.setExperienceLevel(ExperienceLevel.FRESHER);
            }
        }
        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isBlank()) {
            user.setProfilePhoto(request.getProfilePhoto());
        } else {
            user.setProfilePhoto("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2NCIgaGVpZ2h0PSI2NCIgZmlsbD0ibm9uZSI+PHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMzIiIGZpbGw9IiM2MzY2ZjEiLz48Y2lyY2xlIGN4PSIzMiIgY3k9IjI0IiByPSIxMiIgZmlsbD0id2hpdGUiLz48cGF0aCBkPSJNMjAgNTJjMC02LjYyNyA1LjM3My0xMiAxMi0xMnMxMiA1LjM3MyAxMiAxMnYyaC0yNHYtMnoiIGZpbGw9IndoaXRlIi8+PC9zdmc+");
        }
        user.setCreatedAt(LocalDateTime.now());

        Role defaultRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roleRepository.save(r);
        });
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        log.info("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getTargetRole() != null && !request.getTargetRole().isBlank()) {
            user.setTargetRole(request.getTargetRole());
        }
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
            try {
                user.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.getReminderEnabled() != null) {
            user.setReminderEnabled(request.getReminderEnabled());
        }
        if (request.getReminderTime() != null) {
            user.setReminderTime(request.getReminderTime());
        }
        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isBlank()) {
            user.setProfilePhoto(request.getProfilePhoto());
        }
        
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(CustomUserDetails userDetails) {
        return getUserById(userDetails.getId());
    }
}
