// synergyDataV2.js에서 새로운 데이터 구조(jobData)를 가져옵니다.
import { jobData } from './synergyDataV2.js';

console.log('partySimulator.js 스크립트 로드 완료.');

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM 콘텐츠 로드 완료. 이벤트 리스너 설정 시작.');

    // --- DOM 요소 선택 ---
    const jobItems = document.querySelectorAll('.job-item');
    const partySlots = document.querySelectorAll('.party-slot');
    const classGroupHeaders = document.querySelectorAll('.class-group h3');
    const partyValidationMessageArea = document.getElementById('party-validation-message');
    const synergySummaryArea = document.getElementById('synergy-summary');

    // --- 상수 정의 ---
    const HYBRID_CLASSES = ['바드', '도화가', '홀리나이트'];
    const MAX_PARTY_SIZE = 4;

    // --- 아이콘 SVG 정의 ---
    const ICONS = {
        dps: `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 24 24"><path fill="none" stroke="#d81818" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 4v5l-9 7l-4 4l-3-3l4-4l7-9zM6.5 11.5l6 6"/></svg>`,
        supporter: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M19.5,10.5h-6v-6a1.5,1.5,0,0,0-3,0v6h-6a1.5,1.5,0,0,0,0,3h6v6a1.5,1.5,0,0,0,3,0v-6h6a1.5,1.5,0,0,0,0-3Z" fill="#4caf50"/></svg>`
    };

    // --- 이벤트 리스너 설정 ---

    classGroupHeaders.forEach(header => {
        header.addEventListener('click', () => toggleJobs(header.dataset.class));
    });

    jobItems.forEach(item => {
        item.setAttribute('draggable', 'true');
        item.addEventListener('dragstart', handleDragStart);
    });

    partySlots.forEach(slot => {
        slot.addEventListener('dragover', handleDragOver);
        slot.addEventListener('dragleave', handleDragLeave);
        slot.addEventListener('drop', handleDrop);
    });
    
    console.log('이벤트 리스너 설정 완료.');

    // --- 함수 정의 ---

    function toggleJobs(className) {
        const jobList = document.getElementById('jobs-' + className);
        if (!jobList) return;
        const isAlreadyOpen = jobList.style.display === 'block';
        document.querySelectorAll('.job-list').forEach(list => list.style.display = 'none');
        jobList.style.display = isAlreadyOpen ? 'none' : 'block';
    }

    function handleDragStart(event) {
        const jobName = event.target.textContent.trim();
        event.dataTransfer.setData('text/plain', jobName);
        event.dataTransfer.effectAllowed = 'copy';
        console.log(`[Drag Start] 직업: ${jobName}`);
    }

    function handleDragOver(event) {
        event.preventDefault(); // 드롭을 허용하기 위해 필수
        const slot = event.currentTarget;
        console.log(`[Drag Over] 슬롯: ${slot.dataset.slotId}`);
        if (slot.querySelector('span')) {
            slot.classList.add('drag-over');
        }
    }

    function handleDragLeave(event) {
        event.currentTarget.classList.remove('drag-over');
        console.log(`[Drag Leave] 슬롯: ${event.currentTarget.dataset.slotId}`);
    }

    function handleDrop(event) {
        event.preventDefault();
        const slot = event.currentTarget;
        const jobName = event.dataTransfer.getData('text/plain');
        console.log(`[Drop] 슬롯: ${slot.dataset.slotId}, 직업: ${jobName}`);
        
        slot.classList.remove('drag-over');

        if (slot.querySelector('.character-card')) {
            console.log('Drop 실패: 슬롯이 이미 채워져 있습니다.');
            return;
        }

        const currentParty = Array.from(document.querySelectorAll('.character-card')).map(card => card.dataset.class);
        if (currentParty.includes(jobName)) {
            alert('동일한 직업은 파티에 중복하여 추가할 수 없습니다.');
            console.log('Drop 실패: 중복된 직업입니다.');
            return;
        }
        
        console.log('addCharacterToSlot 함수 호출');
        addCharacterToSlot(slot, jobName);
        updatePartySynergies();
    }

    function addCharacterToSlot(slot, jobName) {
        console.log(`addCharacterToSlot: ${jobName}을(를) 슬롯 ${slot.dataset.slotId}에 추가 시도.`);
        const classData = jobData.find(j => j.className === jobName);
        if (!classData) {
            console.error(`데이터 오류: synergyDataV2.js에서 '${jobName}' 직업을 찾을 수 없습니다.`);
            return;
        }

        slot.innerHTML = '';

        const defaultRole = HYBRID_CLASSES.includes(jobName) ? '서포터' : '딜러';

        const card = document.createElement('div');
        card.className = 'character-card';
        card.dataset.class = jobName;
        card.dataset.role = defaultRole;

        const roleIcon = document.createElement('div');
        roleIcon.className = 'role-icon';
        roleIcon.innerHTML = defaultRole === '서포터' ? ICONS.supporter : ICONS.dps;

        const classDetails = document.createElement('div');
        classDetails.className = 'class-details';

        const classNameSpan = document.createElement('span');
        classNameSpan.className = 'class-name';
        classNameSpan.textContent = jobName;
        classDetails.appendChild(classNameSpan);

        if (HYBRID_CLASSES.includes(jobName)) {
            const roleSwitch = document.createElement('div');
            roleSwitch.className = 'role-switch';

            const supporterBtn = document.createElement('button');
            supporterBtn.className = 'role-btn supporter active';
            supporterBtn.dataset.role = '서포터';
            supporterBtn.textContent = '서포터';
            supporterBtn.addEventListener('click', handleRoleSwitch);

            const dpsBtn = document.createElement('button');
            dpsBtn.className = 'role-btn dps';
            dpsBtn.dataset.role = '딜러';
            dpsBtn.textContent = '딜러';
            dpsBtn.addEventListener('click', handleRoleSwitch);
            
            if (defaultRole === '서포터') {
                supporterBtn.classList.add('active');
                dpsBtn.classList.remove('active');
            } else {
                dpsBtn.classList.add('active');
                supporterBtn.classList.remove('active');
            }

            roleSwitch.appendChild(supporterBtn);
            roleSwitch.appendChild(dpsBtn);
            classDetails.appendChild(roleSwitch);
        }

        const removeBtn = document.createElement('button');
        removeBtn.className = 'remove-btn';
        removeBtn.innerHTML = '&times;';
        removeBtn.addEventListener('click', () => {
            slot.innerHTML = `<span>슬롯 ${slot.dataset.slotId}</span>`;
            updatePartySynergies();
        });

        card.appendChild(roleIcon);
        card.appendChild(classDetails);
        card.appendChild(removeBtn);
        
        slot.appendChild(card);
        console.log(`'${jobName}' 카드 생성 및 슬롯에 추가 완료.`);
    }

    function handleRoleSwitch(event) {
        const clickedBtn = event.currentTarget;
        const newRole = clickedBtn.dataset.role;
        const card = clickedBtn.closest('.character-card');
        const currentRole = card.dataset.role;

        if (newRole === currentRole) return;

        console.log(`[Role Switch] ${card.dataset.class}: ${currentRole} -> ${newRole}`);

        card.dataset.role = newRole;
        card.querySelector('.role-icon').innerHTML = newRole === '서포터' ? ICONS.supporter : ICONS.dps;

        card.querySelector('.role-switch .active').classList.remove('active');
        clickedBtn.classList.add('active');
        
        updatePartySynergies();
    }

    function updatePartySynergies() {
        console.log('파티 시너지 업데이트 시작.');
        const partyCards = document.querySelectorAll('.character-card');
        const synergyProviders = new Map();

        partyCards.forEach(card => {
            const className = card.dataset.class;
            const role = card.dataset.role;

            const classInfo = jobData.find(j => j.className === className);
            if (!classInfo) return;

            let engravingData;
            if (HYBRID_CLASSES.includes(className)) {
                const engravingMap = {
                    '바드': { '서포터': '절실한 구원', '딜러': '진실된 용맹' },
                    '도화가': { '서포터': '만개', '딜러': '회귀' },
                    '홀리나이트': { '서포터': '축복의 오라', '딜러': '심판자' }
                };
                const engravingName = engravingMap[className][role];
                engravingData = classInfo.classEngravings.find(e => e.engravingName === engravingName);
            } else {
                engravingData = classInfo.classEngravings[0];
            }

            if (engravingData && engravingData.skills) {
                engravingData.skills.forEach(synergyGroup => {
                    const synergyName = synergyGroup.name;
                    if (!synergyProviders.has(synergyName)) {
                        synergyProviders.set(synergyName, []);
                    }
                    if (!synergyProviders.get(synergyName).includes(className)) {
                        synergyProviders.get(synergyName).push(className);
                    }
                });
            }
        });

        renderSynergies(synergyProviders);
        checkPartyComposition();
        console.log('파티 시너지 업데이트 완료.');
    }

    function renderSynergies(synergyProviders) {
        synergySummaryArea.innerHTML = '';

        if (synergyProviders.size === 0) {
            synergySummaryArea.innerHTML = '<p style="color: #aaa;">파티원을 추가하여 시너지를 확인하세요.</p>';
            return;
        }

        const sortedSynergies = new Map([...synergyProviders.entries()].sort((a, b) => a[0].localeCompare(b[0], 'ko-KR')));

        const mainList = document.createElement('ul');
        mainList.className = 'synergy-main-list';

        sortedSynergies.forEach((providers, synergyName) => {
            const mainListItem = document.createElement('li');
            mainListItem.textContent = synergyName;

            const subList = document.createElement('ul');
            subList.className = 'synergy-provider-list';

            providers.sort((a, b) => a.localeCompare(b, 'ko-KR')).forEach(providerName => {
                const providerItem = document.createElement('li');
                providerItem.textContent = `- ${providerName}`;
                subList.appendChild(providerItem);
            });

            mainListItem.appendChild(subList);
            mainList.appendChild(mainListItem);
        });

        synergySummaryArea.appendChild(mainList);
    }

    function checkPartyComposition() {
        const partyCards = document.querySelectorAll('.character-card');
        partyValidationMessageArea.textContent = '';

        if (partyCards.length === MAX_PARTY_SIZE) {
            let supporterCount = 0;
            partyCards.forEach(card => {
                if (card.dataset.role === '서포터') {
                    supporterCount++;
                }
            });

            if (supporterCount === 0) {
                partyValidationMessageArea.textContent = "경고: 4인 파티에 서포터가 없습니다!";
            } else if (supporterCount > 1) {
                 partyValidationMessageArea.textContent = "경고: 4인 파티에 서포터가 너무 많습니다!";
            }
        }
    }
});
