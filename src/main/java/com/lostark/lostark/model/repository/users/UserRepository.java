package com.lostark.lostark.model.repository.users;

import com.lostark.lostark.model.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);
}
