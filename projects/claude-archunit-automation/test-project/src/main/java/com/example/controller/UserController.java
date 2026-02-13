package com.example.controller;

import com.example.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    public String createUser(@RequestBody String user) {
        return userService.createUser(user);
    }
}
