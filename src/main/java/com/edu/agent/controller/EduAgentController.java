package com.edu.agent.controller;

import com.edu.agent.service.EduAgentService;
import com.edu.agent.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/edu-agent")
public class EduAgentController {
    @Autowired
    private EduAgentService eduAgentService;
    
    @Autowired
    private UserProfileService userProfileService;
    
    @PostMapping("/process")
    public Map<String, String> processRequest(@RequestParam String userId, @RequestParam String request) {
        String response = eduAgentService.processRequest(userId, request);
        return Map.of("response", response);
    }
    
    @GetMapping("/user-profile/{userId}")
    public Map<String, Object> getUserProfile(@PathVariable String userId) {
        return Map.of(
                "userProfile", userProfileService.getUserProfile(userId),
                "preferences", userProfileService.getUserPreferences(userId)
        );
    }
    
    @PostMapping("/user-preference")
    public Map<String, Object> updateUserPreference(@RequestParam String userId, @RequestParam String key, @RequestParam String value) {
        return Map.of(
                "userProfile", userProfileService.updateUserPreference(userId, key, value)
        );
    }
}