import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
 
// --- משתנים גלובליים ---
let scene, camera, renderer, controls;
let boxesToPack = [];
let lastCalculatedData = [];
let currentUserId = null;
let jwtToken = null;

// ─── הגנת XSS + ולידציה ───
function sanitize(str) {
    if (typeof str !== 'string') return str;
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
function isValidPassword(pw) {
    // לפחות 6 תווים, אות גדולה, אות קטנה, ספרה
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/.test(pw);
}
const screens = {
    login:     document.getElementById('login-screen'),
    menu:      document.getElementById('menu-screen'),
    creator:   document.getElementById('creator-screen'),
    result:    document.getElementById('result-screen'),
    register:  document.getElementById('register-screen'),
    benchmark: document.getElementById('benchmark-screen'),
    history:   document.getElementById('history-screen')
};
 
// ─────────────────────────────────────────────────────────────
//  showScreen — עם ניהול היסטוריית דפדפן
//  addToHistory=false מועבר כשהקריאה מגיעה מ-popstate או initSession
// ─────────────────────────────────────────────────────────────
export function showScreen(screenName, addToHistory = true) {
    // Guard: מסכים מוגנים ללא אימות
    const protectedScreens = ['menu', 'creator', 'result', 'benchmark', 'history'];
    if (protectedScreens.includes(screenName) && !jwtToken) {
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
 
    Object.values(screens).forEach(s => { if (s) s.style.display = 'none'; });
    if (screens[screenName]) screens[screenName].style.display = 'flex';
 
    if (addToHistory) {
        history.pushState({ screen: screenName }, '', '#' + screenName);
    }
}
window.showScreen = showScreen;
 
// ─────────────────────────────────────────────────────────────
//  ניהול סשן — localStorage
// ─────────────────────────────────────────────────────────────
const LS_TOKEN    = 'clp_token';
const LS_USER_ID  = 'clp_userId';
const LS_USERNAME = 'clp_username';
 
function saveSession(token, userId, username) {
    localStorage.setItem(LS_TOKEN,    token);
    localStorage.setItem(LS_USER_ID,  userId);
    localStorage.setItem(LS_USERNAME, username);
    jwtToken      = token;
    currentUserId = userId;
}
 
function clearSession() {
    localStorage.removeItem(LS_TOKEN);
    localStorage.removeItem(LS_USER_ID);
    localStorage.removeItem(LS_USERNAME);
    jwtToken           = null;
    currentUserId      = null;
    lastCalculatedData = [];
    boxesToPack        = [];
}
 
// משחזר סשן מ-localStorage בטעינת הדף
function initSession() {
    const token    = localStorage.getItem(LS_TOKEN);
    const userId   = localStorage.getItem(LS_USER_ID);
    const username = localStorage.getItem(LS_USERNAME);
 
    if (token && userId) {
        jwtToken      = token;
        currentUserId = userId;
        const el = document.getElementById('user-display');
        if (el) el.innerText = username || '';
 
        // שחזור מסך לפי hash בURL (אם קיים ובטוח)
        const hash = window.location.hash.slice(1);
        const safeScreens = ['menu', 'creator', 'benchmark', 'history'];
        const target = safeScreens.includes(hash) ? hash : 'menu';
        showScreen(target, false);
    } else {
        showScreen('login', false);
    }
 
    // state ראשוני — replaceState כדי לא להוסיף כניסה מיותרת לhistory
    const currentScreen = window.location.hash.slice(1) || (jwtToken ? 'menu' : 'login');
    history.replaceState({ screen: currentScreen }, '');
}
 
function logout() {
    clearSession();
    showScreen('login');
}
window.logout = logout;
 
// ─────────────────────────────────────────────────────────────
//  כפתור חזור/קדימה של הדפדפן
// ─────────────────────────────────────────────────────────────
window.addEventListener('popstate', (event) => {
    const screenName = event.state?.screen;
 
    if (!screenName) {
        showScreen(jwtToken ? 'menu' : 'login', false);
        return;
    }
 
    const protectedScreens = ['menu', 'creator', 'result', 'benchmark', 'history'];
 
    if (protectedScreens.includes(screenName) && !jwtToken) {
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
 
// ─────────────────────────────────────────────────────────────
//  מאזינים לכפתורי ניווט
// ─────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('goToRegisterBtn')?.addEventListener('click',  () => showScreen('register'));
    document.getElementById('backToLoginBtn')?.addEventListener('click',   () => showScreen('login'));
    document.getElementById('createNewBtn')?.addEventListener('click',     () => showScreen('creator'));
    document.getElementById('benchmarkBtn')?.addEventListener('click',     () => showScreen('benchmark'));
    document.getElementById('benchmarkBackBtn')?.addEventListener('click', () => showScreen('menu'));
    document.getElementById('creatorBackBtn')?.addEventListener('click',   () => showScreen('menu')); // תוקן: כפתור חזור ב-creator לא היה מחובר
 
    // כפתור התנתקות דינמי בתפריט
    const menuModal = document.querySelector('#menu-screen .modal');
    if (menuModal) {
        const logoutBtn = document.createElement('button');
        logoutBtn.textContent = 'התנתק 🚪';
        logoutBtn.style.cssText = 'background:#444; margin-top:8px; font-size:12px;';
        logoutBtn.onclick = logout;
        menuModal.appendChild(logoutBtn);
    }
});
 
// ─────────────────────────────────────────────────────────────
//  פונקציות עזר
// ─────────────────────────────────────────────────────────────
function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = message;
    el.style.display = 'block';
}
function hideError(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = 'none';
}
async function authenticatedFetch(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + jwtToken,
            ...(options.headers || {})
        }
    });

    const newToken = response.headers.get('New-Token');
    if (newToken) {
        jwtToken = newToken;
        localStorage.setItem(LS_TOKEN, newToken);
    }

    return response;
}
 

