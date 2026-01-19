package com.cr0w.smartplanner.repository;

import com.cr0w.smartplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.expression.spel.ast.OpAnd;

import java.util.Optional;

public interface UserRepository  extends JpaRepository<User, Long> {
    Optional<User> findByTelegramChatId(Long telegramChatId);
    //Optional<User> findByUsername(String username);
}
