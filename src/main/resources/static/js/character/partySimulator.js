import { jobData } from './synergyDataV2.js';

/**
 * 실시간 레이드 파티 시뮬레이터 & 추천기
 * 최적화 버전 (이벤트 위임 및 DOM 캐싱 적용)
 */
document.addEventListener('DOMContentLoaded', () => {
    // --- 상수 및 상태 관리 ---
    const MAX_SLOTS = 8;
    const HYBRID_CLASSES = ['바드', '도화가', '홀리나이트'];
    
    let raidState = {
        slots: Array(9).fill(null), // 1~8번 슬롯 사용
        selectedRaid: 'none'
    };

    // --- DOM 캐싱 ---
    const elements = {
        classSelection: document.getElementById('class-selection'),
        raidBoard: document.querySelector('.raid-board'),
        partySlots: document.querySelectorAll('.party-slot'),
        raidSelect: document.getElementById('raid-select'),
        resetBtn: document.getElementById('reset-btn'),
        shareBtn: document.getElementById('share-btn'),
        recommendationList: document.getElementById('recommendation-list'),
        totalSummary: document.getElementById('synergy-total-summary')
    };

    // --- 초기화 ---
    const init = () => {
        // 드래그 가능 속성 부여
        document.querySelectorAll('.job-item').forEach(item => {
            item.setAttribute('draggable', 'true');
        });
        
        bindEvents();
        loadFromUrl();
        updateAll();
    };

    // --- 이벤트 바인딩 (이벤트 위임 활용) ---
    const bindEvents = () => {
        // 1. 클래스 목록 클릭 (그룹 열기/닫기)
        elements.classSelection.addEventListener('click', (e) => {
            const header = e.target.closest('.class-group h3');
            if (header) {
                const jobList = document.getElementById('jobs-' + header.dataset.class);
                const isCurrentlyOpen = jobList.style.display === 'flex';
                
                document.querySelectorAll('.job-list').forEach(list => list.style.display = 'none');
                jobList.style.display = isCurrentlyOpen ? 'none' : 'flex';
            }
        });

        // 2. 드래그 시작 (클래스 아이템)
        elements.classSelection.addEventListener('dragstart', (e) => {
            if (e.target.classList.contains('job-item')) {
                e.dataTransfer.setData('text/plain', e.target.textContent.trim());
            }
        });

        // 3. 드롭 및 슬롯 내 이벤트 (공격대 구성판 전체 위임)
        elements.raidBoard.addEventListener('dragover', (e) => {
            const slot = e.target.closest('.party-slot');
            if (slot) {
                e.preventDefault();
                slot.classList.add('drag-over');
            }
        });

        elements.raidBoard.addEventListener('dragleave', (e) => {
            const slot = e.target.closest('.party-slot');
            if (slot) slot.classList.remove('drag-over');
        });

        elements.raidBoard.addEventListener('drop', (e) => {
            const slot = e.target.closest('.party-slot');
            if (slot) {
                e.preventDefault();
                slot.classList.remove('drag-over');
                const jobName = e.dataTransfer.getData('text/plain');
                addJobToSlot(parseInt(slot.dataset.slotId), jobName);
            }
        });

        elements.raidBoard.addEventListener('click', (e) => {
            const slot = e.target.closest('.party-slot');
            if (!slot) return;
            
            const slotId = parseInt(slot.dataset.slotId);

            // 삭제 버튼 클릭
            if (e.target.classList.contains('remove-btn')) {
                removeSlot(slotId);
            }
            // 각인/역할 버튼 클릭
            else if (e.target.classList.contains('role-btn')) {
                const index = parseInt(e.target.dataset.index);
                handleEngravingSwitch(slotId, index);
            }
        });

        // 4. 기타 컨트롤
        elements.raidSelect.addEventListener('change', (e) => {
            raidState.selectedRaid = e.target.value;
            updateAll();
        });

        elements.resetBtn.addEventListener('click', () => {
            if (confirm('모든 구성을 초기화하시겠습니까?')) {
                raidState.slots = Array(9).fill(null);
                updateAll();
            }
        });

        elements.shareBtn.addEventListener('click', handleShare);
    };

    // --- 비즈니스 로직 ---
    const addJobToSlot = (slotId, jobName) => {
        const classData = jobData.find(j => j.className === jobName);
        if (!classData) return;

        raidState.slots[slotId] = {
            className: jobName,
            selectedEngravingIndex: 0,
            data: classData
        };
        updateAll();
    };

    const handleEngravingSwitch = (slotId, index) => {
        if (raidState.slots[slotId]) {
            raidState.slots[slotId].selectedEngravingIndex = index;
            updateAll();
        }
    };

    const removeSlot = (slotId) => {
        raidState.slots[slotId] = null;
        updateAll();
    };

    const updateAll = () => {
        renderSlots();
        const partySynergies = [calculateSynergy(1), calculateSynergy(2)];
        renderSynergySummary(partySynergies);
        renderRecommendations(partySynergies);
        updateUrl();
    };

    // --- 렌더링 엔진 ---
    const renderSlots = () => {
        elements.partySlots.forEach(slot => {
            const slotId = parseInt(slot.dataset.slotId);
            const state = raidState.slots[slotId];

            if (!state) {
                slot.innerHTML = `<span>슬롯 ${slotId}</span>`;
                return;
            }

            const currentEng = state.data.classEngravings[state.selectedEngravingIndex];
            const role = currentEng.role === '서포터' ? '서포터' : '딜러';

            // 시너지 태그 생성 (priority: 'main'인 것만, 중복 이름 제거)
            const synergyTags = currentEng.skills
                .filter(skill => skill.priority === 'main')
                .reduce((acc, skill) => {
                    if (!acc.find(s => s.name === skill.name)) acc.push(skill);
                    return acc;
                }, [])
                .map(skill => {
                    const isAwk = skill.sources && skill.sources.some(src => src.isAwakening);
                    const shortName = skill.name.split(' ')[0]; // '치명타 저항 감소' -> '치명타'
                    return `<span class="synergy-tag ${isAwk ? 'awk' : ''}">${isAwk ? '[각성] ' : ''}${shortName}</span>`;
                }).join('');

            slot.innerHTML = `
                <div class="character-card" data-role="${role}">
                    <div class="role-icon">${getRoleIcon(role)}</div>
                    <div class="class-details">
                        <div class="name-row">
                            <span class="class-name">${state.className}</span>
                            <div class="synergy-tag-list">${synergyTags}</div>
                        </div>
                        <div class="role-info-area">
                            <div class="role-switch">
                                ${state.data.classEngravings.map((eng, idx) => `
                                    <button class="role-btn ${state.selectedEngravingIndex === idx ? 'active' : ''}" 
                                            data-index="${idx}">${eng.engravingName}</button>
                                `).join('')}
                            </div>
                        </div>
                    </div>
                    <button class="remove-btn">&times;</button>
                </div>
            `;
        });
    };

    const calculateSynergy = (partyNum) => {
        const offset = partyNum === 1 ? 1 : 5;
        const members = raidState.slots.slice(offset, offset + 4).filter(s => s !== null);
        
        let stats = { critRate: 0, dmgIncrease: 0, defReduction: 0, backHeadDmg: 0, 
                      atkSpeed: 0, movSpeed: 0, hasSupporter: false, hasBrand: false };

        members.forEach(m => {
            const eng = m.data.classEngravings[m.selectedEngravingIndex];
            if (eng.role === '서포터') stats.hasSupporter = true;

            eng.skills.forEach(skill => {
                const name = skill.name;
                const value = parseFloat((skill.sources[0]?.tripodRate || skill.sources[0]?.baseRate || "0").replace(/[^0-9.]/g, ""));

                if (name.includes('치명타')) stats.critRate += value;
                else if (name.includes('피해 증가')) {
                    if (eng.role === '서포터') stats.hasBrand = true;
                    else stats.dmgIncrease += value;
                }
                else if (name.includes('받는 피해')) stats.dmgIncrease += value;
                else if (name.includes('방어력')) stats.defReduction += value;
                else if (name.includes('백/헤드')) stats.backHeadDmg += value;
                else if (name.includes('공격속도')) stats.atkSpeed += value;
                else if (name.includes('이동속도')) stats.movSpeed += value;
            });
        });

        if (stats.hasBrand) stats.dmgIncrease += 10;
        return stats;
    };

    const renderSynergySummary = (partySynergies) => {
        partySynergies.forEach((syn, i) => {
            const container = document.getElementById(`synergy-party-${i + 1}`);
            if (syn.critRate === 0 && syn.dmgIncrease === 0 && syn.defReduction === 0) {
                container.innerHTML = '<p class="empty-msg">파티원을 추가하세요.</p>';
                return;
            }

            container.innerHTML = `
                <div class="synergy-badge-list">
                    <span class="badge ${syn.critRate > 0 ? 'active' : ''}">치적 +${syn.critRate}%</span>
                    <span class="badge ${syn.dmgIncrease > 0 ? 'active' : ''}">피증 +${syn.dmgIncrease}%</span>
                    <span class="badge ${syn.defReduction > 0 ? 'active' : ''}">방깎 +${syn.defReduction}%</span>
                    <span class="badge ${syn.hasSupporter ? 'active' : 'warn'}">${syn.hasSupporter ? '서포터 유' : '서포터 무'}</span>
                </div>
            `;
        });

        elements.totalSummary.innerHTML = `
            <div class="total-stats">
                <p>총 파티원: ${raidState.slots.filter(s => s !== null).length} / 8</p>
                <p>서포터 수: ${raidState.slots.filter(s => s?.data.classEngravings[s.selectedEngravingIndex].role === '서포터').length} 명</p>
            </div>
        `;
    };

    const renderRecommendations = (partySynergies) => {
        elements.recommendationList.innerHTML = '';
        partySynergies.forEach((syn, i) => {
            const partyName = `${i + 1}파티`;
            let tips = [];
            if (!syn.hasSupporter) tips.push(`⚠️ ${partyName}: 서포터가 필요합니다.`);
            if (syn.critRate < 10) tips.push(`💡 ${partyName}: 치적 시너지 추천`);
            if (syn.defReduction === 0) tips.push(`💡 ${partyName}: 방깎 시너지 추천`);

            tips.forEach(t => {
                const div = document.createElement('div');
                div.className = 'rec-item';
                div.textContent = t;
                elements.recommendationList.appendChild(div);
            });
        });
        if (!elements.recommendationList.innerHTML) elements.recommendationList.innerHTML = '<p>파티 구성이 안정적입니다!</p>';
    };

    // --- 유틸리티 및 공유 ---
    const getRoleIcon = (role) => {
        return role === '서포터' 
            ? `<svg viewBox="0 0 24 24" fill="none"><path d="M19.5,10.5h-6v-6a1.5,1.5,0,0,0-3,0v6h-6a1.5,1.5,0,0,0,0,3h6v6a1.5,1.5,0,0,0,3,0v-6h6a1.5,1.5,0,0,0,0-3Z" fill="#4caf50"/></svg>`
            : `<svg viewBox="0 0 24 24" fill="none" stroke="#d81818" stroke-width="2"><path d="M20 4v5l-9 7l-4 4l-3-3l4-4l7-9zM6.5 11.5l6 6" stroke-linecap="round"/></svg>`;
    };

    const updateUrl = () => {
        const params = new URLSearchParams();
        raidState.slots.forEach((s, i) => {
            if (s) params.append(`s${i}`, `${s.className}|${s.selectedEngravingIndex}`);
        });
        window.history.replaceState({}, '', `${location.origin}${location.pathname}?${params}`);
        return window.location.href;
    };

    const loadFromUrl = () => {
        const params = new URLSearchParams(window.location.search);
        for (let i = 1; i <= MAX_SLOTS; i++) {
            const val = params.get(`s${i}`);
            if (val) {
                const [name, idx] = val.split('|');
                const data = jobData.find(j => j.className === name);
                if (data) raidState.slots[i] = { className: name, selectedEngravingIndex: parseInt(idx) || 0, data };
            }
        }
    };

    const handleShare = () => {
        navigator.clipboard.writeText(updateUrl()).then(() => alert('공유 링크가 복사되었습니다.'));
    };

    init();
});
