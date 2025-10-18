package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입


    @Override
    public void signup(SignupUser user) {
        // 비밀번호 암호화 -> user 에도 적용
        String encodedPassword = passwordEncoder.encode(user.getUserPassword());
        User newUser = user.toEntity(encodedPassword);
        userRepository.save(newUser);
    }

    @Override
    public boolean checkUserIdExists(String userId) {
        return userRepository.existsByUserId(userId);
    }
}
