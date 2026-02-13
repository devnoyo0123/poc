package com.example.service;

import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getUser(Long id) {
        return userRepository.findById(id);
    }

    public String createUser(String user) {
        return "Created: " + user;
    }
}
