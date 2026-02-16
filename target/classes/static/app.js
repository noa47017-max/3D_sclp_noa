import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// --- משתנים גלובליים ---
let scene, camera, renderer, controls;
let boxesToPack = [];
let lastCalculatedData = [];

const screens = {
    login: document.getElementById('login-screen'),
    menu: document.getElementById('menu-screen'),
    creator: document.getElementById('creator-screen'),
    result: document.getElementById('result-screen')
};

function showScreen(screenName) {
    Object.values(screens).forEach(s => {
        if (s) s.style.display = 'none';
    });
    if (screens[screenName]) screens[screenName].style.display = 'flex';
}

// --- התחברות ---
const loginBtn = document.getElementById('loginBtn');
if (loginBtn) {
    loginBtn.onclick = async () => {
        const user = document.getElementById('username').value;
        const pass = document.getElementById('password').value;

        try {
            const response = await fetch('/api/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ username: user, password: pass })
            });

            const result = await response.json();
            if (result.status === "success") {
                document.getElementById('user-display').innerText = result.name;
                showScreen('menu');
            } else {
                alert("פרטי התחברות שגויים");
            }
        } catch {
            alert("שגיאה בחיבור לשרת");
        }
    };
}

// --- ניווט ---
document.getElementById('createNewBtn')
    ?.addEventListener('click', () => showScreen('creator'));
// --- ייבוא מקובץ JSON ---
document.getElementById('jsonFileInput')?.addEventListener('change', function(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const jsonData = JSON.parse(e.target.result);

            // בדיקה אם הקובץ הוא מערך או אובייקט בודד
            const newBoxes = Array.isArray(jsonData) ? jsonData : [jsonData];

            let addedCount = 0;
            newBoxes.forEach(box => {
                // בדיקה שכל המידות קיימות
                if (box.l && box.w && box.h && box.quantity) {
                    boxesToPack.push({
                        typeId: boxesToPack.length + 1,
                        l: parseFloat(box.l),
                        w: parseFloat(box.w),
                        h: parseFloat(box.h),
                        quantity: parseInt(box.quantity)
                    });
                    addedCount++;
                }
            });

            if (addedCount > 0) {
                updateTable();
                alert(`ייבוא הושלם: נוספו ${addedCount} סוגי ארגזים.`);
            } else {
                alert("לא נמצאו נתונים תקינים בקובץ. וודא שהשדות הם l, w, h, quantity.");
            }

            // איפוס שדה הקובץ כדי לאפשר בחירה חוזרת של אותו קובץ
            event.target.value = '';

        } catch (err) {
            console.error("JSON Error:", err);
            alert("שגיאה בקריאת הקובץ: וודא שזהו קובץ JSON תקין.");
        }
    };
    reader.readAsText(file);
});
// --- הוספת ארגז ---
document.getElementById('addBoxBtn').onclick = () => {
    const l = parseFloat(document.getElementById('newBoxL').value);
    const w = parseFloat(document.getElementById('newBoxW').value);
    const h = parseFloat(document.getElementById('newBoxH').value);
    const qty = parseInt(document.getElementById('newBoxQty').value);

    if (l && w && h && qty) {
        boxesToPack.push({
            typeId: boxesToPack.length + 1,
            l, w, h,
            quantity: qty
        });
        updateTable();
        document.getElementById('newBoxL').value = '';
        document.getElementById('newBoxW').value = '';
        document.getElementById('newBoxH').value = '';
        document.getElementById('newBoxQty').value = '';
    } else {
        alert("נא למלא את כל שדות הארגז");
    }
};

