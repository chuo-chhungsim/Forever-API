package com.forever.foreverapi.client.mail;

import com.forever.foreverapi.configuration.AppProperties;
import com.forever.foreverapi.exception.ApiException;
import com.forever.foreverapi.modules.auth.model.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.Duration;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.from = props.mail().from();
    }

    public void sendOtp(String to, OtpPurpose purpose, String code, Duration ttl) {
        String subject = purpose == OtpPurpose.VERIFY_EMAIL
                ? "Verify your Forever account"
                : "Reset your Forever password";
        String intro = purpose == OtpPurpose.VERIFY_EMAIL
                ? "Use this code to verify your email address:"
                : "Use this code to reset your password:";
        long minutes = ttl.toMinutes();
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#222">
                  <h2 style="letter-spacing:2px">FOREVER<span style="color:#e60023">.</span></h2>
                  <p>%s</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0">%s</p>
                  <p style="color:#666">This code expires in %d minutes. If you did not request it, you can ignore this email.</p>
                </div>""".formatted(intro, code, minutes);
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(intro + "\n\n" + code + "\n\nThis code expires in " + minutes + " minutes.", html);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
            throw ApiException.unavailable("EMAIL_UNAVAILABLE", "Could not send the verification email, try again later");
        }
    }
}
