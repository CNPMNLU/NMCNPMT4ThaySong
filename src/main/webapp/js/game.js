// game.js — Battleship with 20s turn timer & PvP support

const COLS = ['A','B','C','D','E','F','G','H','I','J'];
const ROWS = ['1','2','3','4','5','6','7','8','9','10'];

let gameOver = false;
let myTurn = true;
let contextPath = '';
let gameMode = 'PvE';

// === TIMER ===
let turnTimer = null;
let turnTimeLeft = 20;
const TURN_SECONDS = 20;

function startTimer(onTimeout) {
    clearTimer();
    turnTimeLeft = TURN_SECONDS;
    updateTimerUI(turnTimeLeft);
    turnTimer = setInterval(() => {
        turnTimeLeft--;
        updateTimerUI(turnTimeLeft);
        if (turnTimeLeft <= 0) {
            clearTimer();
            onTimeout();
        }
    }, 1000);
}

function clearTimer() {
    if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
    updateTimerUI(null);
}

function updateTimerUI(seconds) {
    const el = document.getElementById('turn-timer');
    if (!el) return;
    if (seconds === null) { el.textContent = ''; el.className = 'turn-timer'; return; }
    el.textContent = seconds + 's';
    el.className = 'turn-timer' + (seconds <= 5 ? ' danger' : seconds <= 10 ? ' warning' : '');
}

function init(ctx, mode) {
    contextPath = ctx;
    gameMode = mode || 'PvE';
    if (!gameOver && gameMode === 'PvE') {
        startTimer(onPlayerTimeout);
    }
}

// ===== PvE TIMEOUT =====
function onPlayerTimeout() {
    addLog('⏰ Hết giờ! Bỏ lượt.', 'timeout', null);
    myTurn = false;
    setTurnUI(false);
    fetch(contextPath + '/game', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'action=skip'
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) { myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout); return; }
        if (data.aiMove) {
            setTimeout(() => {
                const am = data.aiMove;
                applyResult(am.result, am.x, am.y, 'my');
                addLog('AI bắn ' + COLS[am.x] + ROWS[am.y] + ': ' + resultText(am.result), 'ai', am.result);
                if (am.result === 'GAME_OVER' || data.aiWon) {
                    showWinModal('💀 Bạn thua!', 'AI đã đánh chìm hết thuyền của bạn.', false);
                    gameOver = true; return;
                }
                myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout);
            }, 800);
        } else {
            myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout);
        }
    })
    .catch(() => { myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout); });
}

// ===== FIRE SHOT PvE =====
function fireShot(x, y) {
    if (gameOver || !myTurn || gameMode === 'PvP') return;
    const cell = document.getElementById('enemy-' + x + '-' + y);
    if (!cell || cell.classList.contains('hit-ship') || cell.classList.contains('miss') || cell.classList.contains('sunk-ship')) return;

    clearTimer();
    myTurn = false;
    setTurnUI(false);

    fetch(contextPath + '/game', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'x=' + x + '&y=' + y
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) { alert('Lỗi: ' + data.error); myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout); return; }
        applyResult(data.result, x, y, 'enemy');
        addLog('Bạn bắn ' + COLS[x] + ROWS[y] + ': ' + resultText(data.result), 'player', data.result);
        if (data.result === 'GAME_OVER') {
            showWinModal('🎉 Bạn thắng!', 'Điểm: ' + (data.score || 0), true);
            gameOver = true; return;
        }
        if (data.aiMove) {
            setTimeout(() => {
                const am = data.aiMove;
                applyResult(am.result, am.x, am.y, 'my');
                addLog('AI bắn ' + COLS[am.x] + ROWS[am.y] + ': ' + resultText(am.result), 'ai', am.result);
                if (am.result === 'GAME_OVER' || data.aiWon) {
                    showWinModal('💀 Bạn thua!', 'AI đã đánh chìm hết thuyền của bạn.', false);
                    gameOver = true; return;
                }
                myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout);
            }, 800);
        } else {
            myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout);
        }
    })
    .catch(e => { console.error(e); myTurn = true; setTurnUI(true); startTimer(onPlayerTimeout); });
}

// ===== PvP LOGIC =====
const SHIP_DEFS = [
    {type:'Carrier',length:5},{type:'Battleship',length:4},
    {type:'Cruiser',length:3},{type:'Submarine',length:3},{type:'Destroyer',length:2}
];

