import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// ─── משתנים גלובליים ל-3D ───
let scene, camera, renderer, controls;
let currentMeshBoxes = [];

// ─── אתחול ויזואליזציה 3D ───
export function init3D(data, containerDim) {
    // הסרת לוח הסטטיסטיקות הישן לפני יצירת חדש
    const oldStats = document.getElementById('stats-panel');
    if (oldStats) oldStats.remove();

    // חישוב ניצולת
    const containerVolume = containerDim.l * containerDim.w * containerDim.h;
    let packedVolume = 0;
    data.forEach(box => {
        const l = Math.abs(box.rightFrontTop[0] - box.leftBackBottom[0]);
        const w = Math.abs(box.rightFrontTop[1] - box.leftBackBottom[1]);
        const h = Math.abs(box.rightFrontTop[2] - box.leftBackBottom[2]);
        packedVolume += (l * w * h);
    });
    const efficiency = ((packedVolume / containerVolume) * 100).toFixed(2);

    // יצירת לוח סטטיסטיקות
    const statsDiv = document.createElement('div');
    statsDiv.id = 'stats-panel';
    statsDiv.style = "position:absolute; top:10px; left:10px; background:rgba(0,0,0,0.8); color:white; padding:15px; border-radius:8px; z-index:100; font-family:Arial;";
    statsDiv.innerHTML = `
        <h3 style="margin:0 0 10px 0">סיכום טעינה</h3>
        <p>מכולה: ${containerDim.l}×${containerDim.w}×${containerDim.h}</p>
        <p>ניצולת: <strong style="color:#e0787a">${efficiency}%</strong></p>
    `;
    document.body.appendChild(statsDiv);

    // מחיקת renderer ישן אם קיים
    if (renderer) {
        document.body.removeChild(renderer.domElement);
    }

    // אתחול Scene
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x1a1a1a);

    // אתחול Camera
    camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(
        containerDim.l * 1.5,
        containerDim.h * 1.5,
        containerDim.w * 1.5
    );

    // אתחול Renderer
    renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);

    // אתחול Controls
    controls = new OrbitControls(camera, renderer.domElement);

    // תאורה
    scene.add(new THREE.AmbientLight(0xffffff, 0.7));
    const light = new THREE.DirectionalLight(0xffffff, 0.5);
    light.position.set(1, 1, 1);
    scene.add(light);

    // ציור המכולה (Wireframe)
    const containerGeo = new THREE.BoxGeometry(containerDim.l, containerDim.h, containerDim.w);
    const containerMesh = new THREE.Mesh(
        containerGeo,
        new THREE.MeshBasicMaterial({ color: 0xE0787A, wireframe: true })
    );
    containerMesh.position.set(containerDim.l / 2, containerDim.h / 2, containerDim.w / 2);
    scene.add(containerMesh);

    // הרצת האנימציה
    runPackingAnimation(data);

    // לולאת ה-render
    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }
    animate();

    return { scene, camera, renderer, controls };
}

// ─── אנימציה של הארגזים ───
export function runPackingAnimation(data) {
    // הסרת ארגזים ישנים
    currentMeshBoxes.forEach(obj => scene.remove(obj));
    currentMeshBoxes = [];

    // הוסיף כל ארגז כשהזמן עובר
    data.forEach((box, index) => {
        setTimeout(() => {
            const l = parseFloat((box.rightFrontTop[0] - box.leftBackBottom[0]).toFixed(2));
            const w = parseFloat((box.rightFrontTop[1] - box.leftBackBottom[1]).toFixed(2));
            const h = parseFloat((box.rightFrontTop[2] - box.leftBackBottom[2]).toFixed(2));

            // יצירת גיאומטריה
            const geometry = new THREE.BoxGeometry(l, h, w);
            
            // יצירת מטריאל עם צבע ייחודי בהתאם לסוג
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

            // הוספת wireframe שחור
            const edges = new THREE.EdgesGeometry(geometry);
            const wireframe = new THREE.LineSegments(
                edges,
                new THREE.LineBasicMaterial({ color: 0x000000 })
            );
            wireframe.position.copy(mesh.position);

            scene.add(mesh, wireframe);
            currentMeshBoxes.push(mesh, wireframe);
        }, index * 300);
    });
}

// ─── רישום וידאו של האנימציה ───
export function startVideoRecording(boxCount) {
    const stream = renderer.domElement.captureStream(30);
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    const chunks = [];

    recorder.ondataavailable = e => chunks.push(e.data);

    const promise = new Promise(resolve => {
        recorder.onstop = () => {
            const blob = new Blob(chunks, { type: 'video/webm' });
            resolve(blob);
        };
    });

    recorder.start();
    
    // עצור רישום לאחר משך זמן מתאים
    setTimeout(() => recorder.stop(), 300 * (boxCount + 2));

    return promise;
}