//  התחברות
const loginBtn = document.getElementById('loginBtn');
if (loginBtn) {
    loginBtn.onclick = async () => {
        hideError('login-error');
        const email = sanitize(document.getElementById('email').value.trim());
        const pass  = document.getElementById('password').value;

        if (!email || !pass) { showError('login-error', 'נא למלא אימייל וסיסמא'); return; }
        if (!isValidEmail(email)) { showError('login-error', 'פורמט אימייל לא תקין'); return; }

        try {
            const response = await fetch('https://localhost:8443/api/users/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ email: email, password: pass })
            });
            const result = await response.json();
 
            if (response.ok && result.status === "success") {
                saveSession(result.token, result.userId, result.username);
                document.getElementById('user-display').innerText = result.username;
                hideError('login-error');
                showScreen('menu');
            } else {
                showError('login-error', result.message || 'אימייל או סיסמה שגויים');
            }
        } catch (err) {
            showError('login-error', 'שגיאה בחיבור לשרת. ודא שהשרת רץ בפורט 8443');
        }
    };
}
 
// ─────────────────────────────────────────────────────────────
//  הרשמה
// ─────────────────────────────────────────────────────────────
const registerBtn = document.getElementById('registerBtn');
if (registerBtn) {
    registerBtn.onclick = async () => {
        hideError('register-error');
        const username  = sanitize(document.getElementById('reg-username').value.trim());
        const email     = sanitize(document.getElementById('reg-email').value.trim());
        const password  = document.getElementById('reg-password').value;
        const password2 = document.getElementById('reg-password2').value;

        if (!username || !email || !password || !password2) { showError('register-error', 'נא למלא את כל השדות'); return; }
        if (!isValidEmail(email)) { showError('register-error', 'פורמט אימייל לא תקין'); return; }
        if (!isValidPassword(password)) {
            showError('register-error', 'הסיסמא חייבת להכיל לפחות 6 תווים, אות גדולה, אות קטנה וספרה');
            return;
        }
        if (password !== password2) { showError('register-error', 'הסיסמאות לא תואמות!'); return; }
 
        try {
            const response = await fetch('https://localhost:8443/api/users/register', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ username, email, password })
            });
            const result = await response.text();
            if (response.ok && result.includes("OK")) {
                showScreen('login');
                const loginError = document.getElementById('login-error');
                if (loginError) {
                    loginError.textContent = 'נרשמת בהצלחה! כעת התחבר';
                    loginError.style.display = 'block';
                    loginError.style.color = '#4caf50';
                }
            } else {
                showError('register-error', result.replace("ERROR", "").trim());
            }
        } catch (err) {
            showError('register-error', 'שגיאה בחיבור לשרת');
        }
    };
}

