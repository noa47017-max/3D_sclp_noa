import { jwtToken } from './auth.js';
import { PROTECTED_SCREENS, SAFE_SCREENS } from '../config/constants.js';

// ─── קובץ מטמון של כל המסכים ───
const screens = {
    login:     document.getElementById('login-screen'),
    menu:      document.getElementById('menu-screen'),
    creator:   document.getElementById('creator-screen'),
    result:    document.getElementById('result-screen'),
    register:  document.getElementById('register-screen'),
    benchmark: document.getElementById('benchmark-screen'),
    history:   document.getElementById('history-screen')
};

// ─── משתנה זמני לשמירת נתוני החישוב האחרון ───
export let lastCalculatedData = [];

// ─── הצגת מסך ───
export function showScreen(screenName, addToHistory = true) {
    // Guard: מסכים מוגנים ללא אימות
    if (PROTECTED_SCREENS.includes(screenName) && !jwtToken) {
        showScreen('login', addToHistory);
        return;
    }
    
    // Guard: result ללא נתונים
    if (screenName === 'result' && lastCalculatedData.length === 0) {
        showScreen('creator', addToHistory);
        return;
    }
    
    // Guard: login/register כשכבר מחוברים
    if ((screenName === 'login' || screenName === 'register') && jwtToken) {
        showScreen('menu', addToHistory);
        return;
    }
    
    // הסתרת כל המסכים
    Object.values(screens).forEach(s => {
        if (s) s.style.display = 'none';
    });
    
    // הצגת המסך הנבחר
    if (screens[screenName]) {
        screens[screenName].style.display = 'flex';
    }
    
    // הוספה להיסטוריית הדפדפן
    if (addToHistory) {
        history.pushState({ screen: screenName }, '', '#' + screenName);
    }
}

// ─── ניהול כפתור חזור בדפדפן ───
export function initBrowserHistory() {
    window.addEventListener('popstate', (event) => {
        const screenName = event.state?.screen;
        
        if (!screenName) {
            showScreen(jwtToken ? 'menu' : 'login', false);
            return;
        }
        
        // בדיקת הרשאות
        if (PROTECTED_SCREENS.includes(screenName) && !jwtToken) {
            showScreen('login', false);
            return;
        }
        
        if ((screenName === 'login' || screenName === 'register') && jwtToken) {
            showScreen('menu', false);
            return;
        }
        
        if (screenName === 'result' && lastCalculatedData.length === 0) {
            showScreen('creator', false);
            return;
        }
        
        showScreen(screenName, false);
    });
}

// ─── אתחול היסטוריה בעת טעינת הדף ───
export function initHistoryFromURL(isLoggedIn) {
    const hash = window.location.hash.slice(1);
    const safeScreens = SAFE_SCREENS;
    const target = safeScreens.includes(hash) ? hash : (isLoggedIn ? 'menu' : 'login');
    
    showScreen(target, false);
    
    const currentScreen = window.location.hash.slice(1) || (isLoggedIn ? 'menu' : 'login');
    history.replaceState({ screen: currentScreen }, '');
}

// ─── קישור כפתורי ניווט ל-DOM ───
export function attachNavigationListeners() {
    document.getElementById('goToRegisterBtn')?.addEventListener('click', () => showScreen('register'));
    document.getElementById('backToLoginBtn')?.addEventListener('click', () => showScreen('login'));
    document.getElementById('createNewBtn')?.addEventListener('click', () => showScreen('creator'));
    document.getElementById('benchmarkBtn')?.addEventListener('click', () => showScreen('benchmark'));
    document.getElementById('benchmarkBackBtn')?.addEventListener('click', () => showScreen('menu'));
    document.getElementById('creatorBackBtn')?.addEventListener('click', () => showScreen('menu'));
    document.getElementById('viewHistoryBtn')?.addEventListener('click', () => showScreen('history'));
    document.getElementById('historyBackBtn')?.addEventListener('click', () => showScreen('menu'));
}
