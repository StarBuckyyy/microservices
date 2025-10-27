package com.brokerx.integration;

import com.brokerx.entity.User;
import com.brokerx.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class UserRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        User user = new User();
        user.setEmail("user@integration.com");
        user.setPhone("1234567890");
        user.setHashedPassword("securepassword");
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setDateOfBirth(java.time.LocalDate.of(1990, 5, 20));
        user.setAddress("123 Rue Principale, Montréal, Canada");

        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findById(savedUser.getUserId());

        assertTrue(foundUser.isPresent());
        assertEquals("user@integration.com", foundUser.get().getEmail());
        assertEquals("Jean", foundUser.get().getFirstName());
        assertEquals("Dupont", foundUser.get().getLastName());
    }

    @Test
    void testFindByEmail() {
        User user = new User();
        user.setEmail("findbyemail@test.com");
        user.setPhone("0987654321");
        user.setHashedPassword("pass");
        user.setFirstName("Email");
        user.setLastName("Search");
        user.setDateOfBirth(LocalDate.of(1985, 3, 15));
        user.setAddress("456 Another St, San Francisco, Etats-Unis");
        userRepository.save(user);


        Optional<User> foundUser = userRepository.findByEmail("findbyemail@test.com");
        
        assertTrue(foundUser.isPresent());
        assertEquals("findbyemail@test.com", foundUser.get().getEmail());
    }

    @Test
    void testFindByEmail_NotFound() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@test.com");
        assertFalse(foundUser.isPresent());
    }
}