// --- ניהול ארגזים ---
document.getElementById('addBoxBtn').onclick = () => {
    const l       = parseFloat(document.getElementById('newBoxL').value);
    const w       = parseFloat(document.getElementById('newBoxW').value);
    const h       = parseFloat(document.getElementById('newBoxH').value);
    const qty     = parseInt(document.getElementById('newBoxQty').value);
    const rotType = document.getElementById('newBoxRotation').value;

    if (!l || !w || !h || !qty)        { alert("⚠️ נא למלא את כל שדות הארגז"); return; }
    if (l <= 0 || w <= 0 || h <= 0)   { alert("⚠️ מידות הארגז חייבות להיות חיוביות!"); return; }
    if (qty <= 0)                       { alert("⚠️ הכמות חייבת להיות חיובית!"); return; }

    let allowedRotations = [0, 1, 2, 3, 4, 5];
    if (rotType === "1") allowedRotations = [0];
    else if (rotType === "2") allowedRotations = [0, 1];

    boxesToPack.push({ typeId: boxesToPack.length + 1, l, w, h, quantity: qty, allowedRotations });
    updateTable();
    ['newBoxL', 'newBoxW', 'newBoxH', 'newBoxQty'].forEach(id => document.getElementById(id).value = '');
};

// תוקן באג 4: הגדרת removeBox כפונקציה גלובלית — לפני כן לא הייתה מוגדרת בכלל
window.removeBox = (index) => {
    boxesToPack.splice(index, 1);
    updateTable();
};

document.getElementById('jsonFileInput')?.addEventListener('change', function(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const jsonData = JSON.parse(e.target.result);
            const newBoxes = Array.isArray(jsonData) ? jsonData : [jsonData];
            let addedCount = 0;
            newBoxes.forEach(box => {
                const l = parseFloat(box.l), w = parseFloat(box.w), h = parseFloat(box.h);
                const quantity = parseInt(box.quantity);
                if (l > 0 && w > 0 && h > 0 && quantity > 0) {
                    boxesToPack.push({
                        typeId: boxesToPack.length + 1, l, w, h, quantity,
                        allowedRotations: box.allowedRotations || [0, 1, 2, 3, 4, 5]
                    });
                    addedCount++;
                }
            });
            if (addedCount > 0) { updateTable(); alert(`ייבוא הושלם: נוספו ${addedCount} סוגי ארגזים`); }
            else alert("לא נמצאו נתונים תקינים.");
        } catch (err) {
            alert("שגיאה בקריאת הקובץ");
        }
        event.target.value = '';
    };
    reader.readAsText(file);
});

function updateTable() {
    const tbody = document.getElementById('boxesTableBody');
    if (!tbody) return;
    tbody.innerHTML = boxesToPack.map((box, index) => {
        let rotStr = "שש-כיווני";
        if (box.allowedRotations.length === 1) rotStr = "חד-כיווני";
        else if (box.allowedRotations.length === 2) rotStr = "דו-כיווני";
        return `
        <tr>
            <td>${box.l}</td><td>${box.w}</td><td>${box.h}</td>
            <td>${box.quantity}</td>
            <td style="color:#e0787a;">${rotStr}</td>
            <td><button onclick="removeBox(${index})">מחק</button></td>
        </tr>`;
    }).join('');
}

