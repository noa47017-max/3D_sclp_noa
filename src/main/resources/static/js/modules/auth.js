import { LS_TOKEN, LS_USER_ID, LS_USERNAME, API_BASE_URL } from '../config/constants.js';
import { isValidEmail, isValidPassword, showError, hideError, sanitize } from '../utils/utils.js';

// ─── משתנים גלובליים לאימות ───
export let jwtToken = null;
export let currentUserId = null;

// ─── שמירת סשן ב-localStorage ───
export function saveSession(token, userId, username) {
    localStorage.setItem(LS_TOKEN, token);
    localStorage.setItem(LS_USER_ID, userId);
    localStorage.setItem(LS_USERNAME, username);
    jwtToken = token;
    currentUserId = userId;
}

// ─── מחיקת סשן ───
export function clearSession() {
    localStorage.removeItem(LS_TOKEN);
    localStorage.removeItem(LS_USER_ID);
    localStorage.removeItem(LS_USERNAME);
    jwtToken = null;
    currentUserId = null;
}

// ─── אתחול סשן מ-localStorage בטעינת הדף ───
export function initSession() {
    const token = localStorage.getItem(LS_TOKEN);
    const userId = localStorage.getItem(LS_USER_ID);
    const username = localStorage.getItem(LS_USERNAME);
    
    if (token && userId) {
        jwtToken = token;
        currentUserId = userId;
        const el = document.getElementById('user-display');
        if (el) el.innerText = username || '';
    }
    
    return !!token;
}

// ─── התנתקות ───
export function logout() {
    clearSession();
}

// ─── בקשה עם אימות (JWT) ───
export async function authenticatedFetch(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${jwtToken}`,
            ...(options.headers || {})
        }
    });

    // עדכון טוקן אם השרת שלח חדש
    const newToken = response.headers.get('New-Token');
    if (newToken) {
        jwtToken = newToken;
        localStorage.setItem(LS_TOKEN, newToken);
    }

    return response;
}

// ─── התחברות ───
export async function handleLogin(email, password) {
    if (!isValidEmail(email)) {
        showError('login-error', 'אימייל לא תקין');
        return false;
    }
    if (!password) {
        showError('login-error', 'הזן סיסמה');
        return false;
    }

    try {
        hideError('login-error');
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email: sanitize(email),
                password
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            showError('login-error', errorData.message || 'שגיאה בהתחברות');
            return false;
        }

        const data = await response.json();
        saveSession(data.token, data.userId, data.username);
        return true;
    } catch (err) {
        showError('login-error', 'שגיאת רשת: ' + err.message);
        return false;
    }
}

// ─── הרשמה ───
export async function handleRegister(username, email, password, password2) {
    if (!username) {
        showError('register-error', 'הזן שם משתמש');
        return false;
    }
    if (!isValidEmail(email)) {
        showError('register-error', 'אימייל לא תקין');
        return false;
    }
    if (!isValidPassword(password)) {
        showError('register-error', 'סיסמה חייבת: 6+ תווים, אות גדולה, אות קטנה, ספרה');
        return false;
    }
    if (password !== password2) {
        showError('register-error', 'הסיסמאות לא תואמות');
        return false;
    }

    try {
        hideError('register-error');
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: sanitize(username),
                email: sanitize(email),
                password
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            showError('register-error', errorData.message || 'שגיאה בהרשמה');
            return false;
        }

        const data = await response.json();
        saveSession(data.token, data.userId, data.username);
        return true;
    } catch (err) {
        showError('register-error', 'שגיאת רשת: ' + err.message);
        return false;
    }
}

// ─── שאלת סיסמה ───
export async function handleForgotPassword(email) {
    if (!isValidEmail(email)) {
        alert('אימייל לא תקין');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/auth/forgot-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: sanitize(email) })
        });

        if (!response.ok) {
            alert('שגיאה בבקשה לאיפוס סיסמה');
            return;
        }

        alert('בדוק את הדוא"ל שלך להנחיות איפוס הסיסמה');
    } catch (err) {
        alert('שגיאת רשת: ' + err.message);
    }
}
