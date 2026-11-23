const listElement = document.getElementById('raid-list');
const gatesContainer = document.getElementById('gates-container');
const idInput = document.getElementById('raid-id');
const nameInput = document.getElementById('raid-name');
const categoryInput = document.getElementById('raid-category');
const memberCountInput = document.getElementById('raid-member-count');
const partyCountOutput = document.getElementById('raid-party-count');
const statusElement = document.getElementById('raid-status');
const deleteButton = document.getElementById('raid-delete');
let raids = [];

const message = (text) => { statusElement.textContent = text; };

function updatePartyCount() {
    const memberCount = Number(memberCountInput.value);
    partyCountOutput.textContent = Number.isInteger(memberCount) && memberCount > 0
        ? String(Math.ceil(memberCount / 4))
        : '-';
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
    });
    if (response.status === 401) {
        window.location.href = '/users/login';
        throw new Error('인증이 필요합니다.');
    }
    if (response.status === 403) throw new Error('관리자 권한이 필요합니다.');
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || body.detail || `요청 실패 (${response.status})`);
    }
    return response.status === 204 ? null : response.json();
}

function formatHp(value) {
    if (value == null) return '';
    let remaining = BigInt(value);
    const units = [[1000000000000n, '조'], [100000000n, '억'], [10000n, '만']];
    const parts = [];
    units.forEach(([base, name]) => {
        const group = remaining / base;
        if (group > 0n) parts.push(`${group}${name}`);
        remaining %= base;
    });
    if (remaining > 0n || parts.length === 0) parts.push(remaining.toString());
    return parts.join(' ');
}

function parseHp(value) {
    try {
        const normalized = value.replace(/[\s,]/g, '');
        if (!normalized) return null;
        let hp = 0n;
        const unitPattern = /(\d+)(조|억|만)/g;
        let match;
        let matched = '';
        while ((match = unitPattern.exec(normalized)) !== null) {
            matched += match[0];
            hp += BigInt(match[1]) * { 조: 1000000000000n, 억: 100000000n, 만: 10000n }[match[2]];
        }
        const rest = normalized.replace(unitPattern, '');
        if ((rest && !/^\d+$/.test(rest)) || (!matched && !/^\d+$/.test(normalized))) return null;
        if (rest) hp += BigInt(rest);
        return hp <= 9223372036854775807n ? hp.toString() : null;
    } catch {
        return null;
    }
}

function addDifficulty(container, difficulty = {}) {
    const row = document.createElement('div');
    row.className = 'difficulty-row';
    const name = document.createElement('select');
    name.className = 'difficulty-name';
    [['', '난이도 선택'], ['매칭', '매칭'], ['노말', '노말'], ['하드', '하드'], ['익스트림', '익스트림'], ['나이트메어', '나이트메어'], ['1단계', '1단계'], ['2단계', '2단계'], ['3단계', '3단계']]
        .forEach(([value, label]) => {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            name.append(option);
        });
    name.value = difficulty.difficulty || '';
    name.addEventListener('change', () => sortDifficultyRows(container));
    const hp = document.createElement('input');
    hp.className = 'difficulty-hp';
    hp.placeholder = 'HP (예: 1조 2345억)';
    hp.value = formatHp(difficulty.raidHp);
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.textContent = '삭제';
    remove.addEventListener('click', () => row.remove());
    row.append(name, hp, remove);
    container.append(row);
    sortDifficultyRows(container);
}

function sortDifficultyRows(container) {
    const order = ['매칭', '노말', '하드', '익스트림'];
    [...container.querySelectorAll('.difficulty-row')]
        .sort((left, right) => {
            const leftValue = left.querySelector('.difficulty-name').value;
            const rightValue = right.querySelector('.difficulty-name').value;
            const leftRank = order.indexOf(leftValue);
            const rightRank = order.indexOf(rightValue);
            return (leftRank === -1 ? order.length : leftRank) - (rightRank === -1 ? order.length : rightRank);
        })
        .forEach((row) => container.append(row));
}

function addGate(gate = {}) {
    const card = document.createElement('section');
    card.className = 'gate-card';
    const heading = document.createElement('h4');
    heading.textContent = `${gate.gateNumber || gatesContainer.children.length + 1}관문`;
    const fields = document.createElement('div');
    fields.className = 'gate-fields';
    const gateNumber = document.createElement('input');
    gateNumber.className = 'gate-number';
    gateNumber.type = 'number';
    gateNumber.min = '1';
    gateNumber.step = '1';
    gateNumber.placeholder = '관문 번호';
    gateNumber.value = gate.gateNumber || gatesContainer.children.length + 1;
    const bossName = document.createElement('input');
    bossName.className = 'boss-name';
    bossName.maxLength = 100;
    bossName.placeholder = '보스 이름';
    bossName.value = gate.bossName || '';
    fields.append(gateNumber, bossName);
    const difficultyList = document.createElement('div');
    difficultyList.className = 'difficulty-list';
    (gate.difficulties || []).forEach((difficulty) => addDifficulty(difficultyList, difficulty));
    const actions = document.createElement('div');
    actions.className = 'gate-actions';
    const addDifficultyButton = document.createElement('button');
    addDifficultyButton.type = 'button';
    addDifficultyButton.textContent = '+ 난이도 추가';
    addDifficultyButton.addEventListener('click', () => addDifficulty(difficultyList));
    const removeGateButton = document.createElement('button');
    removeGateButton.type = 'button';
    removeGateButton.textContent = '관문 삭제';
    removeGateButton.addEventListener('click', () => card.remove());
    actions.append(addDifficultyButton, removeGateButton);
    card.append(heading, fields, difficultyList, actions);
    gatesContainer.append(card);
}

