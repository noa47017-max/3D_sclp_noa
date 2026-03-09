import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// --- משתנים גלובליים ---
let scene, camera, renderer, controls;
let boxesToPack = [];
let lastCalculatedData = [];
let currentUserId = null;

const screens = {
    login: document.getElementById('login-screen'),
    menu: document.getElementById('menu-screen'),
    creator: document.getElementById('creator-screen'),
    result: document.getElementById('result-screen'),
    register: document.getElementById('register-screen')
};

// --- חשיפת הפונקציה לעולם החיצון (תיקון השגיאה שלך) ---
export function showScreen(screenName) {
    Object.values(screens).forEach(s => {
        if (s) s.style.display = 'none';
    });
    if (screens[screenName]) {
        screens[screenName].style.display = 'flex';
    }
}
// הופך את הפונקציה לזמינה גם מתוך ה-HTML אם ממש חייבים
window.showScreen = showScreen; 

// --- חיבור מאזינים לכפתורי ניווט ---
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('goToRegisterBtn')?.addEventListener('click', () => showScreen('register'));
    document.getElementById('backToLoginBtn')?.addEventListener('click', () => showScreen('login'));
    document.getElementById('createNewBtn')?.addEventListener('click', () => showScreen('creator'));
});


document.getElementById('goToRegisterBtn')?.addEventListener('click', () => showScreen('register'));

// --- פונקציית עזר להודעות שגיאה ---
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

// --- התחברות (מתוקן ל-HTTPS) ---
const loginBtn = document.getElementById('loginBtn');
if (loginBtn) {
    loginBtn.onclick = async () => {
        hideError('login-error');
        const user = document.getElementById('username').value;
        const pass = document.getElementById('password').value;

        if (!user || !pass) {
            showError('login-error', '⚠️ נא למלא שם משתמש וסיסמא');
            return;
        }

        try {
            // שינוי הכתובת ל-HTTPS ופורט 8443
            const response = await fetch('https://localhost:8443/api/users/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ username: user, password: pass })
            });

            const result = await response.json();
            if (response.ok && result.status === "success") {
                currentUserId = result.userId;
                hideError('login-error');
                document.getElementById('user-display').innerText = user;
                showScreen('menu');
            } else {
                showError('login-error', result.replace("❌", "").trim() || 'שם משתמש או סיסמה שגויים');
            }

        } catch (err) {
            showError('login-error', '⚠️ שגיאה בחיבור לשרת המאובטח. ודאי שהשרת רץ בפורט 8443');
        }
    };
}

// --- הרשמה (מתוקן ל-HTTPS) ---
const registerBtn = document.getElementById('registerBtn');
if (registerBtn) {
    registerBtn.onclick = async () => {
        hideError('register-error');
        const username  = document.getElementById('reg-username').value;
        const email     = document.getElementById('reg-email').value;
        const password  = document.getElementById('reg-password').value;
        const password2 = document.getElementById('reg-password2').value;

        if (!username || !email || !password || !password2) {
            showError('register-error', '⚠️ נא למלא את כל השדות');
            return;
        }
        if (password !== password2) {
            showError('register-error', '⚠️ הסיסמאות לא תואמות!');
            return;
        }

        try {
            // שינוי הכתובת ל-HTTPS ופורט 8443
            const response = await fetch('https://localhost:8443/api/users/register', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ username, email, password })
            });

            const result = await response.text();

            if (response.ok && result.includes("✅")) {
                showScreen('login');
                const loginError = document.getElementById('login-error');
                if (loginError) {
                    loginError.textContent = '✅ נרשמת בהצלחה! כעת התחבר';
                    loginError.style.display = 'block';
                    loginError.style.color = '#4caf50';
                }
            } else {
                showError('register-error', result.replace("❌", "").trim());
            }

        } catch (err) {
            showError('register-error', '⚠️ שגיאה בחיבור לשרת המאובטח');
        }
    };
}

// --- שחזור סיסמא ---
async function forgotPassword() {
    const email = prompt("הכניסי את האימייל שלך:");
    if (!email) return;

    try {
        const response = await fetch('https://localhost:8443/api/users/forgot-password', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ email })
        });
        const result = await response.text();
        alert(result);
    } catch (err) {
        alert("שגיאה בחיבור לשרת");
    }
}

document.getElementById('forgotPasswordBtn')?.addEventListener('click', forgotPassword);

