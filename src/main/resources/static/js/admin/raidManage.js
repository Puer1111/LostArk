const listElement = document.getElementById('raid-list');
const idInput = document.getElementById('raid-id');
const nameInput = document.getElementById('raid-name');
const categoryInput = document.getElementById('raid-category');
const memberCountInput = document.getElementById('raid-member-count');
const partyCountInput = document.getElementById('raid-party-count');
const statusElement = document.getElementById('raid-status');
const deleteButton = document.getElementById('raid-delete');
let raids = [];

const message = (text) => { statusElement.textContent = text; };

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
    });
    if (response.status === 401) {
        window.location.href = '/users/login';
        throw new Error('Authentication required.');
    }
    if (response.status === 403) throw new Error('Administrator permission required.');
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || body.detail || `Request failed (${response.status})`);
    }
    return response.status === 204 ? null : response.json();
}

function clearForm() {
    idInput.value = '';
    nameInput.value = '';
    categoryInput.value = '';
    memberCountInput.value = '';
    partyCountInput.value = '';
    deleteButton.disabled = true;
    document.getElementById('raid-form-title').textContent = 'Register a raid';
    listElement.querySelectorAll('li').forEach((item) => item.classList.remove('active'));
}

function selectRaid(raid) {
    idInput.value = raid.id;
    nameInput.value = raid.raidName;
    categoryInput.value = raid.raidCategory;
    memberCountInput.value = raid.memberCount;
    partyCountInput.value = raid.partyCount;
    deleteButton.disabled = false;
    document.getElementById('raid-form-title').textContent = `Edit: ${raid.raidName}`;
    listElement.querySelectorAll('li').forEach((item) => {
        item.classList.toggle('active', Number(item.dataset.id) === raid.id);
    });
    message('');
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
        category.textContent = `${raid.raidCategory} \u00B7 ${raid.memberCount} players \u00B7 ${raid.partyCount} parties`;
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

document.getElementById('raid-save').addEventListener('click', async () => {
    const payload = {
        id: idInput.value ? Number(idInput.value) : null,
        raidName: nameInput.value.trim(),
        raidCategory: categoryInput.value.trim(),
        memberCount: Number(memberCountInput.value),
        partyCount: Number(partyCountInput.value)
    };
    if (!payload.raidName || !payload.raidCategory || payload.memberCount < 1 || payload.partyCount < 1) {
        message('Enter a name, category, player count, and party count.');
        return;
    }
    try {
        const saved = await request('/admin/raids/api', { method: 'POST', body: JSON.stringify(payload) });
        await loadRaids();
        const current = raids.find((raid) => raid.id === saved.id);
        if (current) selectRaid(current);
        message('Saved.');
    } catch (error) {
        message(error.message);
    }
});

deleteButton.addEventListener('click', async () => {
    const id = idInput.value;
    if (!id || !window.confirm('Delete this raid from the database?')) return;
    try {
        await request(`/admin/raids/api/${id}`, { method: 'DELETE' });
        clearForm();
        await loadRaids();
        message('Deleted.');
    } catch (error) {
        message(error.message);
    }
});

loadRaids();