let pvpState = {
    currentPlayer: 1,
    board1: null, board2: null,
    ships1: null, ships2: null,
    name1: 'Người chơi 1', name2: 'Người chơi 2',
    totalShots: 0,
    startTime: null
};
let pvpPhase = 'setup2';
let pvpSetupBoard2 = Array.from({length:10}, () => Array(10).fill(null));
let pvpSetupShips = [];
let pvpSelectedShipIdx = null;
let pvpCurrentDir = 'H';

function initPvP(p1Name, p2Name, p1Ships) {
    pvpState.name1 = p1Name || 'Người chơi 1';
    pvpState.name2 = p2Name || 'Người chơi 2';
    pvpState.ships1 = p1Ships;
    pvpState.board1 = buildBoardFromShips(p1Ships);
    pvpPhase = 'setup2';
    document.getElementById('pvp-setup-title').textContent = pvpState.name2 + ' — Đặt thuyền của bạn';
    document.getElementById('pvp-setup-overlay').style.display = 'flex';
}

function buildBoardFromShips(ships) {
    const grid = [];
    for (let x = 0; x < 10; x++) { grid[x] = []; for (let y = 0; y < 10; y++) grid[x][y] = {hasShip:false, hit:false}; }
    (ships || []).forEach(s => {
        for (let i = 0; i < s.length; i++) {
            let cx = s.dir === 'H' ? s.x+i : s.x;
            let cy = s.dir === 'V' ? s.y+i : s.y;
            if (cx < 10 && cy < 10) grid[cx][cy].hasShip = true;
        }
    });
    return grid;
}

function submitPvPSetup2() {
    const allPlaced = SHIP_DEFS.every((_, i) => pvpSetupShips[i]);
    if (!allPlaced) { alert('Hãy đặt tất cả 5 thuyền!'); return; }
    pvpState.ships2 = pvpSetupShips.slice();
    pvpState.board2 = buildBoardFromShips(pvpState.ships2);
    document.getElementById('pvp-setup-overlay').style.display = 'none';
    pvpPhase = 'play';
    pvpState.totalShots = 0;
    pvpState.startTime = Date.now();
    pvpStartTurn(1);
}

function pvpStartTurn(player) {
    pvpState.currentPlayer = player;
    const name = player === 1 ? pvpState.name1 : pvpState.name2;
    document.getElementById('pvp-turn-name').textContent = '🎮 ' + name + ', đến lượt bạn!';
    document.getElementById('pvp-turn-overlay').style.display = 'flex';
}

function pvpReady() {
    document.getElementById('pvp-turn-overlay').style.display = 'none';
    const cp = pvpState.currentPlayer;
    const name = cp === 1 ? pvpState.name1 : pvpState.name2;
    setTurnUI(true, '⚔ ' + name + ' — Hãy bắn!');
    renderPvPBoards(cp);
    startTimer(() => {
        addLog('⏰ ' + name + ' hết giờ! Bỏ lượt.', 'timeout', null);
        setTimeout(() => pvpEndTurn(), 500);
    });
}

function fireShotPvP(x, y) {
    if (gameOver || pvpPhase !== 'play') return;
    const cp = pvpState.currentPlayer;
    const targetBoard = cp === 1 ? pvpState.board2 : pvpState.board1;
    const cell = document.getElementById('enemy-' + x + '-' + y);
    if (!cell || cell.classList.contains('hit-ship') || cell.classList.contains('miss') || cell.classList.contains('sunk-ship')) return;

    clearTimer();
    pvpState.totalShots++;
    const c = targetBoard[x][y];
    c.hit = true;

    let result;
    if (c.hasShip) {
        const ships = cp === 1 ? pvpState.ships2 : pvpState.ships1;
        const sunkShip = checkSunk(ships, targetBoard, x, y);
        if (sunkShip) {
            // Mark all sunk cells
            for (let i = 0; i < sunkShip.length; i++) {
                let cx = sunkShip.dir === 'H' ? sunkShip.x+i : sunkShip.x;
                let cy = sunkShip.dir === 'V' ? sunkShip.y+i : sunkShip.y;
                const sc = document.getElementById('enemy-' + cx + '-' + cy);
                if (sc) sc.className = 'grid-cell sunk-ship';
            }
            result = allShipsSunk(targetBoard) ? 'GAME_OVER' : 'SUNK';
        } else {
            result = 'HIT';
        }
    } else {
        result = 'MISS';
    }

    const name = cp === 1 ? pvpState.name1 : pvpState.name2;
    if (result !== 'SUNK') applyResult(result, x, y, 'enemy');
    addLog(name + ' bắn ' + COLS[x] + ROWS[y] + ': ' + resultText(result), cp === 1 ? 'player' : 'ai', result);

    if (result === 'GAME_OVER') {
        gameOver = true;
        renderPvPBoards(cp);
        const durationSecs = pvpState.startTime ? Math.floor((Date.now() - pvpState.startTime) / 1000) : 0;
        const winnerName = name;
        const loserName  = cp === 1 ? pvpState.name2 : pvpState.name1;
        // Lưu kết quả PvP vào DB qua server
        fetch(contextPath + '/game', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'action=pvp_save'
                + '&winner=' + encodeURIComponent(winnerName)
                + '&loser='  + encodeURIComponent(loserName)
                + '&shots='  + pvpState.totalShots
                + '&duration=' + durationSecs
        }).catch(e => console.warn('pvp_save failed:', e));
        showWinModal('🏆 ' + name + ' thắng!', 'Đã đánh chìm toàn bộ thuyền địch!', true);
        return;
    }
    setTimeout(() => pvpEndTurn(), 700);
}