// --- חישוב אלגוריתם (תוקן באג 5: היה מוגדר פעמיים — הגרסה השנייה דרסה את הראשונה ועבדה בלי isBenchmark) ---
document.getElementById('calculateBtn').onclick = async () => {
    if (boxesToPack.length === 0) return alert("נא להוסיף ארגזים");

    const container = {
        l: parseFloat(document.getElementById('contL').value) || 1200,
        w: parseFloat(document.getElementById('contW').value) || 235,
        h: parseFloat(document.getElementById('contH').value) || 269
    };

    try {
        const response = await authenticatedFetch('https://localhost:8443/api/solve', {
            method: 'POST',
            body: JSON.stringify({ userId: currentUserId, container, boxes: boxesToPack, isBenchmark: false })
        }); // תוקן: fetch → authenticatedFetch (מוסיף Authorization header)
        const result = await response.json();
        if (result && result.placements && result.placements.length > 0) {
            lastCalculatedData = result.placements;
            showScreen('result');
            init3D(result.placements, container);
        } else {
            alert("לא נמצא סידור אפשרי");
        }
    } catch (err) {
        alert("שגיאה בחיבור לשרת החישוב");
    }
};

// --- שליחת בנצ'מארק ---
async function runBenchmarkCase(tc) {
    const t0 = Date.now();
    const preparedBoxes = tc.boxes.map(b => ({
        ...b, allowedRotations: b.allowedRotations || [0, 1, 2, 3, 4, 5]
    }));
    const response = await authenticatedFetch('https://localhost:8443/api/solve', {
        method: 'POST',
        body: JSON.stringify({ userId: currentUserId, container: tc.container, boxes: preparedBoxes, isBenchmark: true })
    }); // תוקן: authenticatedFetch
    const data = await response.json();
    const ms = Date.now() - t0;
    const result = data.placements || [];
    const util = data.vur ? (data.vur * 100).toFixed(1) + '%' : '—';

    return { result, ms, util };
}

// --- היסטוריה ---
document.getElementById('viewHistoryBtn')?.addEventListener('click', loadHistory);
document.getElementById('historyBackBtn')?.addEventListener('click', () => showScreen('menu'));

