package com.interviewprep.repository;

import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.ExperienceLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_Success() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setTargetRole("Dev");
        user.setExperienceLevel(ExperienceLevel.JUNIOR);

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
    }

    @Test
    void findByEmail_Found() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("found@example.com");
        user.setPassword("password");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("found@example.com");

        assertTrue(found.isPresent());
        assertEquals("found@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_NotFound() {
        Optional<User> found = userRepository.findByEmail("notfound@example.com");
        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmail_True() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("exists@example.com");
        user.setPassword("password");
        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void existsByEmail_False() {
        assertFalse(userRepository.existsByEmail("notexists@example.com"));
    }
}