function pvpEndTurn() {
    const next = pvpState.currentPlayer === 1 ? 2 : 1;
    pvpStartTurn(next);
}

function checkSunk(ships, board, hx, hy) {
    for (const s of ships) {
        let cells = [];
        for (let i = 0; i < s.length; i++) {
            let cx = s.dir === 'H' ? s.x+i : s.x;
            let cy = s.dir === 'V' ? s.y+i : s.y;
            cells.push([cx, cy]);
        }
        if (cells.some(([cx,cy]) => cx === hx && cy === hy)) {
            if (cells.every(([cx,cy]) => board[cx][cy].hit)) return s;
        }
    }
    return null;
}

function allShipsSunk(board) {
    for (let x = 0; x < 10; x++)
        for (let y = 0; y < 10; y++)
            if (board[x][y].hasShip && !board[x][y].hit) return false;
    return true;
}

function renderPvPBoards(viewingPlayer) {
    const enemyBoard = viewingPlayer === 1 ? pvpState.board2 : pvpState.board1;
    const myBoard   = viewingPlayer === 1 ? pvpState.board1 : pvpState.board2;
    const enemyName = viewingPlayer === 1 ? pvpState.name2 : pvpState.name1;
    const myName    = viewingPlayer === 1 ? pvpState.name1 : pvpState.name2;
    const eh = document.querySelector('.enemy-board h2');
    const mh = document.querySelector('.my-board h2');
    if (eh) eh.textContent = '🎯 Lưới của ' + enemyName + ' — bắn vào đây';
    if (mh) mh.textContent = '🛡 Lưới của ' + myName;
    for (let x = 0; x < 10; x++) {
        for (let y = 0; y < 10; y++) {
            const ec = document.getElementById('enemy-' + x + '-' + y);
            const mc = document.getElementById('my-' + x + '-' + y);
            if (ec && enemyBoard) {
                const c = enemyBoard[x][y];
                if (c.hit && c.hasShip) ec.className = 'grid-cell sunk-ship';
                else if (c.hit) ec.className = 'grid-cell miss';
                else ec.className = 'grid-cell';
            }
            if (mc && myBoard) {
                const c = myBoard[x][y];
                if (c.hit && c.hasShip) mc.className = 'grid-cell hit-ship';
                else if (c.hit) mc.className = 'grid-cell miss';
                else if (c.hasShip) mc.className = 'grid-cell has-ship';
                else mc.className = 'grid-cell';
            }
        }
    }
}

// ===== PvP SETUP GRID for player 2 =====
function pvpSelectShip(idx) {
    if (pvpSetupShips[idx]) return;
    pvpSelectedShipIdx = idx;
    document.querySelectorAll('.pvp-ship-item').forEach((el, i) => {
        el.style.borderColor = i === idx ? 'var(--accent)' : '';
    });
}

function pvpToggleDir() {
    pvpCurrentDir = pvpCurrentDir === 'H' ? 'V' : 'H';
    const btn = document.getElementById('pvp-dir-btn');
    if (btn) btn.textContent = pvpCurrentDir === 'H' ? '↔ Ngang' : '↕ Dọc';
}