// תוקן באג 1 + 2: הפונקציה הייתה מוגדרת פעמיים ועם שם מסך שגוי ('history-screen' במקום 'history')
async function loadHistory() {
    showScreen('history');    // ← 'history' ולא 'history-screen'
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '<tr><td colspan="3">טוען נתונים...</td></tr>';

    try {
        const response = await authenticatedFetch(
            `https://localhost:8443/api/history?userId=${currentUserId}`
        );
        const historyData = await response.json();

        if (historyData.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">אין סידורים קודמים</td></tr>';
            return;
        }
        tbody.innerHTML = historyData.map(item => `
    <tr>
        <td style="font-size:12px;">${sanitize(new Date(item.createdAt).toLocaleDateString())}</td>
        <td style="color:#e0787a; font-weight:bold;">${sanitize((item.vur * 100).toFixed(1))}%</td>
        <td>
            <button onclick='loadAndViewHistory("${sanitize(item.id)}")'              
                            style="padding:4px 8px; font-size:11px; width:auto;">צפה</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="3" style="color:red;">שגיאה בטעינת הנתונים</td></tr>';
    }
}

// תוקן באג 6: loadAndViewHistory לא הייתה מוגדרת בכלל — כפתור "צפה" זרק שגיאה
window.loadAndViewHistory = async (id) => {
    try {
        const response = await authenticatedFetch(`https://localhost:8443/api/history/${id}`);
        const data = await response.json();

        if (!data || !data.placements || data.placements.length === 0) {
            alert('לא נמצאו נתוני סידור לפריט זה');
            return;
        }

        lastCalculatedData = data.placements;
        showScreen('result');

        // ברירת מחדל — אין מידות מכולה בתשובת השרת הנוכחית
        const container = { l: data.containerL, w: data.containerW, h: data.containerH };
        init3D(data.placements, container);

    } catch (err) {
        alert('שגיאה בטעינת הסידור: ' + err.message);
    }
};

window.viewSpecificHistory = (steps, container) => {
    lastCalculatedData = steps;
    showScreen('result');
    init3D(steps, container);
};

//  מסך בנצ'מארק

let benchmarkCases = [];

function parseBenchmarkFile(text) {
    const tokens = text.trim().split(/\s+/).map(Number);
    let i = 0;
    const nxt = () => tokens[i++];
    const cases = [];
    const n = nxt();
    for (let c = 0; c < n; c++) {
        const caseNum   = nxt();
        const problemId = nxt();
        const cl = nxt(), cw = nxt(), ch = nxt();
        const nBoxes = nxt();
        const boxes = [];
        for (let b = 0; b < nBoxes; b++) {
            const typeId = nxt();
            const l = nxt(); nxt();
            const w = nxt(); nxt();
            const h = nxt(); nxt();
            const quantity = nxt();
            boxes.push({ typeId, l, w, h, quantity });
        }
        cases.push({ caseNum, problemId, container: { l: cl, w: cw, h: ch }, boxes });
    }
    return cases;
}

function loadBenchmarkFile(file) {
    if (!file) return;
    document.getElementById('benchmarkFileName').textContent = `📄 ${file.name}`;
    const reader = new FileReader();
    reader.onload = e => {
        try {
            benchmarkCases = parseBenchmarkFile(e.target.result);
            populateBenchmarkUI();
        } catch (err) {
            alert('שגיאה בקריאת הקובץ: ' + err.message);
        }
    };
    reader.readAsText(file);
}

function populateBenchmarkUI() {
    const info   = document.getElementById('benchmarkInfo');
    const stats  = document.getElementById('benchmarkStats');
    const select = document.getElementById('benchmarkCaseSelect');
    const logDiv = document.getElementById('benchmarkLog');

    info.style.display = 'block';
    logDiv.style.display = 'none';
    logDiv.innerHTML = '';

    const avgBoxes = (benchmarkCases.reduce((s, c) => s + c.boxes.reduce((ss, b) => ss + b.quantity, 0), 0) / benchmarkCases.length).toFixed(1);
    const types    = benchmarkCases[0]?.boxes.length || 0;
    stats.textContent = `${benchmarkCases.length} מקרים  |  ${types} סוגי ארגזים  |  ממוצע ${avgBoxes} ארגזים למקרה`;

    select.innerHTML = '<option value="">— בחר מקרה —</option>';
    benchmarkCases.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.caseNum;
        opt.textContent = `מקרה #${c.caseNum}  (ID: ${c.problemId})`;
        select.appendChild(opt);
    });

    document.getElementById('benchmarkRunBtn').disabled = true;
    document.getElementById('benchmarkCaseDetails').style.display = 'none';
}

document.getElementById('benchmarkCaseSelect')?.addEventListener('change', e => {
    const caseNum = parseInt(e.target.value);
    const tc = benchmarkCases.find(c => c.caseNum === caseNum);
    if (!tc) {
        document.getElementById('benchmarkCaseDetails').style.display = 'none';
        document.getElementById('benchmarkRunBtn').disabled = true;
        return;
    }
    showCaseDetails(tc);
    document.getElementById('benchmarkRunBtn').disabled = false;
});

document.getElementById('benchmarkCaseFilter')?.addEventListener('input', e => {
    const q = e.target.value.trim();
    const select = document.getElementById('benchmarkCaseSelect');
    Array.from(select.options).forEach(opt => {
        if (!opt.value) return;
        opt.style.display = opt.textContent.includes(q) ? '' : 'none';
    });
});

