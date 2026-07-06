import * as THREE from 'three';

// ─── ייבוא מודולים ───
import { showScreen, lastCalculatedData, attachNavigationListeners, initHistoryFromURL, initBrowserHistory } from './modules/ui.js';
import { 
    jwtToken, 
    initSession, 
    handleLogin, 
    handleRegister, 
    handleForgotPassword,
    logout as logoutAuth,
    clearSession
} from './modules/auth.js';
import { 
    boxesToPack, 
    addBoxType, 
    clearBoxes, 
    updateBoxesTable, 
    calculatePacking, 
    importBoxesFromFile 
} from './modules/boxManager.js';
import { 
    init3D, 
    runPackingAnimation, 
    startVideoRecording 
} from './modules/visualization.js';
import { 
    printTable, 
    downloadCSV, 
    saveVideo, 
    updateVideoButtonState 
} from './modules/export.js';
import { 
    parseBenchmarkFile, 
    runBenchmarkCase, 
    runAllBenchmarkCases 
} from './modules/benchmark.js';

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ── אתחול האפליקציה ──
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

document.addEventListener('DOMContentLoaded', () => {
    // ─── אתחול סשן ─── 
    const isLoggedIn = initSession();
    
    // ─── אתחול UI ───
    initHistoryFromURL(isLoggedIn);
    initBrowserHistory();
    attachNavigationListeners();
    
    // ─── קישור כפתורי התחברות ־ הרשמה ───
    document.getElementById('loginBtn')?.addEventListener('click', async () => {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const success = await handleLogin(email, password);
        
        if (success) {
            document.getElementById('email').value = '';
            document.getElementById('password').value = '';
            showScreen('menu');
        }
    });

    document.getElementById('registerBtn')?.addEventListener('click', async () => {
        const username = document.getElementById('reg-username').value;
        const email = document.getElementById('reg-email').value;
        const password = document.getElementById('reg-password').value;
        const password2 = document.getElementById('reg-password2').value;
        
        const success = await handleRegister(username, email, password, password2);
        
        if (success) {
            document.getElementById('reg-username').value = '';
            document.getElementById('reg-email').value = '';
            document.getElementById('reg-password').value = '';
            document.getElementById('reg-password2').value = '';
            showScreen('menu');
        }
    });

    document.getElementById('forgotPasswordBtn')?.addEventListener('click', async () => {
        const email = prompt('הזן את האימייל שלך:');
        if (email) {
            await handleForgotPassword(email);
        }
    });

    // ─── כפתור התנתקות דינמי בתפריט ───
    const menuModal = document.querySelector('#menu-screen .modal');
    if (menuModal && !document.getElementById('logoutMenuBtn')) {
        const logoutBtn = document.createElement('button');
        logoutBtn.id = 'logoutMenuBtn';
        logoutBtn.textContent = 'התנתק 🚪';
        logoutBtn.style.cssText = 'background:#444; margin-top:8px; font-size:12px;';
        logoutBtn.onclick = () => {
            logoutAuth();
            clearSession();
            showScreen('login');
        };
        menuModal.appendChild(logoutBtn);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ── מסך יצירת סידור (Creator) ──
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // הוספת ארגז חדש
    document.getElementById('addBoxBtn')?.addEventListener('click', () => {
        const length = document.getElementById('newBoxL').value;
        const width = document.getElementById('newBoxW').value;
        const height = document.getElementById('newBoxH').value;
        const qty = document.getElementById('newBoxQty').value;
        const rotation = document.getElementById('newBoxRotation').value;

        if (addBoxType(length, width, height, qty, rotation)) {
            document.getElementById('newBoxL').value = '';
            document.getElementById('newBoxW').value = '';
            document.getElementById('newBoxH').value = '';
            document.getElementById('newBoxQty').value = '';
            updateBoxesTable();
        }
    });

    // ייבוא ארגזים מקובץ
    document.getElementById('jsonFileInput')?.addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (file) {
            const success = await importBoxesFromFile(file);
            if (success) {
                document.getElementById('jsonFileInput').value = '';
            }
        }
    });

    // ביצוע חישוב
    document.getElementById('calculateBtn')?.addEventListener('click', async () => {
        const containerL = document.getElementById('contL').value;
        const containerW = document.getElementById('contW').value;
        const containerH = document.getElementById('contH').value;

        const result = await calculatePacking(containerL, containerW, containerH);

        if (result) {
            // עדכון lastCalculatedData
            Object.assign(lastCalculatedData, result);
            
            // הצגת תוצאות
            init3D(result, { l: parseFloat(containerL), w: parseFloat(containerW), h: parseFloat(containerH) });
            
            updateVideoButtonState(false);
            startVideoRecording(result.length).then(blob => {
                updateVideoButtonState(true, blob);
            });
            
            showScreen('result');
        }
    });

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ── מסך בנצ'מארק ──
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    let benchmarkCases = [];

    const benchmarkDropZone = document.getElementById('benchmarkDropZone');
    if (benchmarkDropZone) {
        benchmarkDropZone.addEventListener('dragover', e => {
            e.preventDefault();
            benchmarkDropZone.style.borderColor = '#e0787a';
        });

        benchmarkDropZone.addEventListener('dragleave', () => {
            benchmarkDropZone.style.borderColor = '#555';
        });

        benchmarkDropZone.addEventListener('drop', e => {
            e.preventDefault();
            benchmarkDropZone.style.borderColor = '#555';
            const file = e.dataTransfer.files[0];
            if (file) loadBenchmarkFile(file);
        });
    }

    document.getElementById('benchmarkFileInput')?.addEventListener('change', e => {
        const file = e.target.files[0];
        if (file) loadBenchmarkFile(file);
    });

    function loadBenchmarkFile(file) {
        const reader = new FileReader();
        reader.onload = e => {
            try {
                benchmarkCases = parseBenchmarkFile(e.target.result);
                
                const fileName = document.getElementById('benchmarkFileName');
                if (fileName) {
                    fileName.textContent = `✓ נטען: ${file.name} (${benchmarkCases.length} מקרים)`;
                }

                const benchInfo = document.getElementById('benchmarkInfo');
                if (benchInfo) benchInfo.style.display = 'block';

                const benchStats = document.getElementById('benchmarkStats');
                if (benchStats) {
                    benchStats.textContent = `📊 ${benchmarkCases.length} מקרים`;
                }

                updateBenchmarkCaseSelect();
            } catch (err) {
                alert(`שגיאה בקריאת הקובץ: ${err.message}`);
            }
        };
        reader.readAsText(file);
    }

    function updateBenchmarkCaseSelect() {
        const select = document.getElementById('benchmarkCaseSelect');
        if (!select) return;

        select.innerHTML = '<option value="">— בחר מקרה —</option>';
        benchmarkCases.forEach(tc => {
            const option = document.createElement('option');
            option.value = tc.caseNum;
            option.textContent = `מקרה #${String(tc.caseNum).padStart(3, '0')} (${tc.boxes.length} ארגזים)`;
            select.appendChild(option);
        });
    }

    document.getElementById('benchmarkCaseSelect')?.addEventListener('change', () => {
        const caseNum = parseInt(document.getElementById('benchmarkCaseSelect').value);
        const tc = benchmarkCases.find(c => c.caseNum === caseNum);

        if (tc) {
            displayBenchmarkCaseDetails(tc);
            document.getElementById('benchmarkRunBtn').disabled = false;
        }
    });

    function displayBenchmarkCaseDetails(tc) {
        document.getElementById('benchmarkCaseDetails').style.display = 'block';
        document.getElementById('bcContainer').textContent = `${tc.containerLength}×${tc.containerWidth}×${tc.containerHeight}`;
        document.getElementById('bcTypes').textContent = tc.boxes.length;
        document.getElementById('bcTotal').textContent = tc.boxes.reduce((sum, b) => sum + b.quantity, 0);
        document.getElementById('bcUtil').textContent = '—';

        const tbody = document.getElementById('bcBoxesBody');
        tbody.innerHTML = '';
        tc.boxes.forEach((b, i) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td style="padding:3px 6px;">${i + 1}</td>
                <td style="padding:3px 6px;">${b.length}</td>
                <td style="padding:3px 6px;">${b.width}</td>
                <td style="padding:3px 6px;">${b.height}</td>
                <td style="padding:3px 6px;">${b.quantity}</td>
            `;
            tbody.appendChild(row);
        });
    }

    document.getElementById('benchmarkRunBtn')?.addEventListener('click', async () => {
        const caseNum = parseInt(document.getElementById('benchmarkCaseSelect').value);
        const tc = benchmarkCases.find(c => c.caseNum === caseNum);

        if (!tc) return;

        const btn = document.getElementById('benchmarkRunBtn');
        btn.disabled = true;
        btn.textContent = 'הרצה... ⏳';

        try {
            const result = await runBenchmarkCase(tc);
            document.getElementById('bcUtil').textContent = result.utilization + '%';
            btn.textContent = 'הרץ מקרה זה ▶';
        } catch (err) {
            alert(`שגיאה: ${err.message}`);
            btn.textContent = 'הרץ מקרה זה ▶';
        } finally {
            btn.disabled = false;
        }
    });

    document.getElementById('benchmarkRunAllBtn')?.addEventListener('click', async () => {
        if (benchmarkCases.length === 0) {
            alert('טען קבצי באנצמרק תחילה' );
            return;
        }

        const btn = document.getElementById('benchmarkRunAllBtn');
        btn.disabled = true;
        btn.textContent = 'בעיבוד... ⏳';

        const logDiv = document.getElementById('benchmarkLog');
        if (logDiv) {
            logDiv.style.display = 'block';
            logDiv.innerHTML = '';
        }

        const results = await runAllBenchmarkCases(benchmarkCases, (progress) => {
            if (!logDiv) return;

            if (progress.type === 'success') {
                logDiv.innerHTML += `<span style="color:#4caf50">✓ #${String(progress.caseNum).padStart(3, '0')}  ${progress.util}%  ${progress.time}ms</span>\n`;
            } else {
                logDiv.innerHTML += `<span style="color:#ff4d4d">✗ #${String(progress.caseNum).padStart(3, '0')}  ${progress.error}</span>\n`;
            }
            logDiv.scrollTop = logDiv.scrollHeight;
        });

        if (logDiv) {
            logDiv.innerHTML += `\n<span style="color:#f0c040">━━━ סיכום ━━━\n✅ ${results.passed}  ❌ ${results.failed}\n⏱ ${results.summary.avgTime}ms  📊 ${results.summary.avgUtil}%</span>\n`;
        }

        btn.disabled = false;
        btn.textContent = 'הרץ את כולם ⚡';
    });

    // ── כפתורי תוצאות (Export) ──

    document.getElementById('printTableBtn')?.addEventListener('click', () => {
        printTable(lastCalculatedData);
    });

    document.getElementById('downloadExcelBtn')?.addEventListener('click', () => {
        downloadCSV(lastCalculatedData);
    });

    document.getElementById('replayBtn')?.addEventListener('click', () => {
        if (lastCalculatedData.length > 0) {
            runPackingAnimation(lastCalculatedData);
        }
    });
});