function pvpSetupCellClick(x, y) {
    if (pvpSelectedShipIdx === null || pvpSetupShips[pvpSelectedShipIdx]) return;
    const ship = SHIP_DEFS[pvpSelectedShipIdx];
    if (!pvpCanPlace(x, y, ship.length, pvpCurrentDir)) { shakeEl('pvp-setup-grid'); return; }
    for (let i = 0; i < ship.length; i++) {
        let cx = pvpCurrentDir === 'H' ? x+i : x, cy = pvpCurrentDir === 'V' ? y+i : y;
        pvpSetupBoard2[cx][cy] = pvpSelectedShipIdx;
        const cell = document.getElementById('pvp-setup-' + cx + '-' + cy);
        if (cell) cell.classList.add('has-ship');
    }
    pvpSetupShips[pvpSelectedShipIdx] = {type: ship.type, length: ship.length, x, y, dir: pvpCurrentDir};
    const item = document.querySelector('.pvp-ship-item[data-idx="' + pvpSelectedShipIdx + '"]');
    if (item) item.classList.add('placed');
    pvpSelectedShipIdx = null;
    document.querySelectorAll('.pvp-ship-item').forEach(el => el.style.borderColor = '');
}

function pvpSetupCellHover(x, y) {
    for (let a = 0; a < 10; a++) for (let b = 0; b < 10; b++) {
        const cell = document.getElementById('pvp-setup-' + a + '-' + b);
        if (cell && !cell.classList.contains('has-ship')) cell.style.background = '';
    }
    if (pvpSelectedShipIdx === null || pvpSetupShips[pvpSelectedShipIdx]) return;
    const ship = SHIP_DEFS[pvpSelectedShipIdx];
    const valid = pvpCanPlace(x, y, ship.length, pvpCurrentDir);
    for (let i = 0; i < ship.length; i++) {
        let cx = pvpCurrentDir === 'H' ? x+i : x, cy = pvpCurrentDir === 'V' ? y+i : y;
        if (cx >= 10 || cy >= 10) continue;
        const cell = document.getElementById('pvp-setup-' + cx + '-' + cy);
        if (cell) cell.style.background = valid ? 'rgba(59,130,246,0.35)' : 'rgba(239,68,68,0.35)';
    }
}

function pvpCanPlace(x, y, len, dir) {
    for (let i = 0; i < len; i++) {
        let cx = dir === 'H' ? x+i : x, cy = dir === 'V' ? y+i : y;
        if (cx >= 10 || cy >= 10) return false;
        if (pvpSetupBoard2[cx][cy] !== null) return false;
    }
    return true;
}

function pvpResetSetup() {
    pvpSetupBoard2 = Array.from({length:10}, () => Array(10).fill(null));
    pvpSetupShips = [];
    pvpSelectedShipIdx = null;
    pvpCurrentDir = 'H';
    for (let x = 0; x < 10; x++) for (let y = 0; y < 10; y++) {
        const cell = document.getElementById('pvp-setup-' + x + '-' + y);
        if (cell) { cell.classList.remove('has-ship'); cell.style.background = ''; }
    }
    document.querySelectorAll('.pvp-ship-item').forEach(el => { el.classList.remove('placed'); el.style.borderColor = ''; });
    const btn = document.getElementById('pvp-dir-btn');
    if (btn) btn.textContent = '↔ Ngang';
}

function pvpAutoPlace() {
    pvpResetSetup();
    SHIP_DEFS.forEach((ship, idx) => {
        let placed = false, tries = 0;
        while (!placed && tries++ < 1000) {
            const x = Math.floor(Math.random()*10), y = Math.floor(Math.random()*10);
            pvpCurrentDir = Math.random() < 0.5 ? 'H' : 'V';
            if (pvpCanPlace(x, y, ship.length, pvpCurrentDir)) {
                pvpSetupCellClick(x, y);
                placed = pvpSetupShips[idx] !== undefined;
            }
        }
    });
}

// ===== SHARED =====
function applyResult(result, x, y, side) {
    const cell = document.getElementById(side + '-' + x + '-' + y);
    if (!cell) return;
    if (result === 'HIT') cell.className = 'grid-cell hit-ship';
    else if (result === 'MISS') cell.className = 'grid-cell miss';
    else if (result === 'SUNK') cell.className = 'grid-cell sunk-ship';
    else if (result === 'GAME_OVER') cell.className = 'grid-cell sunk-ship';
}

