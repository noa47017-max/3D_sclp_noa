import { sanitize } from '../utils/utils.js';
import { API_BASE_URL, PASSWORD_MIN_LENGTH } from '../config/constants.js';
import { authenticatedFetch } from './auth.js';

// ─── רשימת ארגזים מקומית ───
export let boxesToPack = [];

// ─── הוספת סוג ארגז חדש ───
export function addBoxType(length, width, height, quantity, rotationOption) {
    if (!length || !width || !height || !quantity) {
        alert('כל השדות חייבים להיות מלאים');
        return false;
    }

    const numL = parseFloat(length);
    const numW = parseFloat(width);
    const numH = parseFloat(height);
    const numQ = parseInt(quantity);

    if (numL <= 0 || numW <= 0 || numH <= 0 || numQ <= 0) {
        alert('כל הערכים חייבים להיות חיוביים');
        return false;
    }

    boxesToPack.push({
        length: numL,
        width: numW,
        height: numH,
        quantity: numQ,
        rotationOption: rotationOption || '6',
        id: Math.random().toString(36).substring(7)
    });

    return true;
}

// ─── מחיקת סוג ארגז ───
export function removeBoxType(boxId) {
    boxesToPack = boxesToPack.filter(b => b.id !== boxId);
}

// ─── ניקוי רשימת ארגזים ───
export function clearBoxes() {
    boxesToPack = [];
}

// ─── עדכון טבלת הארגזים ב-DOM ───
export function updateBoxesTable() {
    const tbody = document.getElementById('boxesTableBody');
    if (!tbody) return;

    tbody.innerHTML = '';

    boxesToPack.forEach((box, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${box.length}</td>
            <td>${box.width}</td>
            <td>${box.height}</td>
            <td>${box.quantity}</td>
            <td>${getRotationLabel(box.rotationOption)}</td>
            <td><button onclick="removeBoxRow(${index})" style="background:#ff6b6b; padding:4px 8px; width:auto;">מחק</button></td>
        `;
        tbody.appendChild(row);
    });
}

// ─── תרגום rotationOption לטקסט ───
function getRotationLabel(rotOpt) {
    const rotations = {
        '6': '6-כיווני',
        '2': '2-כיווני',
        '1': '1-כיווני'
    };
    return rotations[rotOpt] || rotOpt;
}

// ─── פונקציה גלובלית להסרת שורה מהטבלה (קרויה מ-onclick) ───
window.removeBoxRow = function(index) {
    if (boxesToPack[index]) {
        removeBoxType(boxesToPack[index].id);
        updateBoxesTable();
    }
};

// ─── קריאה ל-API לביצוע חישוב טעינה ───
export async function calculatePacking(containerLength, containerWidth, containerHeight) {
    if (boxesToPack.length === 0) {
        alert('הוסף לפחות סוג ארגז אחד');
        return null;
    }

    const payload = {
        containerLength: parseFloat(containerLength),
        containerWidth: parseFloat(containerWidth),
        containerHeight: parseFloat(containerHeight),
        cargoTypes: boxesToPack.map(b => ({
            length: b.length,
            width: b.width,
            height: b.height,
            quantity: b.quantity,
            rotationOption: parseInt(b.rotationOption)
        }))
    };

    try {
        const response = await authenticatedFetch(`${API_BASE_URL}/packing/calculate`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorData = await response.json();
            alert(`שגיאה בחישוב: ${errorData.message || 'שגיאה לא ידועה'}`);
            return null;
        }

        return await response.json();
    } catch (err) {
        alert(`שגיאת רשת: ${err.message}`);
        return null;
    }
}

// ─── ייבוא ארגזים מקובץ JSON/TXT ───
export async function importBoxesFromFile(file) {
    return new Promise((resolve) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const content = e.target.result;
                const boxes = JSON.parse(content);

                if (!Array.isArray(boxes)) {
                    alert('פורמט קובץ לא תקין. צפה מערך של ארגזים');
                    resolve(false);
                    return;
                }

                clearBoxes();
                boxes.forEach(box => {
                    addBoxType(
                        box.length || box.L || box.l,
                        box.width || box.W || box.w,
                        box.height || box.H || box.h,
                        box.quantity || box.qty || box.Q,
                        box.rotationOption || '6'
                    );
                });

                updateBoxesTable();
                alert(`נטען בהצלחה ${boxes.length} סוגי ארגזים`);
                resolve(true);
            } catch (err) {
                alert(`שגיאה בקריאת הקובץ: ${err.message}`);
                resolve(false);
            }
        };
        reader.readAsText(file);
    });
}
