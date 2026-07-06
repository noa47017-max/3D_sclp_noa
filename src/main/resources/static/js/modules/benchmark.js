import { API_BASE_URL } from '../config/constants.js';
import { authenticatedFetch } from './auth.js';

// ─── פרסום קובץ בנצ'מארק ─ BR1-BR7 ───
export function parseBenchmarkFile(content) {
    const lines = content.trim().split('\n');
    const cases = [];

    let currentCase = null;
    let boxCount = 0;

    for (const line of lines) {
        const trimmed = line.trim();

        if (trimmed === '') {
            if (currentCase) {
                cases.push(currentCase);
                currentCase = null;
            }
            continue;
        }

        const parts = trimmed.split(/\s+/);

        // שורה ראשונה: מקרה חדש
        if (parts.length >= 3 && !isNaN(parts[0]) && !isNaN(parts[1]) && !isNaN(parts[2])) {
            if (currentCase) {
                cases.push(currentCase);
            }

            const [caseNum, contL, contW, contH, ...rest] = parts.map(Number);
            boxCount = rest[0] || 0;

            currentCase = {
                caseNum,
                containerLength: contL,
                containerWidth: contW,
                containerHeight: contH,
                boxes: []
            };
        } else if (currentCase && parts.length >= 4) {
            const [L, W, H, qty, ...rest] = parts.map(Number);
            if (!isNaN(L) && !isNaN(W) && !isNaN(H) && !isNaN(qty)) {
                currentCase.boxes.push({
                    length: L,
                    width: W,
                    height: H,
                    quantity: qty,
                    rotationOption: 6
                });
            }
        }
    }

    if (currentCase) {
        cases.push(currentCase);
    }

    return cases;
}

// ─── הרצת מקרה בנצ'מארק אחד ───
export async function runBenchmarkCase(testCase) {
    const payload = {
        containerLength: testCase.containerLength,
        containerWidth: testCase.containerWidth,
        containerHeight: testCase.containerHeight,
        cargoTypes: testCase.boxes
    };

    const startTime = performance.now();

    const response = await authenticatedFetch(`${API_BASE_URL}/packing/calculate`, {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        throw new Error('בעיה בחישוב');
    }

    const data = await response.json();
    const endTime = performance.now();

    // חישוב ניצולת
    const containerVolume = testCase.containerLength * testCase.containerWidth * testCase.containerHeight;
    let packedVolume = 0;

    if (data && Array.isArray(data)) {
        data.forEach(box => {
            const l = Math.abs(box.rightFrontTop[0] - box.leftBackBottom[0]);
            const w = Math.abs(box.rightFrontTop[1] - box.leftBackBottom[1]);
            const h = Math.abs(box.rightFrontTop[2] - box.leftBackBottom[2]);
            packedVolume += (l * w * h);
        });
    }

    const utilization = ((packedVolume / containerVolume) * 100).toFixed(2);
    const time = Math.round(endTime - startTime);

    return {
        utilization: parseFloat(utilization),
        time,
        packedBoxCount: data ? data.length : 0
    };
}

// ─── הרצת כל מקרי בנצ'מארק ───
export async function runAllBenchmarkCases(testCases, onProgress) {
    const results = {
        passed: 0,
        failed: 0,
        times: [],
        utils: []
    };

    for (let i = 0; i < testCases.length; i++) {
        const tc = testCases[i];
        try {
            const result = await runBenchmarkCase(tc);
            results.passed++;
            results.times.push(result.time);
            results.utils.push(result.utilization);

            if (onProgress) {
                onProgress({
                    type: 'success',
                    caseNum: tc.caseNum,
                    util: result.utilization,
                    time: result.time
                });
            }
        } catch (err) {
            results.failed++;
            if (onProgress) {
                onProgress({
                    type: 'error',
                    caseNum: tc.caseNum,
                    error: err.message
                });
            }
        }
    }

    const avgTime = results.times.length ? Math.round(results.times.reduce((a, b) => a + b, 0) / results.times.length) : 0;
    const avgUtil = results.utils.length ? (results.utils.reduce((a, b) => a + b, 0) / results.utils.length).toFixed(1) : '—';

    return {
        ...results,
        summary: {
            avgTime,
            avgUtil
        }
    };
}