function resultText(r) {
    if (r === 'HIT') return '💥 Trúng!';
    if (r === 'MISS') return '💧 Trượt';
    if (r === 'SUNK') return '🔥 Đánh chìm!';
    if (r === 'GAME_OVER') return '💀 Kết thúc!';
    return r || '';
}

function setTurnUI(isMyTurn, customLabel) {
    const indicator = document.getElementById('turn-indicator');
    if (!indicator) return;
    if (customLabel) { indicator.textContent = customLabel; indicator.className = 'turn-indicator'; }
    else if (isMyTurn) { indicator.textContent = '⚔ Lượt của bạn'; indicator.className = 'turn-indicator'; }
    else { indicator.textContent = '⏳ AI đang suy nghĩ...'; indicator.className = 'turn-indicator ai-turn'; }
}

function addLog(msg, who, result) {
    const log = document.getElementById('game-log');
    if (!log) return;
    const div = document.createElement('div');
    let cls = 'log-entry ';
    if (who === 'ai') cls += 'log-ai';
    else if (who === 'timeout') cls += 'log-timeout';
    else if (result === 'HIT' || result === 'SUNK' || result === 'GAME_OVER') cls += 'log-hit';
    else cls += 'log-miss';
    div.className = cls;
    div.textContent = msg;
    log.appendChild(div);
    log.scrollTop = log.scrollHeight;
}

function showWinModal(title, subtitle, won) {
    clearTimer();
    const overlay = document.getElementById('end-modal');
    if (!overlay) return;
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-subtitle').textContent = subtitle;
    document.getElementById('modal-title').className = 'result-title ' + (won ? 'win' : 'lose');
    overlay.style.display = 'flex';
}

function shakeEl(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.animation = 'none';
    setTimeout(() => { el.style.animation = 'shake 0.3s ease'; }, 10);
}

// ===== SETUP PAGE (PvE) =====
let setupBoard = Array.from({length:10}, () => Array(10).fill(null));
let placedShips = [];
let selectedShipIdx = null;
let currentDir = 'H';

function selectShip(idx) {
    if (placedShips[idx]) return;
    selectedShipIdx = idx;
    document.querySelectorAll('.ship-item').forEach((el, i) => {
        el.style.borderColor = i === idx ? 'var(--accent)' : '';
    });
}

function toggleDir() {
    currentDir = currentDir === 'H' ? 'V' : 'H';
    const btn = document.getElementById('dir-btn');
    if (btn) btn.textContent = currentDir === 'H' ? '↔ Ngang' : '↕ Dọc';
}

function canPlace(x, y, len, dir) {
    for (let i = 0; i < len; i++) {
        let cx = dir === 'H' ? x+i : x, cy = dir === 'V' ? y+i : y;
        if (cx >= 10 || cy >= 10) return false;
        if (setupBoard[cx][cy] !== null) return false;
    }
    return true;
}

function placeShipOnSetup(x, y, ship, idx) {
    for (let i = 0; i < ship.length; i++) {
        let cx = currentDir === 'H' ? x+i : x, cy = currentDir === 'V' ? y+i : y;
        setupBoard[cx][cy] = idx;
        const cell = document.getElementById('setup-' + cx + '-' + cy);
        if (cell) cell.classList.add('has-ship');
    }
    placedShips[idx] = {type: ship.type, length: ship.length, x, y, dir: currentDir};
    const item = document.querySelector('.ship-item[data-idx="' + idx + '"]');
    if (item) item.classList.add('placed');
    selectedShipIdx = null;
    document.querySelectorAll('.ship-item').forEach(el => el.style.borderColor = '');
}

function setupCellClick(x, y) {
    if (selectedShipIdx === null || placedShips[selectedShipIdx]) return;
    const ship = SHIP_DEFS[selectedShipIdx];
    if (!canPlace(x, y, ship.length, currentDir)) { shakeEl('setup-grid'); return; }
    placeShipOnSetup(x, y, ship, selectedShipIdx);
}

function setupCellHover(x, y) {
    clearHoverHighlight();
    if (selectedShipIdx === null || placedShips[selectedShipIdx]) return;
    const ship = SHIP_DEFS[selectedShipIdx];
    const valid = canPlace(x, y, ship.length, currentDir);
    for (let i = 0; i < ship.length; i++) {
        let cx = currentDir === 'H' ? x+i : x, cy = currentDir === 'V' ? y+i : y;
        if (cx >= 10 || cy >= 10) continue;
        const cell = document.getElementById('setup-' + cx + '-' + cy);
        if (cell) cell.style.background = valid ? 'rgba(59,130,246,0.35)' : 'rgba(239,68,68,0.35)';
    }
}

