package com.edu.agent.service;

import com.edu.agent.model.UserProfile;
import com.edu.agent.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserProfileService {
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    public UserProfile getUserProfile(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setUserName("User " + userId);
            profile.setPreferences(new HashMap<>());
            profile = userProfileRepository.save(profile);
        }
        return profile;
    }
    
    public UserProfile updateUserPreference(String userId, String key, String value) {
        UserProfile profile = getUserProfile(userId);
        profile.getPreferences().put(key, value);
        return userProfileRepository.save(profile);
    }
    
    public Map<String, String> getUserPreferences(String userId) {
        UserProfile profile = getUserProfile(userId);
        return profile.getPreferences();
    }
    
    public String getUserPreference(String userId, String key) {
        UserProfile profile = getUserProfile(userId);
        return profile.getPreferences().get(key);
    }
}