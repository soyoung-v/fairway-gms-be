package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.common.config.FairwayAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final FairwayAppProperties appProperties;

    public void sendPasswordResetEmail(String to, String rawToken) {
        String resetUrl = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Fairway GMS] 비밀번호 재설정");
        message.setText(
                "안녕하세요.\n\n" +
                "비밀번호를 재설정하려면 아래 링크를 클릭하세요.\n\n" +
                resetUrl + "\n\n" +
                "링크는 30분 후 만료됩니다.\n" +
                "본인이 요청하지 않은 경우 이 메일을 무시하세요."
        );
        mailSender.send(message);
    }
}
