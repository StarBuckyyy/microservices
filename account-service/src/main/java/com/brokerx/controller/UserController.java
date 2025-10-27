package com.brokerx.controller;

import com.brokerx.entity.User;
import com.brokerx.entity.Account;
import com.brokerx.repository.UserRepository;
import com.brokerx.service.AccountService;
import com.brokerx.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService, AccountService accountService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User userRequest) {
        User savedUser = userRepository.save(userRequest);
        // Crée automatiquement un compte PENDING pour cet utilisateur
        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("PENDING");
        accountService.saveAccount(account);

        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}