function updateTable() {
    const tbody = document.getElementById('boxesTableBody');
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

// --- חישוב ---
document.getElementById('calculateBtn').onclick = async () => {
    if (boxesToPack.length === 0)
        return alert("נא להוסיף ארגזים");

    const container = {
        l: parseFloat(document.getElementById('contL').value) || 1200,
        w: parseFloat(document.getElementById('contW').value) || 235,
        h: parseFloat(document.getElementById('contH').value) || 269
    };

    try {
        const response = await fetch('/api/solve', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ container, boxes: boxesToPack })
        });

        const result = await response.json();
        if (result && result.length > 0) {
            lastCalculatedData = result;
            showScreen('result');
            init3D(result, container);
        } else {
            alert("לא נמצא סידור אפשרי");
        }
    } catch {
        alert("שגיאה בחיבור לשרת");
    }
};
// function downloadVideo(lengthBox) {
//     const stream = renderer.domElement.captureStream(30);
//     const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
//     const chunks = [];
//
//     recorder.ondataavailable = e => chunks.push(e.data);
//     recorder.onstop = () => {
//         const blob = new Blob(chunks, { type: 'video/webm' });
//         const url = URL.createObjectURL(blob);
//         const a = document.createElement('a');
//         a.href = url;
//         a.download = 'packing_simulation.webm';
//         a.click();
//
//         // עדכון סטטוס כשההורדה מסתיימת
//         const statusDiv = document.getElementById('rec-status');
//         if(statusDiv) statusDiv.innerHTML = "✅ הקלטה נשמרה";
//     };
//
//     recorder.start();
//     console.log("הקלטה התחילה...");
//
//     // עוצר את ההקלטה לפי מספר הארגזים (שנייה לארגז + שנייה אקסטרה)
//     setTimeout(() => {
//         recorder.stop();
//     }, 300 * (lengthBox + 1));
// }
let finalVideoBlob = null; // משתנה שיחזיק את הסרטון המוכן

function downloadVideo(lengthBox) {
    const stream = renderer.domElement.captureStream(30);
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    const chunks = [];

    recorder.ondataavailable = e => chunks.push(e.data);

    recorder.onstop = () => {
        // במקום להוריד מיד, שומרים את ה-Blob במשתנה
        finalVideoBlob = new Blob(chunks, { type: 'video/webm' });

        // עדכון ממשק משתמש שהסרטון מוכן
        const downloadBtn = document.getElementById('saveVideoBtn');
        if (downloadBtn) {
            downloadBtn.disabled = false;
            downloadBtn.innerHTML = "שמור סרטון הדמיה 🎬";
            downloadBtn.style.background = "#e0787a"; // ירוק כשהוא מוכן
        }
        console.log("ההקלטה הסתיימה והסרטון מוכן להורדה.");
    };

    recorder.start();
    console.log("הקלטה אוטומטית התחילה...");

    // עצירה אוטומטית לאחר סיום האנימציה
    setTimeout(() => {
        recorder.stop();
    }, 300 * (lengthBox + 2)); // הוספנו עוד קצת זמן לביטחון
}

