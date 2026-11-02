/**
 * 어드민 직업 및 시너지 관리 스크립트
 * synergyDataV3.js 스키마와 1:1 매핑
 */

let currentJobList = [];
let activeJobData = null;
let currentTab = 'active'; // 'active' | 'trash'

document.addEventListener('DOMContentLoaded', () => {
    initEvents();
    loadJobList();
});

function initEvents() {
    document.getElementById('btn-new-job').addEventListener('click', () => {
        if (currentTab === 'trash') {
            currentTab = 'active';
            updateTabButtons();
            loadJobList();
        }
        resetForm();
    });

    document.getElementById('btn-save-job').addEventListener('click', () => {
        saveCurrentJob();
    });

    const btnDelete = document.getElementById('btn-delete-job');
    if (btnDelete) {
        btnDelete.addEventListener('click', () => {
            deleteCurrentJob();
        });
    }

    const btnRestore = document.getElementById('btn-restore-job');
    if (btnRestore) {
        btnRestore.addEventListener('click', () => {
            restoreCurrentJob();
        });
    }

    // 탭 전환 이벤트
    const tabActive = document.getElementById('tab-active-jobs');
    const tabTrash = document.getElementById('tab-trash-jobs');
    if (tabActive && tabTrash) {
        tabActive.addEventListener('click', () => {
            if (currentTab !== 'active') {
                currentTab = 'active';
                updateTabButtons();
                resetForm();
                loadJobList();
            }
        });
        tabTrash.addEventListener('click', () => {
            if (currentTab !== 'trash') {
                currentTab = 'trash';
                updateTabButtons();
                resetForm();
                loadJobList();
            }
        });
    }

    document.getElementById('btn-add-engraving').addEventListener('click', () => {
        addEngravingRow();
        updatePreview();
    });

    document.getElementById('btn-add-skill').addEventListener('click', () => {
        addSkillCard();
        updatePreview();
    });

    document.getElementById('input-className').addEventListener('input', updatePreview);
    
    // 직업군 선택 및 직접 입력 전환 이벤트
    const selectClassGroup = document.getElementById('select-classGroup');
    const inputCustomClassGroup = document.getElementById('input-customClassGroup');

    selectClassGroup.addEventListener('change', () => {
        if (selectClassGroup.value === 'DIRECT') {
            inputCustomClassGroup.style.display = 'inline-block';
            inputCustomClassGroup.focus();
        } else {
            inputCustomClassGroup.style.display = 'none';
            inputCustomClassGroup.value = '';
        }
        updatePreview();
    });

    inputCustomClassGroup.addEventListener('input', updatePreview);
}

function updateTabButtons() {
    const tabActive = document.getElementById('tab-active-jobs');
    const tabTrash = document.getElementById('tab-trash-jobs');
    if (!tabActive || !tabTrash) return;

    if (currentTab === 'active') {
        tabActive.style.background = '#3f51b5';
        tabActive.style.color = '#fff';
        tabTrash.style.background = '#2a2a32';
        tabTrash.style.color = '#aaa';
    } else {
        tabActive.style.background = '#2a2a32';
        tabActive.style.color = '#aaa';
        tabTrash.style.background = '#ff9800';
        tabTrash.style.color = '#fff';
    }
}

// 1. 서버에서 직업 목록 조회 (활성 / 휴지통 분기)
async function loadJobList() {
    try {
        const url = (currentTab === 'trash') ? '/admin/synergies/api/trash' : '/admin/synergies/api/list';
        const res = await fetch(url);
        if (!res.ok) throw new Error('목록 조회 실패');
        currentJobList = await res.json();
        renderJobList();
    } catch (err) {
        console.error(err);
        alert('직업 목록을 불러오지 못했습니다: ' + err.message);
    }
}

function renderJobList() {
    const ul = document.getElementById('job-list-ul');
    ul.innerHTML = '';

    if (currentJobList.length === 0) {
        const emptyMsg = (currentTab === 'trash') ? '휴지통이 비어 있습니다.' : '등록된 직업 없음';
        ul.innerHTML = `<li style="cursor: default; color: #888;">${emptyMsg}</li>`;
        return;
    }

    currentJobList.forEach(job => {
        const li = document.createElement('li');
        if (currentTab === 'trash') {
            li.innerHTML = `<span>${job.className}</span> <span style="font-size: 11px; color: #ffb74d;">[삭제됨]</span>`;
        } else {
            li.textContent = `${job.className} (${job.classGroup || '미분류'})`;
        }
        
        if (activeJobData && activeJobData.className === job.className) {
            li.classList.add('active');
        }

        li.addEventListener('click', () => {
            selectJob(job);
        });

        ul.appendChild(li);
    });
}