function clearForm() {
    idInput.value = '';
    nameInput.value = '';
    categoryInput.value = '';
    memberCountInput.value = '';
    updatePartyCount();
    gatesContainer.replaceChildren();
    deleteButton.disabled = true;
    document.getElementById('raid-form-title').textContent = '새 레이드 등록';
    listElement.querySelectorAll('li').forEach((item) => item.classList.remove('active'));
}

function selectRaid(raid) {
    idInput.value = raid.id;
    nameInput.value = raid.raidName;
    categoryInput.value = raid.raidCategory;
    memberCountInput.value = raid.memberCount;
    updatePartyCount();
    gatesContainer.replaceChildren();
    raid.gates.forEach(addGate);
    deleteButton.disabled = false;
    document.getElementById('raid-form-title').textContent = `수정: ${raid.raidName}`;
    listElement.querySelectorAll('li').forEach((item) => item.classList.toggle('active', Number(item.dataset.id) === raid.id));
    message('');
}

function collectGates() {
    return [...gatesContainer.querySelectorAll('.gate-card')].map((card) => ({
        gateNumber: Number(card.querySelector('.gate-number').value),
        bossName: card.querySelector('.boss-name').value.trim(),
        difficulties: [...card.querySelectorAll('.difficulty-row')].map((row) => ({
            difficulty: row.querySelector('.difficulty-name').value.trim(),
            raidHp: parseHp(row.querySelector('.difficulty-hp').value)
        }))
    }));
}

function renderList() {
    listElement.replaceChildren();
    raids.forEach((raid) => {
        const item = document.createElement('li');
        item.dataset.id = raid.id;
        const title = document.createElement('strong');
        title.textContent = raid.raidName;
        const category = document.createElement('span');
        category.className = 'raid-category-label';
        category.textContent = `${raid.raidCategory} · ${raid.gates.length}개 관문`;
        item.append(title, category);
        item.addEventListener('click', () => selectRaid(raid));
        listElement.append(item);
    });
}

async function loadRaids() {
    try {
        raids = await request('/admin/raids/api');
        renderList();
    } catch (error) {
        message(error.message);
    }
}

document.getElementById('raid-new').addEventListener('click', () => {
    clearForm();
    message('');
    nameInput.focus();
});
document.getElementById('gate-add').addEventListener('click', () => addGate());
document.getElementById('raid-save').addEventListener('click', async () => {
    const payload = {
        id: idInput.value ? Number(idInput.value) : null,
        raidName: nameInput.value.trim(),
        raidCategory: categoryInput.value.trim(),
        memberCount: Number(memberCountInput.value),
        gates: collectGates()
    };
    const invalid = !payload.raidName || !payload.raidCategory || payload.memberCount < 1 || payload.gates.length === 0
        || payload.gates.some((gate) => !gate.gateNumber || !gate.bossName
            || gate.difficulties.length === 0 || gate.difficulties.some((difficulty) => !difficulty.difficulty || difficulty.raidHp === null));
    if (invalid) {
        message('레이드와 관문 정보를 입력하고 각 관문에 난이도와 HP를 하나 이상 추가해 주세요.');
        return;
    }
    try {
        const isUpdate = Boolean(payload.id);
        const url = isUpdate ? `/admin/raids/api/${payload.id}` : '/admin/raids/api';
        const saved = await request(url, { method: isUpdate ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        await loadRaids();
        const current = raids.find((raid) => raid.id === saved.id);
        if (current) selectRaid(current);
        message('저장했습니다.');
    } catch (error) {
        message(error.message);
    }
});
deleteButton.addEventListener('click', async () => {
    const id = idInput.value;
    if (!id || !window.confirm('이 레이드와 모든 관문 정보를 삭제할까요?')) return;
    try {
        await request(`/admin/raids/api/${id}`, { method: 'DELETE' });
        clearForm();
        await loadRaids();
        message('삭제했습니다.');
    } catch (error) {
        message(error.message);
    }
});

loadRaids();
memberCountInput.addEventListener('input', updatePartyCount);
