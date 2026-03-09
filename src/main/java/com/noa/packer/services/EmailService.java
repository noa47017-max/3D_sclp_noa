package com.noa.packer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("איפוס סיסמא – 3D Container Optimizer");
        message.setText(
            "שלום,\n\n" +
            "קיבלנו בקשה לאיפוס הסיסמא שלך.\n" +
            "לחץ על הקישור הבא לאיפוס (בתוקף ל-30 דקות):\n\n" +
            resetLink + "\n\n" +
            "אם לא ביקשת איפוס סיסמא, התעלם מהודעה זו.\n\n" +
            "בברכה,\nצוות 3D Container Optimizer"
        );
        mailSender.send(message);
        System.out.println("📧 Reset email sent to: " + toEmail);
    }
}