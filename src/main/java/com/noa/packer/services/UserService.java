package com.noa.packer.services;

import com.noa.packer.models.User;
import com.noa.packer.repositories.UserRepository;
import com.noa.packer.util.SanitizeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private EmailService emailService;

    // ➕ הרשמה
    public String registerUser(String username, String email, String password) {
        username = SanitizeUtil.clean(username);
        email    = SanitizeUtil.clean(email);
        if (userRepository.getUserByEmail(email) != null)
            return "❌ האימייל כבר קיים במערכת!";
        if (userRepository.getUserByUsername(username) != null)
            return "❌ שם המשתמש כבר תפוס!";

        // הצפנת הסיסמא
        String hashedPassword = encoder.encode(password);
        User newUser = new User(username, email, hashedPassword);
        userRepository.addUser(newUser);
        return "✅ נרשמת בהצלחה!";
    }

    // 🔐 התחברות
    public String loginUser(String username, String password) {
        User user = userRepository.getUserByUsername(username);
        if (user == null||!encoder.matches(password, user.getPassword()))
            return "שם המשתמש או סיסמא שגויים!";

        if (!"active".equals(user.getSubscriptionStatus()))
            return " המנוי שלך אינו פעיל!";

        return " התחברת בהצלחה!";
    }
    public User getUserAfterLogin(String email, String password) {
           User user = userRepository.getUserByEmail(email);
            if (user == null) return null;
            if (!encoder.matches(password, user.getPassword())) return null;
        return user;
    }
    // בקשת שחזור סיסמא
    public String requestPasswordReset(String email) {
        User user = userRepository.getUserByEmail(email);
        if (user == null)
            return "האימייל לא נמצא במערכת!";

        // יצירת Token אקראי בתוקף ל-30 דקות
        String token  = UUID.randomUUID().toString();
        Date   expiry = new Date(System.currentTimeMillis() + 30 * 60 * 1000);

        userRepository.saveResetToken(email, token, expiry);
        emailService.sendPasswordResetEmail(email, token);
        return "✅ נשלח אימייל לשחזור סיסמא!";
    }

    // 🔒 איפוס סיסמא
    public String resetPassword(String token, String newPassword) {
        User user = userRepository.getUserByResetToken(token);
        if (user == null)
            return "הקישור לא תקין!";

        // בדיקה שה-Token לא פג תוקף
        if (user.getResetTokenExpiry().before(new Date()))
            return "הקישור פג תוקף! בקש קישור חדש.";

        String hashedPassword = encoder.encode(newPassword);
        userRepository.updatePassword(user.getEmail(), hashedPassword);
        return "הסיסמא עודכנה בהצלחה!";
    }
}