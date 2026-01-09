package com.cr0w.smartplanner.service;

import com.cr0w.smartplanner.exception.UserNotCreatedException;
import com.cr0w.smartplanner.model.User;
import com.cr0w.smartplanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    public final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserOrCreateNew(Long tgId) {
        logger.info("Getting user or creating new user with tgId: {}", tgId);

        if (tgId == null) {
            logger.error("Telegram ID is null");
            throw new IllegalArgumentException("Telegram ID cannot be null");
        }

        try {
            return userRepository.findByTelegramChatId(tgId)
                        .orElseGet(() -> {
                            User newUser = new User();
                        newUser.setTelegramChatId(tgId);
                        User saved = userRepository.save(newUser);

                        if (saved.getId() == null) {
                            logger.error("Failed to save new user for tgId: {}", tgId);
                            throw new UserNotCreatedException("Failed to create new user");
                        }
                        return saved;
                    });
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when saving user with tgId {}: {}", tgId, e.getMessage(), e);
            throw new UserNotCreatedException("Cannot create user due to DB constraints", e);
        } catch (Exception e) {
            logger.error("Unexpected error while creating user with tgId {}: {}", tgId, e.getMessage(), e);
            throw new UserNotCreatedException("Unexpected error while creating user", e);
        }
    }
}