function showCaseDetails(tc) {
    document.getElementById('benchmarkCaseDetails').style.display = 'block';
    const contVol  = tc.container.l * tc.container.w * tc.container.h;
    const cargoVol = tc.boxes.reduce((s, b) => s + b.l * b.w * b.h * b.quantity, 0);
    const util     = ((cargoVol / contVol) * 100).toFixed(1);

    document.getElementById('bcContainer').textContent = `${tc.container.l} × ${tc.container.w} × ${tc.container.h}`;
    document.getElementById('bcTypes').textContent  = tc.boxes.length;
    document.getElementById('bcTotal').textContent  = tc.boxes.reduce((s, b) => s + b.quantity, 0);
    document.getElementById('bcUtil').textContent   = `${util}%`;

    document.getElementById('bcBoxesBody').innerHTML = tc.boxes.map(b => `
        <tr style="border-top:1px solid #333;">
            <td style="padding:3px 6px; color:#888;">${b.typeId}</td>
            <td style="padding:3px 6px;">${b.l}</td>
            <td style="padding:3px 6px;">${b.w}</td>
            <td style="padding:3px 6px;">${b.h}</td>
            <td style="padding:3px 6px;">${b.quantity}</td>
        </tr>`).join('');
}

document.getElementById('benchmarkRunBtn')?.addEventListener('click', async () => {
    const caseNum = parseInt(document.getElementById('benchmarkCaseSelect').value);
    const tc = benchmarkCases.find(c => c.caseNum === caseNum);
    if (!tc) return;

    const btn = document.getElementById('benchmarkRunBtn');
    btn.disabled = true;
    btn.textContent = 'שולח...';

    try {
        const { result, ms, util } = await runBenchmarkCase(tc);
        if (result.length > 0) {
            lastCalculatedData = result;
            showScreen('result');
            init3D(result, tc.container);
        } else {
            alert(`מקרה #${tc.caseNum} – לא נמצא סידור אפשרי`);
        }
    } catch (err) {
        alert(`שגיאה: ${err.message}`);
    } finally {
        btn.disabled = false;
        btn.textContent = 'הרץ מקרה זה ▶';
    }
});

document.getElementById('benchmarkRunAllBtn')?.addEventListener('click', async () => {
    if (benchmarkCases.length === 0) return alert('נא לטעון קובץ תחילה');

    const btn    = document.getElementById('benchmarkRunAllBtn');
    const logDiv = document.getElementById('benchmarkLog');
    btn.disabled = true;
    btn.textContent = 'מריץ...';
    logDiv.style.display = 'block';
    logDiv.innerHTML = `<span style="color:#aaa">מריץ ${benchmarkCases.length} מקרים...\n</span>`;

    let ok = 0, fail = 0, times = [], utils = [];
    for (const tc of benchmarkCases) {
        try {
            const { ms, util } = await runBenchmarkCase(tc);
            ok++;
            times.push(ms);
            if (util !== '—') utils.push(parseFloat(util));
            logDiv.innerHTML += `<span style="color:#4caf50">✅ #${String(tc.caseNum).padStart(3)}  ${ms}ms  ניצולת: ${util}</span>\n`;
        } catch (err) {
            fail++;
            logDiv.innerHTML += `<span style="color:#ff4d4d">❌ #${String(tc.caseNum).padStart(3)}  שגיאה: ${err.message}</span>\n`;
        }
        logDiv.scrollTop = logDiv.scrollHeight;
    }

    const avgTime = times.length ? Math.round(times.reduce((a,b)=>a+b,0)/times.length) : 0;
    const avgUtil = utils.length ? (utils.reduce((a,b)=>a+b,0)/utils.length).toFixed(1) : '—';
    logDiv.innerHTML += `\n<span style="color:#f0c040">━━━ סיכום ━━━\n✅ הצלחות: ${ok}  ❌ כישלונות: ${fail}\n⏱ זמן ממוצע: ${avgTime}ms\n📊 ניצולת ממוצעת: ${avgUtil}%</span>\n`;
    logDiv.scrollTop = logDiv.scrollHeight;
    btn.disabled = false;
    btn.textContent = 'הרץ את כולם ⚡';
});

