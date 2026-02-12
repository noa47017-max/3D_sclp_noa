import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// --- ניהול תצוגה ---
const screens = {
    login: document.getElementById('login-screen'),
    menu: document.getElementById('menu-screen'),
    creator: document.getElementById('creator-screen')
};

function showScreen(screenName) {
    Object.values(screens).forEach(s => {
        if (s) s.style.display = 'none';
    });
    if (screens[screenName]) screens[screenName].style.display = 'flex';
}

// --- אירועי כפתורים ---

// התחברות
document.getElementById('loginBtn').onclick = async () => {
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username: user, password: pass})
        });
        const result = await response.json();
        if (result.status === "success") {
            document.getElementById('user-display').innerText = result.name;
            showScreen('menu');
        } else {
            alert("פרטי התחברות שגויים");
        }
    } catch (e) { alert("שגיאה בחיבור לשרת"); }
};

document.getElementById('createNewBtn').onclick = () => showScreen('creator');
// document.getElementById('backBtn').onclick = () => showScreen('menu');
document.getElementById('closeVizBtn').onclick = () => {
    document.getElementById('closeVizBtn').style.display = 'none';
    if (renderer) renderer.domElement.style.display = 'none';
    showScreen('menu');
};
let boxesToPack = [];

// פונקציה להוספת ארגז לרשימה
document.getElementById('addBoxBtn').onclick = () => {
    const l = parseFloat(document.getElementById('newBoxL').value);
    const w = parseFloat(document.getElementById('newBoxW').value);
    const h = parseFloat(document.getElementById('newBoxH').value);
    const qty = parseInt(document.getElementById('newBoxQty').value);

    if (l && w && h && qty) {
        const newBox = { typeId: boxesToPack.length + 1, l, w, h, quantity: qty };
        boxesToPack.push(newBox);
        updateTable();
        // ניקוי השדות
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
    const container = document.getElementById('tableContainer') || tbody.parentElement.parentElement;
    if (container) {
        container.scrollTop = container.scrollHeight;
}

// פונקציה למחיקת ארגז מהרשימה
window.removeBox = (index) => {
    boxesToPack.splice(index, 1);
    updateTable();
};
// --- לוגיקת החישוב (כאן היה הכפל) ---
document.getElementById('calculateBtn').onclick = async () => {
    if (boxesToPack.length === 0) {
        alert("נא להוסיף לפחות ארגז אחד לרשימה");
        return;
    }

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
            showScreen('none');
            document.getElementById('closeVizBtn').style.display = 'block';
            init3D(result, container);
        } else {
            alert("לא נמצא סידור אפשרי לארגזים אלו.");
        }
    } catch (e) {
        console.error(e);
        alert("שגיאה בחיבור לשרת Java");
    }
};

// --- Three.js ---
let scene, camera, renderer, controls;
function downloadVideo(lengthBox) {
        const stream = renderer.domElement.captureStream(30);
        const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
        const chunks = [];

        recorder.ondataavailable = e => chunks.push(e.data);
        recorder.onstop = () => {
            const blob = new Blob(chunks, { type: 'video/webm' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'packing_simulation.webm';
            a.click();
            console.log("ההקלטה הסתיימה והורדה.");
        };

        recorder.start();
        console.log("הקלטה התחילה...");

        // עוצר את ההקלטה שנייה אחרי שהארגז האחרון נטען
        setTimeout(() => {
            recorder.stop();
        }, 300 * (lengthBox + 1));
}

function init3D(data, containerDim) {
    console.log("--- דוח ארגזים מהשרת ---");

    // 1. ניקוי וחישוב נתונים
    if (!data || data.length === 0) {
        console.error("שגיאה: הרשימה שחזרה מהשרת ריקה!");
    }else {
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
    statsDiv.innerHTML = `
        <h3 style="margin:0 0 10px 0">סיכום טעינה</h3>
        <p>מכולה: ${containerDim.l}x${containerDim.w}x${containerDim.h}</p>
        <p>נצילות: <strong style="color:#00ff00">${efficiency}%</strong></p>
    `;
    document.body.appendChild(statsDiv);

    if (renderer) document.body.removeChild(renderer.domElement);
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x111111);
    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(containerDim.l * 1.5, containerDim.h * 1.5, containerDim.w * 1.5);

    renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);
    controls = new OrbitControls(camera, renderer.domElement);
    scene.add(new THREE.AmbientLight(0xffffff, 0.7));

    // 2. ציור המכולה
    const containerGeo = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
    const containerMesh = new THREE.Mesh(containerGeo, new THREE.MeshBasicMaterial({ color: 0x00ff00, wireframe: true }));
    containerMesh.position.set(containerDim.l / 2, containerDim.h / 2, containerDim.w / 2);
    scene.add(containerMesh);

    // 3. הפעלת הקלטה אוטומטית
    // אנחנו קוראים לזה רגע לפני תחילת האנימציה
    downloadVideo(data.length);

    // 4. אנימציית טעינה (כל ארגז מופיע אחרי שנייה אחת)
    data.forEach((box, index) => {
        setTimeout(() => {
            const l = parseFloat((box.rightFrontTop[0] - box.leftBackBottom[0]).toFixed(2));
            const w = parseFloat((box.rightFrontTop[1] - box.leftBackBottom[1]).toFixed(2));
            const h = parseFloat((box.rightFrontTop[2] - box.leftBackBottom[2]).toFixed(2));

            const geometry = new THREE.BoxGeometry(l, h, w);
            const mesh = new THREE.Mesh(geometry, new THREE.MeshPhongMaterial({
                color: new THREE.Color(`hsl(${box.cargoType * 137}, 70%, 50%)`),
                transparent: true, opacity: 0.9
            }));

            mesh.position.set(box.leftBackBottom[0] + l/2, box.leftBackBottom[2] + h/2, box.leftBackBottom[1] + w/2);
            scene.add(mesh);

            // הוספת קווי מתאר וספרות (כפי שהיה בקוד המקורי שלך)
            const edges = new THREE.EdgesGeometry(geometry);
            const wireframe = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0x000000 }));
            wireframe.position.copy(mesh.position);
            scene.add(wireframe);


        }, index * 300); // 1000ms = 1 second
    });
       }
    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }
    animate();
}}