function selectJob(job) {
    activeJobData = JSON.parse(JSON.stringify(job)); // Deep copy
    renderJobList();
    populateForm(activeJobData);

    const btnSave = document.getElementById('btn-save-job');
    const btnDelete = document.getElementById('btn-delete-job');
    const btnRestore = document.getElementById('btn-restore-job');

    if (currentTab === 'trash') {
        if (btnSave) btnSave.disabled = true;
        if (btnDelete) btnDelete.style.display = 'none';
        if (btnRestore) {
            btnRestore.style.display = 'inline-block';
            btnRestore.disabled = false;
        }
    } else {
        if (btnSave) btnSave.disabled = false;
        if (btnDelete) {
            btnDelete.style.display = 'inline-block';
            btnDelete.disabled = false;
        }
        if (btnRestore) btnRestore.style.display = 'none';
    }
}

function resetForm() {
    activeJobData = {
        className: '',
        classGroup: '전사',
        classEngravings: [],
        skills: []
    };
    renderJobList();
    populateForm(activeJobData);

    const btnSave = document.getElementById('btn-save-job');
    const btnDelete = document.getElementById('btn-delete-job');
    const btnRestore = document.getElementById('btn-restore-job');

    if (btnSave) btnSave.disabled = false;
    if (btnDelete) {
        btnDelete.style.display = 'inline-block';
        btnDelete.disabled = true;
    }
    if (btnRestore) {
        btnRestore.style.display = 'none';
    }
}

// 폼 채우기
function populateForm(data) {
    document.getElementById('input-className').value = data.className || '';
    
    const targetGroup = data.classGroup || '전사';
    const selectElem = document.getElementById('select-classGroup');
    const customInput = document.getElementById('input-customClassGroup');

    const hasOption = Array.from(selectElem.options).some(opt => opt.value === targetGroup && opt.value !== 'DIRECT');
    if (hasOption) {
        selectElem.value = targetGroup;
        customInput.style.display = 'none';
        customInput.value = '';
    } else {
        selectElem.value = 'DIRECT';
        customInput.style.display = 'inline-block';
        customInput.value = targetGroup;
    }

    // 각인 렌더링
    const engContainer = document.getElementById('engravings-container');
    engContainer.innerHTML = '';
    (data.classEngravings || []).forEach(eng => addEngravingRow(eng.engravingName, eng.role));

    // 시너지 렌더링
    const skillContainer = document.getElementById('skills-container');
    skillContainer.innerHTML = '';
    (data.skills || []).forEach(skill => addSkillCard(skill));

    updatePreview();
}

// 각인 행 추가
function addEngravingRow(name = '', role = '') {
    const container = document.getElementById('engravings-container');
    const div = document.createElement('div');
    div.className = 'form-group engraving-row';
    div.innerHTML = `
        <input type="text" class="eng-name" placeholder="각인명 (예: 질풍노도)" value="${name}" style="flex: 1;">
        <input type="text" class="eng-role" placeholder="설명/역할 (예: 우산 스킬 강화)" value="${role}" style="flex: 2;">
        <button type="button" class="btn btn-danger btn-sm btn-del-eng">삭제</button>
    `;

    div.querySelector('.btn-del-eng').addEventListener('click', () => {
        div.remove();
        updatePreview();
    });

    div.querySelectorAll('input').forEach(inp => inp.addEventListener('input', updatePreview));
    container.appendChild(div);
}

// 시너지 카드 추가
function addSkillCard(skillData = null) {
    const container = document.getElementById('skills-container');
    const card = document.createElement('div');
    card.className = 'sub-card skill-card';

    const synergyName = skillData ? skillData.name || '' : '';
    const priority = skillData ? skillData.priority || '' : '';

    card.innerHTML = `
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
            <strong style="color: #ffca28;">시너지 정의</strong>
            <button type="button" class="btn btn-danger btn-sm btn-del-skill">시너지 삭제</button>
        </div>
        <div class="form-group">
            <label>시너지 종류</label>
            <input type="text" class="skill-synergy-name" placeholder="예: 치명타 저항 감소, 피해 증가 등" value="${synergyName}">
            <label style="width: 70px;">우선순위</label>
            <select class="skill-priority" style="width: 100px;">
                <option value="" ${priority !== 'main' ? 'selected' : ''}>일반</option>
                <option value="main" ${priority === 'main' ? 'selected' : ''}>main</option>
            </select>
        </div>
        <div style="margin-top: 10px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px;">
                <span style="font-size: 13px; color: #81c784;">발동 스킬/트라이포드 목록 (sources)</span>
                <button type="button" class="btn btn-secondary btn-sm btn-add-source">+ 스킬 출처 추가</button>
            </div>
            <div class="sources-container"></div>
        </div>
    `;

    card.querySelector('.btn-del-skill').addEventListener('click', () => {
        card.remove();
        updatePreview();
    });

    const sourcesContainer = card.querySelector('.sources-container');
    card.querySelector('.btn-add-source').addEventListener('click', () => {
        addSourceRow(sourcesContainer);
        updatePreview();
    });

    if (skillData && skillData.sources) {
        skillData.sources.forEach(src => addSourceRow(sourcesContainer, src));
    }

    card.querySelectorAll('input, select').forEach(el => el.addEventListener('input', updatePreview));
    container.appendChild(card);
}

