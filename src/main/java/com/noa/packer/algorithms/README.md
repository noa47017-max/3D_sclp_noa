# 📂 Algorithms - Architecture Overview

## 🏗️ מבנה ארגון השכבות

קובצי algorithms מחולקים לקטגוריות הגיוניות:

### 1️⃣ **Core Algorithm** (נקודת כניסה ראשית)
```
HybridLoadingSystem.java
├── startLoadingProcess()         → נקודת כניסה
├── executeIteration()            → הרץ M איטרציות
├── reinforcePheromoneFromSolution()  → חיזוק פרומונים
└── updateGlobalBestIfNeeded()    → עדכון הטוב ביותר
```

---

### 2️⃣ **Ant Loading Engine** (בניית פתרון יחיד)
```
AntLoadingEngine.java
└── executeAntLoading()           → בונה פתרון אחד
    ├── selectCargo()             → בחירת ארגז
    ├── selectOrientation()       → בחירת כיוון
    └── updateSpaces()            → עדכון חללים
```

---

### 3️⃣ **Strategies** (אלגוריתמי בחירה)
```
GSMStrategy.java                  → Greedy (בחר לפי נפח)
├── selectCargo()                 → בחר קופסה עם נפח מקסימלי
└── selectCargo7()                → בחר לפי base area

ASMStrategy.java                  → ACO (בחר לפי פרומונים)
├── selectCargo()                 → בחר לפי משקל τ^α × η^β
└── rouletteWheelSelection()      → בחירה הסתברותית
```

---

### 4️⃣ **Pheromone Management** (זיכרון נמלים)
```
PheromoneManager.java             → ניהול פרומונים
├── getPheromone()                → קרא פרומון
├── updateGlobalPheromone()       → עדכן גלובלי
├── applyLocalEvaporation()       → אידוי בזמן טעינה
├── applyGlobalEvaporation()      → אידוי אחרי איטרציה
└── generateStateWord()            → קוד המצב (VU/VR/VI)

PheromoneNode.java                → יחידה של פרומון
└── תיעוד של בחירה מוצלחת
```

---

### 5️⃣ **Space Management** (ניהול חללים)
```
SpaceManager.java                 → ניהול חללים
├── splitIntoResiduals()          → פיצול (Right/Front/Top)
├── mergeSpaces()                 → מיזוג חללים צמודים
└── cleanAndOptimizeSpaces()      → הסרת חללים קטנים

Space.java                        → מבנה חלל
└── (x, y, z, L, W, H): קואורדינטה וממדים
```

---

### 6️⃣ **Orientation & Placement** (בחירת כיוונים)
```
OrientationOptimizer.java         → בחירת כיוון אופטימלי
├── getBestOrientation()          → בחר לפי waste מינימלי
└── getBestOrientation7()         → גרסה 7 עם מטריקות חלופיות

Orientation.java                  → כיוון ארגז
└── (id, l, w, h): מפתח וממדים
```

---

### 7️⃣ **Data Models** (מבנים נתונים)
```
CargoType.java                    → סוג ארגז
├── id, l, w, h                   → מפתח וממדים
├── quantity                       → כמות זמינה
└── canFit()                       → בדוק התאמה גיאומטרית

LoadingChain.java                 → שרשרת טעינה (פתרון)
├── addStep()                      → הוסף ארגז
├── getVUR()                       → חשב יעילות
└── getSteps()                     → פרטי כל הנחה

Box.java                          → ארגז יחיד בפתרון
└── (id, ori, pos): מפתח כיוון מיקום

Container.java                    → מכולה
└── (length, width, height): ממדים
```

---

### 8️⃣ **Utilities** (עזרים גלובליים)
```
Global.java                       → קבועים גלובליים
├── M = 100                        → מספר איטרציות
├── m = 30                         → מספר נמלים
└── MIN_DIM                        → ממד מינימלי

StrategySelector.java             → בוחר GSM או ASM
└── shouldRunGreedy()             → בהסתברות P(τ)
```

---

## 🔄 Flow Diagram

```
HybridLoadingSystem
  ↓
  ├─→ iteration 1..M
  │     ├─→ ant 1..m
  │     │   ├─→ AntLoadingEngine.executeAntLoading()
  │     │   │     ├─→ selectCargo()    [GSM או ASM]
  │     │   │     ├─→ selectOrientation() [OrientationOptimizer]
  │     │   │     └─→ updateSpaces()   [SpaceManager]
  │     │   └─→ LoadingChain (פתרון)
  │     │
  │     ├─→ reinforcePheromone()
  │     └─→ updateGlobalBest()
  │
  └─→ output results
```

---

## 📋 Checklist לקריאה הקוד

- [ ] קרא `HybridLoadingSystem` - הבן לולאת ACO
- [ ] קרא `AntLoadingEngine` - הבן בניית פתרון יחיד
- [ ] קרא `GSMStrategy` + `ASMStrategy` - הבן בחירה
- [ ] קרא `SpaceManager` - הבן ניהול חללים (VU/VR/VI)
- [ ] קרא `PheromoneManager` - הבן זיכרון נמלים
- [ ] קרא `OrientationOptimizer` - הבן בחירת כיוונים

---

## 🚀 Quick Start

1. הרץ `HybridLoadingSystem.startLoadingProcess(container, cargoTypes)`
2. המערכת תרוץ M איטרציות של m נמלים
3. כל נמלה תבנה פתרון באמצעות `AntLoadingEngine`
4. הפתרונות הטובים ים לאדום פרומונים
5. בסוף - הפתרון הטוב ביותר

---

## ✅ Files Structure

```
algorithms/
├── Core Algorithm
│   └── HybridLoadingSystem.java
│
├── Ant Engine
│   └── AntLoadingEngine.java
│
├── Strategies
│   ├── GSMStrategy.java
│   └── ASMStrategy.java
│
├── Pheromone
│   ├── PheromoneManager.java
│   └── PheromoneNode.java
│
├── Space Management
│   ├── SpaceManager.java
│   └── Space.java
│
├── Orientation
│   ├── OrientationOptimizer.java
│   └── Orientation.java
│
├── Data Models
│   ├── CargoType.java
│   ├── LoadingChain.java
│   ├── Box.java
│   └── Container.java
│
└── Utilities
    ├── Global.java
    ├── StrategySelector.java
    ├── PlacedBox.java
    └── PruningManager.java
```


