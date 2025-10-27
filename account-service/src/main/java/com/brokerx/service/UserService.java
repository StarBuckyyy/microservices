package com.brokerx.service;

import com.brokerx.entity.User;
import com.brokerx.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getUser(UUID id) { return userRepository.findById(id).orElse(null); }
    public User findByEmail(String email) {
    return userRepository.findByEmail(email).orElse(null);
}

}
