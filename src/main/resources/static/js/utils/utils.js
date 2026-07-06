// ─── הגנת XSS ───
export function sanitize(str) {
    if (typeof str !== 'string') return str;
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ─── ולידציה של אימייל ───
export function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// ─── ולידציה של סיסמה ───
// לפחות 6 תווים, אות גדולה, אות קטנה, ספרה
export function isValidPassword(pw) {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/.test(pw);
}

// ─── הצגת הודעת שגיאה ───
export function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = message;
    el.style.display = 'block';
}

// ─── הסתרת הודעת שגיאה ───
export function hideError(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = 'none';
}
