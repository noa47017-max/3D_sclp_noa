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

function init3D(data, containerDim) {
    if (renderer) {
        document.body.removeChild(renderer.domElement);
    }

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x111111);

    // מצלמה במיקום רחוק יותר כדי לראות את כל המכולה
    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(containerDim.l * 1.5, containerDim.h * 1.5, containerDim.w * 1.5);

    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);

    controls = new OrbitControls(camera, renderer.domElement);

    // תאורה חזקה יותר
    scene.add(new THREE.AmbientLight(0xffffff, 0.5));
    const light = new THREE.PointLight(0xffffff, 1);
    light.position.set(containerDim.l, containerDim.h * 2, containerDim.w);
    scene.add(light);

    // ציור המכולה (Wireframe)
    const geo = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
    const mesh = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ color: 0x00ff00, wireframe: true }));
    // הצבה כך שפינה אחת ב-(0,0,0)
    mesh.position.set(containerDim.l / 2, containerDim.h / 2, containerDim.w / 2);
    scene.add(mesh);

    // ציור הארגזים - בדיקה אם הנתונים הגיעו
    console.log("Data received from server:", data);

    if (data && data.length > 0) {
        data.forEach(box => {
            // הגנה: בודק גם אותיות גדולות וגם קטנות
            const xb = box.xB !== undefined ? box.xB : box.xb;
            const xt = box.xT !== undefined ? box.xT : box.xt;
            const yb = box.yB !== undefined ? box.yB : box.yb;
            const yt = box.yT !== undefined ? box.yT : box.yt;
            const zb = box.zB !== undefined ? box.zB : box.zb;
            const zt = box.zT !== undefined ? box.zT : box.zt;

            const l = xt - xb;
            const h = zt - zb;
            const w = yt - yb;

            console.log(`Drawing box: ${l}x${h}x${w} at ${xb},${zb},${yb}`); // בדיקה

            if (l > 0 && h > 0 && w > 0) {
                const bMesh = new THREE.Mesh(
                    new THREE.BoxGeometry(l, h, w),
                    new THREE.MeshPhongMaterial({
                        color: new THREE.Color(`hsl(${(box.typeId || 1) * 40}, 70%, 50%)`),
                        transparent: false,
                        opacity: 1
                    })
                );
                bMesh.position.set(xb + l/2, zb + h/2, yb + w/2);
                scene.add(bMesh);
            }
        });
    } else {
        console.warn("No boxes received to display!");
    }

    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }
    animate();
}
