package com.lostark.lostark.service.email;

public interface EmailService {
    void sendVerificationEmail(String to, String code);
}
