// ─── הדפסת טבלה ───
export function printTable(data) {
    if (data.length === 0) {
        alert('אין נתונים להדפסה');
        return;
    }

    const win = window.open('', '', 'width=800,height=600');
    win.document.write(`
        <html>
            <head>
                <style>
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        direction: rtl;
                    }
                    th, td {
                        border: 1px solid #2f3e46;
                        padding: 8px;
                        text-align: center;
                    }
                    th {
                        background-color: #2f3e46;
                        color: white;
                    }
                </style>
            </head>
            <body>
                <h2>דוח טעינה</h2>
                <table>
                    <tr>
                        <th>סוג</th>
                        <th>שמאל תחתון X</th>
                        <th>שמאל תחתון Y</th>
                        <th>שמאל תחתון Z</th>
                        <th>ימין עליון X</th>
                        <th>ימין עליון Y</th>
                        <th>ימין עליון Z</th>
                        <th>סיבוב</th>
                    </tr>
    `);

    data.forEach(b => {
        win.document.write(`
            <tr>
                <td>${b.cargoType}</td>
                <td>${b.leftBackBottom[0]}</td>
                <td>${b.leftBackBottom[1]}</td>
                <td>${b.leftBackBottom[2]}</td>
                <td>${b.rightFrontTop[0]}</td>
                <td>${b.rightFrontTop[1]}</td>
                <td>${b.rightFrontTop[2]}</td>
                <td>${b.direction}</td>
            </tr>
        `);
    });

    win.document.write('</table></body></html>');
    win.document.close();
    win.print();
}

// ─── הורדת CSV ───
export function downloadCSV(data) {
    if (data.length === 0) {
        alert('אין נתונים להורדה');
        return;
    }

    let csv = "\uFEFFסוג,X_התחלה,Y_התחלה,Z_התחלה,X_סוף,Y_סוף,Z_סוף\n";
    
    data.forEach(b => {
        csv += `${b.cargoType},${b.leftBackBottom.join(',')},${b.rightFrontTop.join(',')}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'packing_report.csv';
    link.click();
}

// ─── שמירת וידאו ───
export function saveVideo(blob) {
    if (!blob) {
        alert('הסרטון עדיין בתהליך הכנה...');
        return;
    }

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'packing_simulation.webm';
    a.click();
    URL.revokeObjectURL(url);
}

// ─── ניהול מצב כפתור שמירת וידאו ───
export function updateVideoButtonState(isReady, blob = null) {
    const saveBtn = document.getElementById('saveVideoBtn');
    if (!saveBtn) return;

    if (isReady) {
        saveBtn.disabled = false;
        saveBtn.innerHTML = 'שמור סרטון הדמיה 🎬';
        saveBtn.style.background = '#4caf50';
        saveBtn.onclick = () => saveVideo(blob);
    } else {
        saveBtn.disabled = true;
        saveBtn.innerHTML = 'מכין סרטון... 🎥';
        saveBtn.style.background = '#666';
    }
}