// פונקציה שתופעל בלחיצה על הכפתור
document.getElementById('saveVideoBtn').onclick = () => {
    if (!finalVideoBlob) {
        alert("הסרטון עדיין בתהליך הכנה או שלא נוצר.");
        return;
    }

    const url = URL.createObjectURL(finalVideoBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'packing_simulation.webm';
    a.click();
    URL.revokeObjectURL(url); // ניקוי זיכרון
};
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
            <tr><th>סוג</th><th>X</th><th>Y</th><th>Z</th></tr>
        `);

        lastCalculatedData.forEach(b => {
            win.document.write(`
                <tr>
                    <td>${b.cargoType}</td>
                    <td>${b.leftBackBottom[0]}</td>
                    <td>${b.leftBackBottom[1]}</td>
                    <td>${b.leftBackBottom[2]}</td>
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
let currentMeshBoxes = []; // מערך שיחזיק את כל הארגזים המוצגים כרגע
// פונקציה להרצת האנימציה בלבד
function runPackingAnimation(data) {
    // 1. ניקוי ארגזים קיימים מהסצנה
    currentMeshBoxes.forEach(obj => scene.remove(obj));
    currentMeshBoxes = [];

    // 2. הרצה מחדש של האנימציה
    data.forEach((box, index) => {
        setTimeout(() => {
            const l = parseFloat((box.rightFrontTop[0] - box.leftBackBottom[0]).toFixed(2));
            const w = parseFloat((box.rightFrontTop[1] - box.leftBackBottom[1]).toFixed(2));
            const h = parseFloat((box.rightFrontTop[2] - box.leftBackBottom[2]).toFixed(2));

            const geometry = new THREE.BoxGeometry(l, h, w);
            const material = new THREE.MeshPhongMaterial({
                color: new THREE.Color(`hsl(${box.cargoType * 137}, 70%, 50%)`),
                transparent: true,
                opacity: 0.9
            });

            const mesh = new THREE.Mesh(geometry, material);
            mesh.position.set(
                box.leftBackBottom[0] + l / 2,
                box.leftBackBottom[2] + h / 2,
                box.leftBackBottom[1] + w / 2
            );

            // הוספת קווי מתאר
            const edges = new THREE.EdgesGeometry(geometry);
            const wireframe = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0x000000 }));
            wireframe.position.copy(mesh.position);

            // הוספה לסצנה ולמערך הניקוי
            scene.add(mesh);
            scene.add(wireframe);
            currentMeshBoxes.push(mesh, wireframe);

        }, index * 300);
    });
}

// הצמדת אירוע לכפתור ה-Replay
document.getElementById('replayBtn')?.addEventListener('click', () => {
    if (lastCalculatedData && lastCalculatedData.length > 0) {
        runPackingAnimation(lastCalculatedData);
    }
});
// --- תלת־ממד ---
function init3D(data, containerDim) {
    console.log("--- דוח ארגזים מהשרת ---");

    // 1. ניקוי וחישוב נתונים
    if (!data || data.length === 0) {
        console.error("שגיאה: הרשימה שחזרה מהשרת ריקה!");
    } else {
        const debugData = data.map((box, i) => ({
            "ID": box.cargoType,
            "אורך (L)": (box.rightFrontTop[0] - box.leftBackBottom[0]).toFixed(2),
            "רוחב (W)": (box.rightFrontTop[1] - box.leftBackBottom[1]).toFixed(2),
            "גובה (H)": (box.rightFrontTop[2] - box.leftBackBottom[2]).toFixed(2),
            "שמאל X": box.leftBackBottom[0],
            "שמאל Y": box.leftBackBottom[1],
            "שמאל Z": box.leftBackBottom[2],
            "ימין X": box.rightFrontTop[0],
            "ימיו Y": box.rightFrontTop[1],
            "ימין Z": box.rightFrontTop[2]

        }));
        console.table(debugData);


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
        camera.position.set(containerDim.l * 1.5, containerDim.h * 1.5, containerDim.w * 1.5);

        renderer = new THREE.WebGLRenderer({antialias: true, preserveDrawingBuffer: true});
        renderer.setSize(window.innerWidth, window.innerHeight);
        document.body.appendChild(renderer.domElement);
        controls = new OrbitControls(camera, renderer.domElement);
        scene.add(new THREE.AmbientLight(0xffffff, 0.7));

        // 2. ציור המכולה
        const containerGeo = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
        const containerMesh = new THREE.Mesh(containerGeo, new THREE.MeshBasicMaterial({
            color: 0xE0787AFF,
            wireframe: true
        }));
        containerMesh.position.set(containerDim.l / 2, containerDim.h / 2, containerDim.w / 2);
        scene.add(containerMesh);

        // 3. הפעלת הקלטה אוטומטית
        // אנחנו קוראים לזה רגע לפני תחילת האנימציה
        downloadVideo(data.length);
        runPackingAnimation(data);
        // 4. אנימציית טעינה (כל ארגז מופיע אחרי שנייה אחת)
         // 1000ms = 1 second

    }

    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }
    animate();
}