function clearHoverHighlight() {
    for (let x = 0; x < 10; x++) for (let y = 0; y < 10; y++) {
        const cell = document.getElementById('setup-' + x + '-' + y);
        if (cell && !cell.classList.contains('has-ship')) cell.style.background = '';
    }
}

function resetSetup() {
    setupBoard = Array.from({length:10}, () => Array(10).fill(null));
    placedShips = []; selectedShipIdx = null;
    for (let x = 0; x < 10; x++) for (let y = 0; y < 10; y++) {
        const cell = document.getElementById('setup-' + x + '-' + y);
        if (cell) { cell.classList.remove('has-ship'); cell.style.background = ''; }
    }
    document.querySelectorAll('.ship-item').forEach(el => el.classList.remove('placed'));
}

function submitSetup() {
    const allPlaced = SHIP_DEFS.every((_, i) => placedShips[i]);
    if (!allPlaced) { alert('Hãy đặt tất cả 5 thuyền trước!'); return; }
    const shipsJson = JSON.stringify(placedShips.map(s => ({type:s.type,length:s.length,x:s.x,y:s.y,dir:s.dir})));
    document.getElementById('ships-input').value = shipsJson;
    const p2in = document.getElementById('p2name-input');
    const p2hid = document.getElementById('p2name-hidden');
    if (p2in && p2hid) p2hid.value = p2in.value;
    document.getElementById('setup-form').submit();
}

function shakeGrid() { shakeEl('setup-grid'); }

function doAutoPlace() {
    resetSetup();
    SHIP_DEFS.forEach((ship, idx) => {
        let placed = false, tries = 0;
        while (!placed && tries++ < 1000) {
            const x = Math.floor(Math.random()*10), y = Math.floor(Math.random()*10);
            const dir = Math.random() < 0.5 ? 'H' : 'V';
            const prev = currentDir; currentDir = dir;
            if (canPlace(x, y, ship.length, dir)) { placeShipOnSetup(x, y, ship, idx); placed = true; }
            else currentDir = prev;
        }
    });
}

let historyState = {
    currentPage: 1,
    pageSize: 10,
    currentFilter: { mode: 'all', result: 'all' },
    allMatches: []
};

function initHistoryPage() {
    setupHistoryEventListeners();
    loadHistoryData();
}

function setupHistoryEventListeners() {
    document.querySelectorAll('[data-mode]').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('[data-mode]').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            historyState.currentFilter.mode = this.dataset.mode;
            historyState.currentPage = 1;
            loadHistoryData();
        });
    });

    document.querySelectorAll('[data-result]').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('[data-result]').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            historyState.currentFilter.result = this.dataset.result;
            historyState.currentPage = 1;
            loadHistoryData();
        });
    });

    document.querySelectorAll('[data-view]').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('[data-view]').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            document.querySelectorAll('.view-section').forEach(s => s.classList.remove('active'));
            const viewId = this.dataset.view + '-view';
            const viewEl = document.getElementById(viewId);
            if (viewEl) {
                viewEl.classList.add('active');
                if (this.dataset.view === 'chart') {
                    renderHistoryCharts();
                }
            }
        });
    });

    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (historyState.currentPage > 1) {
                historyState.currentPage--;
                loadHistoryData();
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            historyState.currentPage++;
            loadHistoryData();
        });
    }
}

async function loadHistoryData() {
    const container = document.querySelector('.matches-container');
    if (!container) return;

    container.innerHTML = '<div class="loading">⏳ Đang tải lịch sử...</div>';

    try {
        const params = new URLSearchParams({
            page: historyState.currentPage,
            mode: historyState.currentFilter.mode,
            result: historyState.currentFilter.result
        });

        const response = await fetch(`${contextPath}/api/history?${params}`);
        const data = await response.json();

        historyState.allMatches = data.matches || [];
        renderHistoryMatches(historyState.allMatches);
        updateHistoryStats();
        updateHistoryPagination(data.totalPages || 1);
    } catch (error) {
        console.error('Error loading history:', error);
        container.innerHTML = '<div class="empty-state"> Lỗi tải dữ liệu</div>';
    }
}

