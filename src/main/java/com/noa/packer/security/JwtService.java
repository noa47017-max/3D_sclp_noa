package com.noa.packer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_STRING = "ThisIsMyVerySecretKeyForJwtAuthenticationWhichIsVeryLongAndSecure";
    private final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    private final long TOKEN_EXPIRATION = 1000 * 60 * 60 * 1; // שעה
    private final long MAX_SESSION_TIME = 1000 * 60 * 60 * 5; // 5 שעות

    // 1. יצירת טוקן חדש לחלוטין (Login)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionStartTime", System.currentTimeMillis()); // שומרים מתי הכל התחיל
        return createToken(claims, userDetails.getUsername(), TOKEN_EXPIRATION);
    }

    // 2. רענון טוקן (Sliding Expiration)
    public String refreshToken(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();

            // בדיקה האם עברו 5 שעות מההתחלה המקורית
            long sessionStartTime = claims.get("sessionStartTime", Long.class);
            if (System.currentTimeMillis() - sessionStartTime > MAX_SESSION_TIME) {
                return null; // המשתמש חייב לעשות Login מחדש
            }

            // מייצרים טוקן חדש עם אותם Claims
            return createToken(claims, username, TOKEN_EXPIRATION);
        } catch (Exception e) {
            return null;
        }
    }

    // פונקציית העזר המרכזית (משתמשת תמיד ב-SECRET_KEY האחיד)
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // ... (שאר המתודות נשארות כמעט אותו דבר)

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}