const dropZone = document.getElementById('benchmarkDropZone');
dropZone?.addEventListener('dragover', e => { e.preventDefault(); dropZone.style.borderColor = '#e0787a'; });
dropZone?.addEventListener('dragleave', () => { dropZone.style.borderColor = '#555'; });
dropZone?.addEventListener('drop', e => {
    e.preventDefault();
    dropZone.style.borderColor = '#555';
    const file = e.dataTransfer.files[0];
    if (file) loadBenchmarkFile(file);
});
document.getElementById('benchmarkFileInput')?.addEventListener('change', e => loadBenchmarkFile(e.target.files[0]));

// --- וידאו ---
let finalVideoBlob = null;

function downloadVideo(lengthBox) {
    const stream   = renderer.domElement.captureStream(30);
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    const chunks   = [];
    recorder.ondataavailable = e => chunks.push(e.data);
    recorder.onstop = () => {
        finalVideoBlob = new Blob(chunks, { type: 'video/webm' });
        const saveBtn = document.getElementById('saveVideoBtn');
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = "שמור סרטון הדמיה 🎬";
            saveBtn.style.background = "#4caf50";
        }
    };
    recorder.start();
    setTimeout(() => recorder.stop(), 300 * (lengthBox + 2));
}

document.getElementById('saveVideoBtn').onclick = () => {
    if (!finalVideoBlob) return alert("הסרטון עדיין בתהליך הכנה...");
    const url = URL.createObjectURL(finalVideoBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'packing_simulation.webm';
    a.click();
};

document.getElementById('replayBtn')?.addEventListener('click', () => {
    if (lastCalculatedData.length > 0) runPackingAnimation(lastCalculatedData);
});

// --- הדפסה ---
const printBtn = document.getElementById('printTableBtn');
if (printBtn) {
    printBtn.onclick = () => {
        if (lastCalculatedData.length === 0) return alert("אין נתונים להדפסה");
        const win = window.open('', '', 'width=800,height=600');
        win.document.write(`
            <html><head>
            <style>table{width:100%;border-collapse:collapse;direction:rtl}th,td{border:1px solid #2f3e46;padding:8px;text-align:center}</style>
            </head><body>
            <h2>דוח טעינה</h2>
            <table>
            <tr><th>סוג</th><th>שמאל תחתוןX</th><th>שמאל תחתוןY</th><th>שמאל תחתוןZ</th>
            <th>ימין עליוןX</th><th>ימין עליוןY</th><th>ימין עליוןZ</th><th>סיבוב</th></tr>
        `);
        lastCalculatedData.forEach(b => {
            win.document.write(`
                <tr>
                    <td>${b.cargoType}</td>
                    <td>${b.leftBackBottom[0]}</td><td>${b.leftBackBottom[1]}</td><td>${b.leftBackBottom[2]}</td>
                    <td>${b.rightFrontTop[0]}</td><td>${b.rightFrontTop[1]}</td><td>${b.rightFrontTop[2]}</td>
                    <td>${b.direction}</td>
                </tr>`);
        });
        win.document.write('</table></body></html>');
        win.document.close();
        win.print();
    };
}

// --- הורדת CSV ---
const downloadBtn = document.getElementById('downloadExcelBtn');
if (downloadBtn) {
    downloadBtn.onclick = () => {
        if (lastCalculatedData.length === 0) return alert("אין נתונים להורדה");
        let csv = "\uFEFFסוג,X_התחלה,Y_התחלה,Z_התחלה,X_סוף,Y_סוף,Z_סוף\n";
        lastCalculatedData.forEach(b => {
            csv += `${b.cargoType},${b.leftBackBottom.join(',')},${b.rightFrontTop.join(',')}\n`;
        });
        const blob = new Blob([csv], { type: 'text/csv' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = "packing_report.csv";
        link.click();
    };
}

// --- אנימציה ---
function runPackingAnimation(data) {
    currentMeshBoxes.forEach(obj => scene.remove(obj));
    currentMeshBoxes = [];

    data.forEach((box, index) => {
        setTimeout(() => {
            const l = parseFloat((box.rightFrontTop[0] - box.leftBackBottom[0]).toFixed(2));
            const w = parseFloat((box.rightFrontTop[1] - box.leftBackBottom[1]).toFixed(2));
            const h = parseFloat((box.rightFrontTop[2] - box.leftBackBottom[2]).toFixed(2));

            const geometry = new THREE.BoxGeometry(l, h, w);
            const material = new THREE.MeshPhongMaterial({
                color: new THREE.Color(`hsl(${box.cargoType * 137}, 70%, 50%)`),
                transparent: true, opacity: 0.85
            });

            const mesh = new THREE.Mesh(geometry, material);
            mesh.position.set(
                box.leftBackBottom[0] + l / 2,
                box.leftBackBottom[2] + h / 2,
                box.leftBackBottom[1] + w / 2
            );

            const edges     = new THREE.EdgesGeometry(geometry);
            const wireframe = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0x000000 }));
            wireframe.position.copy(mesh.position);

            scene.add(mesh, wireframe);
            currentMeshBoxes.push(mesh, wireframe);
        }, index * 300);
    });
}