// --- ניהול ארגזים וטבלה ---
document.getElementById('addBoxBtn').onclick = () => {
    const l   = parseFloat(document.getElementById('newBoxL').value);
    const w   = parseFloat(document.getElementById('newBoxW').value);
    const h   = parseFloat(document.getElementById('newBoxH').value);
    const qty = parseInt(document.getElementById('newBoxQty').value);

    // ולידציה מלאה
    if (!l || !w || !h || !qty) {
        alert("⚠️ נא למלא את כל שדות הארגז");
        return;
    }
    if (l <= 0 || w <= 0 || h <= 0) {
        alert("⚠️ מידות הארגז חייבות להיות חיוביות!");
        return;
    }
    if (qty <= 0) {
        alert("⚠️ הכמות חייבת להיות חיובית!");
        return;
    }

    boxesToPack.push({ typeId: boxesToPack.length + 1, l, w, h, quantity: qty });
    updateTable();
    ['newBoxL', 'newBoxW', 'newBoxH', 'newBoxQty']
        .forEach(id => document.getElementById(id).value = '');
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
                const l = parseFloat(box.l);
                const w = parseFloat(box.w);
                const h = parseFloat(box.h);
                const quantity = parseInt(box.quantity);

                // ולידציה
                if (l > 0 && w > 0 && h > 0 && quantity > 0) {
                    boxesToPack.push({
                        typeId: boxesToPack.length + 1,
                        l, w, h, quantity
                    });
                    addedCount++;
                }
            });

            if (addedCount > 0) {
                updateTable();
                alert(`ייבוא הושלם: נוספו ${addedCount} סוגי ארגזים`);
            } else {
                alert("לא נמצאו נתונים תקינים. וודא שהשדות הם l, w, h, quantity וכולם חיוביים");
            }

        } catch (err) {
            alert("שגיאה בקריאת הקובץ – וודא שזהו JSON תקין");
        }
        event.target.value = '';
    };
    reader.readAsText(file);
});
function updateTable() {
    const tbody = document.getElementById('boxesTableBody');
    if (!tbody) return;
    tbody.innerHTML = boxesToPack.map((box, index) => `
        <tr>
            <td>${box.l}</td>
            <td>${box.w}</td>
            <td>${box.h}</td>
            <td>${box.quantity}</td>
            <td><button onclick="removeBox(${index})">מחק</button></td>
        </tr>
    `).join('');
}

window.removeBox = (index) => {
    boxesToPack.splice(index, 1);
    updateTable();
};

// --- חישוב אלגוריתם (HTTPS) ---
document.getElementById('calculateBtn').onclick = async () => {
    if (boxesToPack.length === 0) return alert("נא להוסיף ארגזים");

    const container = {
        l: parseFloat(document.getElementById('contL').value) || 1200,
        w: parseFloat(document.getElementById('contW').value) || 235,
        h: parseFloat(document.getElementById('contH').value) || 269
    };

    try {
        const response = await fetch('https://localhost:8443/api/solve', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userId: currentUserId, container, boxes: boxesToPack })
        });

        const result = await response.json();
        if (result && result.length > 0) {
            lastCalculatedData = result;
            showScreen('result');
            init3D(result, container);
        } else {
            alert("לא נמצא סידור אפשרי");
        }
    } catch (err) {
        alert("שגיאה בחיבור לשרת החישוב");
    }
};

// --- וידאו והקלטה ---
let finalVideoBlob = null;

function downloadVideo(lengthBox) {
    const stream = renderer.domElement.captureStream(30);
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    const chunks = [];

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
        if (lastCalculatedData.length === 0)
            return alert("אין נתונים להדפסה");
           const win = window.open('', '', 'width=800,height=600');
           win.document.write(`
            <html><head>
            <style>
            table{width:100%;border-collapse:collapse;direction:rtl}
            th,td{border:1px solid #2f3e46;padding:8px;text-align:center}
            </style>
            </head><body>
            <h2>דוח טעינה</h2>
            <table>
            <tr><th>סוג</th><th>שמאל תחתוןX</th><th>שמאל תחתוןY</th><th>שמאל תחתוןZ</th>
            <th>ימין עליוןX</th><th>ןימין עליוY</th><th>ימין עליוןZ</th>
            <th>סיבוב</th></tr>

        `);
        lastCalculatedData.forEach(b => {
            win.document.write(`
                <tr>
                    <td>${b.cargoType}</td>
                    <td>${b.leftBackBottom[0]}</td>
                    <td>${b.leftBackBottom[1]}</td>
                    <td>${b.leftBackBottom[2]}</td>
                    <td>${b.rightFrontTop[0]}</td>
                    <td>${b.rightFrontTop[1]}</td>
                    <td>${b.rightFrontTop[2]}</td>
                    <td>${b.remainingSpace}</td>

                </tr>
            `);
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
        if (lastCalculatedData.length === 0)

            return alert("אין נתונים להורדה");



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
                transparent: true,
                opacity: 0.85
            });

            const mesh = new THREE.Mesh(geometry, material);
            mesh.position.set(
                box.leftBackBottom[0] + l / 2,
                box.leftBackBottom[2] + h / 2,
                box.leftBackBottom[1] + w / 2
            );

            const edges = new THREE.EdgesGeometry(geometry);
            const wireframe = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0x000000 }));
            wireframe.position.copy(mesh.position);

            scene.add(mesh, wireframe);
            currentMeshBoxes.push(mesh, wireframe);
        }, index * 300);
    });
}

let currentMeshBoxes = [];
function init3D(data, containerDim) {
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
        statsDiv.innerHTML =`<h3 style="margin:0 0 10px 0">סיכום טעינה</h3>
        <p>מכולה: ${containerDim.l}x${containerDim.w}x${containerDim.h}</p>
        <p>נצילות: <strong style="color:#2f3e46">${efficiency}%</strong></p>
        `;        document.body.appendChild(statsDiv);
    if (renderer) document.body.removeChild(renderer.domElement);
    
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x1a1a1a);
    
    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(containerDim.l*1.5, containerDim.h * 1.5, containerDim.w * 1.5);

    renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);
    controls = new OrbitControls(camera, renderer.domElement);
    scene.add(new THREE.AmbientLight(0xffffff, 0.7));
    
    const light = new THREE.DirectionalLight(0xffffff, 0.5);
    light.position.set(1, 1, 1);
    scene.add(light);

    // מכולה (Wireframe)
    const containerGeo = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
    const containerMesh = new THREE.Mesh(containerGeo, new THREE.MeshBasicMaterial({ color: 0xE0787AFF, wireframe: true }));
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