function renderHistoryMatches(matches) {
    const container = document.querySelector('.matches-container');
    if (!container) return;

    if (matches.length === 0) {
        container.innerHTML = '<div class="empty-state">🎮 Chưa có trận đấu nào</div>';
        return;
    }

    container.innerHTML = matches.map(match => `
        <div class="match-card ${match.won ? 'won' : 'lost'}" onclick="showHistoryMatchDetail('${match.id}')">
            <div class="match-result">${match.won ? '' : ''}</div>
            <div class="match-info">
                <div class="match-opponent">vs ${match.opponent}</div>
                <div class="match-meta">
                    <span class="badge ${match.mode === 'PvE' ? 'badge-pve' : 'badge-pvp'}">${match.mode}</span>
                    <span>${formatHistoryDate(match.date)}</span>
                </div>
            </div>
            <div class="badge-result">
                <div class="badge ${match.won ? 'badge-win' : 'badge-loss'}">
                    ${match.won ? '✓ Thắng' : '✗ Thua'}
                </div>
            </div>
            <div class="match-score">${match.score}</div>
            <div class="match-actions">
                <button class="btn-small" onclick="event.stopPropagation(); viewHistoryReplay('${match.id}')">▶ Xem lại</button>
            </div>
        </div>
    `).join('');
}

function updateHistoryStats() {
    const stats = calculateHistoryStats(historyState.allMatches);

    const totalEl = document.querySelector('.stat-total .stat-value');
    const winsEl = document.querySelector('.stat-wins .stat-value');
    const lossesEl = document.querySelector('.stat-losses .stat-value');
    const winrateEl = document.querySelector('.stat-winrate .stat-value');

    if (totalEl) totalEl.textContent = stats.total;
    if (winsEl) winsEl.textContent = stats.wins;
    if (lossesEl) lossesEl.textContent = stats.losses;
    if (winrateEl) winrateEl.textContent = stats.winRate.toFixed(1) + '%';
}

function calculateHistoryStats(matches) {
    const total = matches.length;
    const wins = matches.filter(m => m.won).length;
    const losses = total - wins;
    const winRate = total > 0 ? (wins / total) * 100 : 0;

    return { total, wins, losses, winRate };
}

function updateHistoryPagination(totalPages) {
    const currentPageEl = document.getElementById('current-page');
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');

    if (currentPageEl) currentPageEl.textContent = historyState.currentPage;
    if (prevBtn) prevBtn.disabled = historyState.currentPage === 1;
    if (nextBtn) nextBtn.disabled = historyState.currentPage >= totalPages;
}

function showHistoryMatchDetail(matchId) {
    const modal = document.getElementById('matchModal');
    if (!modal) return;

    const modalBody = document.getElementById('modalBody');
    if (modalBody) {
        modalBody.innerHTML = '<div class="loading">⏳ Đang tải chi tiết...</div>';
    }

    modal.classList.add('show');

    fetch(`${contextPath}/api/match/${matchId}`)
        .then(r => r.json())
        .then(match => {
            if (modalBody) {
                modalBody.innerHTML = `
                    <h2>Chi Tiết Trận Đấu</h2>
                    <div style="margin-top: 20px;">
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Đối thủ:</span>
                            <strong>${match.opponent}</strong>
                        </div>
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Chế độ:</span>
                            <strong>${match.mode}</strong>
                        </div>
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Kết quả:</span>
                            <strong>${match.won ? '✅ Thắng' : '❌ Thua'}</strong>
                        </div>
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Điểm:</span>
                            <strong>${match.score}</strong>
                        </div>
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Thời gian:</span>
                            <strong>${match.duration}</strong>
                        </div>
                        <div style="margin-bottom: 15px;">
                            <span style="color: var(--text-muted);">Số lượt:</span>
                            <strong>${match.shots}</strong>
                        </div>
                    </div>
                `;
            }
        })
        .catch(err => {
            console.error(err);
            if (modalBody) {
                modalBody.innerHTML = '<div style="color: var(--danger);">❌ Lỗi tải chi tiết</div>';
            }
        });
}

function viewHistoryReplay(matchId) {
    alert('Tính năng xem lại sẽ sớm được cập nhật!');
}

function renderHistoryCharts() {
    const stats = calculateHistoryStats(historyState.allMatches);

    const ctx1 = document.getElementById('resultChart');
    if (ctx1 && typeof Chart !== 'undefined') {
        try {

        } catch(e) {
            console.warn('Chart rendering failed:', e);
        }
    }
}

function formatHistoryDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

let leaderboardState = {
    leaderboardData: [],
    currentUser: null
};

