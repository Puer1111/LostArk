package com.lostark.lostark.model.repository.users;

import com.lostark.lostark.model.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);

    Optional<User> findByUserEmail(String userEmail);
}