// 발동 스킬 출처 행 추가
function addSourceRow(container, src = {}) {
    const div = document.createElement('div');
    div.className = 'source-box';
    div.style.marginBottom = '10px';

    const skillImg = src.skillImg || '';
    const skillDescription = src.skillDescription || '';
    const tripodDescription = src.tripodDescription || '';

    div.innerHTML = `
        <div class="form-group source-row" style="margin-bottom: 0;">
            <img class="skill-img-preview" src="${skillImg || 'data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'32\' height=\'32\'><rect width=\'32\' height=\'32\' fill=\'%23333\'/><text x=\'50%\' y=\'50%\' fill=\'%23777\' font-size=\'10\' text-anchor=\'middle\' dy=\'.3em\'>NoImg</text></svg>'}" alt="스킬">
            <input type="text" class="src-skill" placeholder="스킬명" value="${src.skillName || ''}" style="flex: 1.5;">
            <input type="text" class="src-img" placeholder="스킬 이미지 URL (CDN or /uploads/...)" value="${skillImg}" style="flex: 2;">
            <input type="file" class="src-file" accept="image/*" style="display: none;">
            <button type="button" class="btn btn-secondary btn-sm btn-upload-img" title="PC에서 이미지 파일 업로드">파일선택</button>
            <input type="text" class="src-tripod" placeholder="트라이포드명(선택)" value="${src.tripodName || ''}" style="flex: 1.2;">
            <input type="text" class="src-tripodRate" placeholder="트포 수치 (예: 10%)" value="${src.tripodRate || ''}" style="flex: 0.8;">
            <input type="text" class="src-baseRate" placeholder="기본 수치 (예: 12%)" value="${src.baseRate || ''}" style="flex: 0.8;">
            <button type="button" class="btn btn-danger btn-sm btn-del-src">X</button>
        </div>
        <div class="source-desc-row">
            <div class="desc-field">
                <label>💬 스킬 설명 (skillDescription)</label>
                <textarea class="src-skill-desc" rows="2" placeholder="스킬 상세 설명 입력 (선택)">${skillDescription}</textarea>
            </div>
            <div class="desc-field">
                <label>⚡ 트라이포드 설명 (tripodDescription)</label>
                <textarea class="src-tripod-desc" rows="2" placeholder="트라이포드 효과 설명 입력 (선택)">${tripodDescription}</textarea>
            </div>
        </div>
    `;

    const imgPreview = div.querySelector('.skill-img-preview');
    const imgInput = div.querySelector('.src-img');
    const fileInput = div.querySelector('.src-file');
    const uploadBtn = div.querySelector('.btn-upload-img');

    // 이미지 URL 변경 시 프리뷰 업데이트
    imgInput.addEventListener('input', () => {
        const url = imgInput.value.trim();
        if (url) {
            imgPreview.src = url;
        } else {
            imgPreview.src = 'data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'32\' height=\'32\'><rect width=\'32\' height=\'32\' fill=\'%23333\'/><text x=\'50%\' y=\'50%\' fill=\'%23777\' font-size=\'10\' text-anchor=\'middle\' dy=\'.3em\'>NoImg</text></svg>';
        }
        updatePreview();
    });

    imgPreview.addEventListener('error', () => {
        imgPreview.src = 'data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'32\' height=\'32\'><rect width=\'32\' height=\'32\' fill=\'%23500\'/><text x=\'50%\' y=\'50%\' fill=\'%23fff\' font-size=\'9\' text-anchor=\'middle\' dy=\'.3em\'>Error</text></svg>';
    });

    // 파일 선택 버튼 클릭 시 input[type=file] 트리거
    uploadBtn.addEventListener('click', () => {
        fileInput.click();
    });

    // 파일 선택 시 서버로 즉시 업로드
    fileInput.addEventListener('change', async () => {
        const file = fileInput.files[0];
        if (!file) return;

        const formData = new FormData();
        formData.append('file', file);

        uploadBtn.disabled = true;
        uploadBtn.textContent = '업로드중...';

        try {
            const res = await fetch('/admin/upload/skill', {
                method: 'POST',
                body: formData
            });

            if (res.status === 401) {
                alert('로그인이 만료되었거나 로그인이 필요합니다.\n로그인 페이지로 이동합니다.');
                window.location.href = '/users/login';
                return;
            }

            if (res.status === 403) {
                alert('업로드 실패: 관리자(ADMIN) 권한이 필요한 작업입니다.');
                return;
            }

            if (!res.ok) {
                const errData = await res.json().catch(() => ({ message: '업로드 실패' }));
                throw new Error(errData.message || '업로드 실패');
            }

            const data = await res.json();
            imgInput.value = data.url;
            imgPreview.src = data.url;
            updatePreview();
        } catch (err) {
            console.error(err);
            alert('이미지 업로드 실패: ' + err.message);
        } finally {
            uploadBtn.disabled = false;
            uploadBtn.textContent = '파일선택';
            fileInput.value = '';
        }
    });

    div.querySelector('.btn-del-src').addEventListener('click', () => {
        div.remove();
        updatePreview();
    });

    div.querySelectorAll('input, textarea').forEach(inp => inp.addEventListener('input', updatePreview));
    container.appendChild(div);
}

