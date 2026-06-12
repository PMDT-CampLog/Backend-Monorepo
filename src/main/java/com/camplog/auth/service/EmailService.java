package com.camplog.auth.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String userName);
}