let currentMeshBoxes = [];

function init3D(data, containerDim) {
    // תוקן: הסרת לוח הסטטיסטיקות הישן לפני יצירת חדש — בלי זה כל קריאה הוסיפה div חדש
    const oldStats = document.getElementById('stats-panel');
    if (oldStats) oldStats.remove();

    const containerVolume = containerDim.l * containerDim.w * containerDim.h;
    let packedVolume = 0;
    data.forEach(box => {
        const l = Math.abs(box.rightFrontTop[0] - box.leftBackBottom[0]);
        const w = Math.abs(box.rightFrontTop[1] - box.leftBackBottom[1]);
        const h = Math.abs(box.rightFrontTop[2] - box.leftBackBottom[2]);
        packedVolume += (l * w * h);
    });
    const efficiency = ((packedVolume / containerVolume) * 100).toFixed(2);

    const statsDiv = document.createElement('div');
    statsDiv.id = 'stats-panel';
    statsDiv.style = "position:absolute; top:10px; left:10px; background:rgba(0,0,0,0.8); color:white; padding:15px; border-radius:8px; z-index:100; font-family:Arial;";
    statsDiv.innerHTML = `
        <h3 style="margin:0 0 10px 0">סיכום טעינה</h3>
        <p>מכולה: ${containerDim.l}×${containerDim.w}×${containerDim.h}</p>
        <p>ניצולת: <strong style="color:#e0787a">${efficiency}%</strong></p>`;
    document.body.appendChild(statsDiv);

    if (renderer) document.body.removeChild(renderer.domElement);

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x1a1a1a);

    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(containerDim.l * 1.5, containerDim.h * 1.5, containerDim.w * 1.5);

    renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);
    controls = new OrbitControls(camera, renderer.domElement);
    scene.add(new THREE.AmbientLight(0xffffff, 0.7));

    const light = new THREE.DirectionalLight(0xffffff, 0.5);
    light.position.set(1, 1, 1);
    scene.add(light);

    const containerGeo  = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
    const containerMesh = new THREE.Mesh(containerGeo, new THREE.MeshBasicMaterial({ color: 0xE0787A, wireframe: true }));
    containerMesh.position.set(containerDim.l / 2, containerDim.h / 2, containerDim.w / 2);
    scene.add(containerMesh);

    downloadVideo(data.length);
    runPackingAnimation(data);

    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }
    animate();
}