// 폼 데이터를 DTO 형식으로 추출
function collectFormData() {
    const className = document.getElementById('input-className').value.trim();
    const selectVal = document.getElementById('select-classGroup').value;
    const customVal = document.getElementById('input-customClassGroup') ? document.getElementById('input-customClassGroup').value.trim() : '';
    const classGroup = (selectVal === 'DIRECT') ? (customVal || '기타') : selectVal;

    const classEngravings = [];
    document.querySelectorAll('.engraving-row').forEach(row => {
        const engravingName = row.querySelector('.eng-name').value.trim();
        const role = row.querySelector('.eng-role').value.trim();
        if (engravingName) {
            classEngravings.push({ engravingName, role });
        }
    });

    const skills = [];
    document.querySelectorAll('.skill-card').forEach(card => {
        const name = card.querySelector('.skill-synergy-name').value.trim();
        const priority = card.querySelector('.skill-priority').value.trim();

        const sources = [];
        card.querySelectorAll('.source-box').forEach(sBox => {
            const skillName = sBox.querySelector('.src-skill').value.trim();
            const skillImg = sBox.querySelector('.src-img').value.trim();
            const tripodName = sBox.querySelector('.src-tripod').value.trim();
            const tripodRate = sBox.querySelector('.src-tripodRate').value.trim();
            const baseRate = sBox.querySelector('.src-baseRate').value.trim();
            const skillDescription = sBox.querySelector('.src-skill-desc') ? sBox.querySelector('.src-skill-desc').value.trim() : '';
            const tripodDescription = sBox.querySelector('.src-tripod-desc') ? sBox.querySelector('.src-tripod-desc').value.trim() : '';

            if (skillName) {
                const sObj = { skillName };
                if (skillImg) sObj.skillImg = skillImg;
                if (skillDescription) sObj.skillDescription = skillDescription;
                if (tripodName) sObj.tripodName = tripodName;
                if (tripodRate) sObj.tripodRate = tripodRate;
                if (tripodDescription) sObj.tripodDescription = tripodDescription;
                if (baseRate) sObj.baseRate = baseRate;
                sources.push(sObj);
            }
        });

        if (name) {
            const skillObj = { name, sources };
            if (priority) skillObj.priority = priority;
            skills.push(skillObj);
        }
    });

    return {
        className,
        classGroup,
        classEngravings,
        skills
    };
}

function updatePreview() {
    const data = collectFormData();
    document.getElementById('json-preview').textContent = JSON.stringify(data, null, 2);
}