function initLeaderboardPage() {
    loadLeaderboardData();
    setupLeaderboardEventListeners();
}

function setupLeaderboardEventListeners() {
    document.querySelectorAll('.sort-option').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.sort-option').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            loadLeaderboardData();
        });
    });

    document.querySelectorAll('.time-range').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.time-range').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            loadLeaderboardData();
        });
    });
}

async function loadLeaderboardData() {
    const tbody = document.querySelector('.leaderboard-table tbody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8"><div class="loading">⏳ Đang tải bảng xếp hạng...</div></td></tr>';
    }

    try {
        const response = await fetch(`${contextPath}/api/leaderboard?limit=100`);
        const data = await response.json();

        leaderboardState.leaderboardData = data.entries || [];
        leaderboardState.currentUser = data.currentUser;

        renderLeaderboardTable();
        renderTopPlayers();
        displayUserPosition();
    } catch (error) {
        console.error('Error loading leaderboard:', error);
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--danger);">❌ Lỗi tải dữ liệu</td></tr>';
        }
    }
}

function renderLeaderboardTable() {
    const tbody = document.querySelector('.leaderboard-table tbody');
    if (!tbody) return;

    if (leaderboardState.leaderboardData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">🏆 Chưa có dữ liệu xếp hạng</td></tr>';
        return;
    }

    tbody.innerHTML = leaderboardState.leaderboardData.map((entry, index) => `
        <tr class="${leaderboardState.currentUser && entry.userId === leaderboardState.currentUser.id ? 'highlight' : ''}">
            <td class="rank-cell">
                ${entry.rank <= 3 ? `<span class="rank-badge top-3">${['🥇', '🥈', '🥉'][entry.rank-1]}</span>` : `<span class="rank-badge">#${entry.rank}</span>`}
            </td>
            <td>
                <div class="player-info">
                    <span class="player-name ${leaderboardState.currentUser && entry.userId === leaderboardState.currentUser.id ? 'current-user' : ''}">
                        ${entry.username}
                        ${leaderboardState.currentUser && entry.userId === leaderboardState.currentUser.id ? ' (Bạn)' : ''}
                    </span>
                    <span class="level-badge">Lv ${entry.level}</span>
                </div>
            </td>
            <td class="stat-wins">${entry.totalWins}</td>
            <td class="stat-losses">${entry.totalLosses}</td>
            <td>${entry.totalGames}</td>
            <td class="stat-winrate">${entry.winRate.toFixed(1)}%</td>
            <td class="stat-elo">${entry.eloRating}</td>
            <td>
                <span class="trend-indicator trend-${entry.trend}">
                    ${entry.trend === 'up' ? '📈' : entry.trend === 'down' ? '📉' : '➡️'}
                </span>
            </td>
        </tr>
    `).join('');
}

function renderTopPlayers() {
    const topPlayers = leaderboardState.leaderboardData.slice(0, 3);
    const container = document.querySelector('.top-players');

    if (!container) return;

    container.innerHTML = topPlayers.map((player, index) => `
        <div class="rank-card rank-${index + 1}">
            <div class="rank-medal">${['🥇', '🥈', '🥉'][index]}</div>
            <div class="rank-name">${player.username}</div>
            <div class="rank-stats">
                <div>Lv ${player.level}</div>
                <div>⭐ ${player.eloRating} ELO</div>
                <div>${player.totalWins}W - ${player.totalLosses}L</div>
            </div>
        </div>
    `).join('');
}

function displayUserPosition() {
    if (!leaderboardState.currentUser) return;

    const userEntry = leaderboardState.leaderboardData.find(e => e.userId === leaderboardState.currentUser.id);
    const card = document.querySelector('.user-position-card');

    if (card && userEntry) {
        card.innerHTML = `
            <div class="user-position-info">
                <div class="position-number">#${userEntry.rank}</div>
                <div class="position-details">
                    <div class="position-label">Xếp hạng của bạn</div>
                    <div class="position-value">${userEntry.username}</div>
                    <div class="position-label">⭐ ${userEntry.eloRating} ELO | Lv ${userEntry.level}</div>
                </div>
            </div>
        `;
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('matchModal');
    const closeBtn = document.querySelector('.modal-close');

    if (closeBtn && modal) {
        closeBtn.onclick = () => {
            modal.classList.remove('show');
        };
    }

    if (modal) {
        window.addEventListener('click', (event) => {
            if (event.target === modal) {
                modal.classList.remove('show');
            }
        });
    }
});