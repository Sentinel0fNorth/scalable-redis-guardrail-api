package com.assignment.controller;

import com.assignment.entity.User;
import com.assignment.entity.Bot;
import com.assignment.repository.UserRepository;
import com.assignment.repository.BotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    public UserController(UserRepository userRepository, BotRepository botRepository) {
        this.userRepository = userRepository;
        this.botRepository = botRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setIsPremium(request.getIsPremium() != null ? request.getIsPremium() : false);
            User saved = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/bots")
    public ResponseEntity<Map<String, Object>> createBot(@RequestBody BotRequest request) {
        try {
            Bot bot = new Bot();
            bot.setName(request.getName());
            bot.setPersonaDescription(request.getPersonaDescription());
            Bot saved = botRepository.save(bot);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/bots/{botId}")
    public ResponseEntity<Map<String, Object>> getBot(@PathVariable Long botId) {
        try {
            Bot bot = botRepository.findById(botId)
                    .orElseThrow(() -> new RuntimeException("Bot not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    static class UserRequest {
        private String username;
        private Boolean isPremium;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Boolean getIsPremium() {
            return isPremium;
        }

        public void setIsPremium(Boolean isPremium) {
            this.isPremium = isPremium;
        }
    }

    static class BotRequest {
        private String name;
        private String personaDescription;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPersonaDescription() {
            return personaDescription;
        }

        public void setPersonaDescription(String personaDescription) {
            this.personaDescription = personaDescription;
        }
    }
}