// 저장 요청
async function saveCurrentJob() {
    const data = collectFormData();
    if (!data.className) {
        alert('직업명을 입력해 주세요.');
        document.getElementById('input-className').focus();
        return;
    }

    try {
        const res = await fetch('/admin/synergies/api', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (res.status === 401) {
            alert('로그인이 만료되었거나 로그인이 필요합니다.\n로그인 페이지로 이동합니다.');
            window.location.href = '/users/login';
            return;
        }

        if (res.status === 403) {
            alert('저장 실패: 관리자(ADMIN) 권한이 없는 계정입니다.\n관리자 계정으로 로그인 후 다시 시도해 주세요.');
            return;
        }

        if (!res.ok) {
            let errorMsg = '저장 실패';
            try {
                const errorData = await res.json();
                errorMsg = errorData.message || errorData.error || errorMsg;
            } catch {
                errorMsg = (await res.text()) || errorMsg;
            }
            throw new Error(errorMsg);
        }

        alert(`${data.className} 시너지 정보가 성공적으로 저장되었습니다!`);
        await loadJobList();
    } catch (err) {
        console.error(err);
        alert('저장 중 오류 발생: ' + err.message);
    }
}

// 직업 삭제 요청 (논리 삭제 / Soft Delete)
async function deleteCurrentJob() {
    const classNameInput = document.getElementById('input-className').value.trim();
    const targetClassName = (activeJobData && activeJobData.className) ? activeJobData.className : classNameInput;

    if (!targetClassName) {
        alert('삭제할 직업이 선택되지 않았습니다. 목록에서 삭제할 직업을 선택해 주세요.');
        return;
    }

    // 목록에 실제 등록된 직업인지 검증
    const exists = currentJobList.some(job => job.className === targetClassName);
    if (!exists) {
        alert(`'${targetClassName}' 직업은 아직 DB에 저장되지 않았거나 목록에 존재하지 않습니다.`);
        return;
    }

    const confirmed = confirm(
        `[안내] 정말 '${targetClassName}' 직업을 삭제(휴지통 이동)하시겠습니까?\n\n` +
        `※ 삭제 시 파티 시너지 시뮬레이터에서 즉시 제외되며, 필요할 경우 좌측 '휴지통 🗑️' 탭에서 언제든 복원할 수 있습니다.`
    );
    if (!confirmed) {
        return;
    }

    try {
        const res = await fetch(`/admin/synergies/api/${encodeURIComponent(targetClassName)}`, {
            method: 'DELETE'
        });

        if (res.status === 401) {
            alert('로그인이 만료되었거나 로그인이 필요합니다.\n로그인 페이지로 이동합니다.');
            window.location.href = '/users/login';
            return;
        }

        if (res.status === 403) {
            alert('삭제 실패: 관리자(ADMIN) 권한이 없는 계정입니다.\n관리자 계정으로 로그인 후 다시 시도해 주세요.');
            return;
        }

        if (!res.ok) {
            let errorMsg = '삭제 실패';
            try {
                const errorData = await res.json();
                errorMsg = errorData.message || errorData.error || errorMsg;
            } catch {
                errorMsg = (await res.text()) || errorMsg;
            }
            throw new Error(errorMsg);
        }

        alert(`'${targetClassName}' 직업이 휴지통으로 이동되었습니다.\n(휴지통 탭에서 언제든 복원 가능)`);
        resetForm();
        await loadJobList();
    } catch (err) {
        console.error(err);
        alert('삭제 중 오류 발생: ' + err.message);
    }
}

// 삭제된 직업 복원 요청 (Restore)
async function restoreCurrentJob() {
    const targetClassName = (activeJobData && activeJobData.className) ? activeJobData.className : '';

    if (!targetClassName) {
        alert('복원할 직업을 휴지통 목록에서 선택해 주세요.');
        return;
    }

    const confirmed = confirm(`'${targetClassName}' 직업을 다시 활성 상태로 복원하시겠습니까?`);
    if (!confirmed) return;

    try {
        const res = await fetch(`/admin/synergies/api/${encodeURIComponent(targetClassName)}/restore`, {
            method: 'POST'
        });

        if (res.status === 401) {
            alert('로그인이 만료되었거나 로그인이 필요합니다.\n로그인 페이지로 이동합니다.');
            window.location.href = '/users/login';
            return;
        }

        if (res.status === 403) {
            alert('복원 실패: 관리자(ADMIN) 권한이 없는 계정입니다.\n관리자 계정으로 로그인 후 다시 시도해 주세요.');
            return;
        }

        if (!res.ok) {
            let errorMsg = '복원 실패';
            try {
                const errorData = await res.json();
                errorMsg = errorData.message || errorData.error || errorMsg;
            } catch {
                errorMsg = (await res.text()) || errorMsg;
            }
            throw new Error(errorMsg);
        }

        alert(`'${targetClassName}' 직업이 성공적으로 복원되었습니다!`);
        currentTab = 'active';
        updateTabButtons();
        resetForm();
        await loadJobList();
    } catch (err) {
        console.error(err);
        alert('복원 중 오류 발생: ' + err.message);
    }
}
