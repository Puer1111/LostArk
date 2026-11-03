import {raidConfigs} from '../character/raidConfigs.js';
/**
 * 사이드바 클래스 선택 및 드래그 기능 관련 모듈
 */
import {synergyService} from "../character/synergyService.js";
import {commonFunction} from "../common/commonFunction.js";

export const classFunction = {
    /**
     * DB 시너지 데이터를 기반으로 좌측 사이드바 클래스 목록을 동적 렌더링합니다.
     */
    renderSidebar: (elements) => {
        const container = document.getElementById('class-groups-container') || elements.classSelection?.querySelector('.class-list-section');
        if (!container) return;

        const jobList = synergyService.getJobData();
        if (!jobList || jobList.length === 0) {
            container.innerHTML = '<p style="padding: 10px; color: #888; font-size: 0.85rem;">직업 데이터를 불러올 수 없습니다.</p>';
            return;
        }

        // 공식 클래스 군 정렬 우선순위
        const preferredOrder = ['전사', '무도가', '헌터', '마법사', '암살자', '스페셜리스트'];
        const slugMap = {
            '전사': 'warrior',
            '무도가': 'martial-artist',
            '헌터': 'hunter',
            '마법사': 'magician',
            '암살자': 'assassin',
            '스페셜리스트': 'specialist'
        };

        // 그룹별 직업 분류
        const grouped = new Map();
        preferredOrder.forEach(group => grouped.set(group, []));

        jobList.forEach(job => {
            const group = job.classGroup || '기타';
            if (!grouped.has(group)) {
                grouped.set(group, []);
            }
            grouped.get(group).push(job);
        });

        let html = '';
        grouped.forEach((jobs, groupName) => {
            if (jobs.length === 0) return;

            const groupSlug = slugMap[groupName] || `custom-${groupName}`;
            html += `
                <div class="class-group">
                    <h3 data-class="${groupSlug}">${groupName}</h3>
                    <div id="jobs-${groupSlug}" class="job-list" style="display: none;">
                        ${jobs.map(job => `
                            <div class="job-item" draggable="true">${job.className}</div>
                        `).join('')}
                    </div>
                </div>
            `;
        });

        if (container.id === 'class-groups-container') {
            container.innerHTML = html;
        } else {
            const existingGroups = container.querySelectorAll('.class-group');
            existingGroups.forEach(el => el.remove());
            container.insertAdjacentHTML('beforeend', html);
        }
    },

    /**
     * 사이드바와 관련된 이벤트를 초기화합니다.
     */
    initSidebarEvents: (elements, raidManager, updateAll, saveToLocalStorage) => {
        if (!elements || !elements.classSelection) return;

        // 1. 사이드바 그룹(전사, 무도가 등) 열기/닫기
        elements.classSelection.addEventListener('click', (e) => {
            const header = e.target.closest('.class-group h3');
            if (header) {
                const jobList = header.nextElementSibling || document.getElementById('jobs-' + header.dataset.class);
                if (!jobList) return;

                const isCurrentlyOpen = jobList.style.display === 'flex';
                document.querySelectorAll('.job-list').forEach(list => list.style.display = 'none');
                jobList.style.display = isCurrentlyOpen ? 'none' : 'flex';
            }
        });

        // 2. 클래스 아이템 드래그 시작
        elements.classSelection.addEventListener('dragstart', (e) => {
            const jobItem = e.target.closest('.job-item');
            if (jobItem) {
                e.dataTransfer.setData('text/plain', jobItem.textContent.trim());
                e.dataTransfer.effectAllowed = 'move';
            }
        });

        // 3. 파티 슬롯 드래그 오버 & 드롭 (통합 관리)
        if (elements.partySlots) {
            elements.partySlots.forEach(slot => {
                slot.addEventListener('dragover', (e) => {
                    if (!slot.classList.contains('disabled')) {
                        e.preventDefault();
                        slot.classList.add('drag-over');
                    }
                });

                slot.addEventListener('dragleave', () => {
                    slot.classList.remove('drag-over');
                });

                slot.addEventListener('drop', (e) => {
                    if (slot.classList.contains('disabled')) return;
                    
                    e.preventDefault();
                    e.stopPropagation();
                    slot.classList.remove('drag-over');
                    
                    const jobName = e.dataTransfer.getData('text/plain');
                    const slotId = parseInt(slot.dataset.slotId);
                    
                    // 해당 슬롯에 데이터 업데이트
                    raidManager.state.slots[slotId] = {
                        jobName: jobName,
                        activeEngravingIndex: 0,
                        searchData: null
                    };
                    
                    saveToLocalStorage();
                    updateAll();
                });
            });
        }

        // 4. 캐릭터 검색 기능
        if (elements.searchBtn && elements.searchInput) {
            const handleSearch = async () => {
                const characterName = elements.searchInput.value.trim();
                const { state } = raidManager;

                if (!characterName) {
                    alert('캐릭터명을 입력해주세요.');
                    return;
                }

                if (!state.selectedSlotId) {
                    alert('캐릭터를 배치할 슬롯을 먼저 선택해주세요.');
                    return;
                }

                try {
                    const apiUrl = `/character/api/simplified/${encodeURIComponent(characterName)}`;
                    const response = await fetch(apiUrl);
                    
                    const contentType = response.headers.get("content-type");
                    if (!response.ok || !contentType || !contentType.includes("application/json")) {
                        throw new Error('캐릭터 정보를 가져오는데 실패했습니다.');
                    }
                    
                    const characterData = await response.json();
                    const slotId = state.selectedSlotId;

                    // 상태 업데이트
                    state.slots[slotId] = {
                        jobName: characterData.characterClassName,
                        activeEngravingIndex: 0,
                        searchData: characterData
                    };
                    
                    saveToLocalStorage();
                    updateAll();
                } catch (error) {
                    console.error('검색 에러:', error);
                    alert(error.message);
                }
            };

            elements.searchBtn.addEventListener('click', handleSearch);
            elements.searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') handleSearch();
            });
        }
    },

    /**
     * 특정 슬롯에 직업/캐릭터 카드를 렌더링합니다.
     */
    renderJobCard: (slot, jobName, searchData = null, activeIndex = 0, raidManager, updateAll, saveToLocalStorage) => {
        const jobInfo = synergyService.findJobByName(jobName);
        if (!jobInfo) return;

        // 배경 설정 (우측 썸네일/좌측 썸네일 분리로 none 처리)
        slot.style.backgroundImage = 'none';

        const rawMainSkills = jobInfo.skills ? jobInfo.skills.filter(s => s.priority === 'main') : [];
        // 띄어쓰기 차이(예: "피해 감소" vs "피해감소")까지 고려한 정규화 중복 제거
        const seenNorms = new Set();
        const uniqueMainSynergies = [];
        rawMainSkills.forEach(skill => {
            if (!skill || !skill.name) return;
            const norm = skill.name.replace(/\s+/g, '');
            if (!seenNorms.has(norm)) {
                seenNorms.add(norm);
                uniqueMainSynergies.push(skill.name);
            }
        });

        slot.classList.add('has-character');
        
        let html = `
            <div class="character-card">
                <!-- 좌측 캐릭터 이미지 영역 (검색 데이터가 있을 때만 노출) -->
                ${(searchData && searchData.characterImage) ? `
                    <div class="character-image-box">
                        <img src="${searchData.characterImage}" alt="${searchData.characterName || ''}" class="character-profile-img" onerror="this.parentElement.style.display='none'">
                    </div>
                ` : ''}

                <!-- 우측 텍스트 정보 영역 -->
                <div class="card-bottom-content">
        `;

        if (searchData) {
            html += `
                <div class="search-info">
                    <div class="user-nickname">${searchData.characterName}</div>
                    <div class="combat-stats">Lv.${searchData.itemLevel} | ${searchData.combatPower}</div>
                </div>
            `;
        }

        html += `<div class="job-name">${jobInfo.className}</div>`;

        const classEngravings = jobInfo.classEngravings || [];

        if (searchData) {
            const currentEngravingName = (classEngravings[activeIndex || 0] && classEngravings[activeIndex || 0].engravingName) 
                ? classEngravings[activeIndex || 0].engravingName 
                : '각인 정보 없음';
            html += `
                <div class="engraving-selector">
                    <span class="fixed-engraving">${currentEngravingName}</span>
                </div>
            `;
        } else {
            html += `
                <div class="engraving-selector">
                    ${classEngravings.map((eng, idx) => `
                        <button class="role-btn ${idx === (activeIndex || 0) ? 'active' : ''}" data-index="${idx}">${eng.engravingName}</button>
                    `).join('')}
                </div>
            `;
        }

        html += `
                    <div class="synergy-preview">
                        <div class="effect-tags">
                            ${uniqueMainSynergies.length > 0 
                                ? uniqueMainSynergies.map(name => `<span class="effect-tag">${name}</span>`).join('')
                                : '<span class="effect-tag none">메인 시너지 없음</span>'}
                        </div>
                    </div>
                </div>

                <!-- 오른쪽 상단 제거 버튼 -->
                <button class="remove-btn" title="제거">×</button>
            </div>
        `;

        slot.innerHTML = html;

        // 5. 카드 내부 액션 (제거 버튼)
        slot.querySelector('.remove-btn').addEventListener('click', (e) => {
            const slotId = parseInt(slot.dataset.slotId);
            raidManager.state.slots[slotId] = null;
            saveToLocalStorage();
            updateAll();
            e.stopPropagation();
        });

        // 6. 각인 변경 버튼 (일반 드롭인 경우만)
        if (!searchData) {
            const roleBtns = slot.querySelectorAll('.role-btn');
            roleBtns.forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const index = parseInt(e.target.dataset.index);
                    const slotId = parseInt(slot.dataset.slotId);
                    
                    raidManager.state.slots[slotId].activeEngravingIndex = index;
                    saveToLocalStorage();
                    updateAll();
                    e.stopPropagation();
                });
            });
        }
    }
,
    /**
     * 슬롯에 배치된 캐릭터들의 시너지를 분석하여 상단 요약 바(시너지 추천/중복 경고)를 실시간 갱신합니다.
     */
    analyzeSynergy: (elements, raidManager) => {
        const { state } = raidManager;
        const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];
        const partySynergies = [{}, {}, {}]; // [0: 미사용, 1: 1파티, 2: 2파티]
        const partyCounts = [0, 0, 0];

        // 1. 파티별 시너지 수집 (직업 내 공백 정규화 중복 방지)
        state.slots.forEach((data, i) => {
            if (!data || i > config.maxPlayers) return;
            const partyId = i <= 4 ? 1 : 2;
            partyCounts[partyId]++;
            
            const jobInfo = synergyService.findJobByName(data.jobName);
            if (!jobInfo) return;

            const skills = jobInfo.skills || [];
            const seenNorm = new Set();
            skills.forEach(skill => {
                if (skill.priority === 'main' && skill.name) {
                    const norm = skill.name.replace(/\s+/g, '');
                    if (!seenNorm.has(norm)) {
                        seenNorm.add(norm);
                        if (!partySynergies[partyId][skill.name]) partySynergies[partyId][skill.name] = [];
                        partySynergies[partyId][skill.name].push({
                            jobName: data.jobName,
                            characterName: data.searchData ? data.searchData.characterName : null
                        });
                    }
                }
            });
        });

        // 2. 핵심 4대 딜 시너지 목록 및 약칭/정규화 매핑
        const importantSynergies = [
            { fullName: '치명타 저항 감소', shortName: '치적', norm: '치명타저항감소' },
            { fullName: '피해 증가', shortName: '피증', norm: '피해증가' },
            { fullName: '방어력 감소', shortName: '방깎', norm: '방어력감소' },
            { fullName: '공격력 증가', shortName: '공증', norm: '공격력증가' }
        ];

        // 3. 1파티 및 2파티 요약 바 실시간 갱신
        [1, 2].forEach(pId => {
            const recElem = document.getElementById(`summary-recommend-${pId}`);
            const dupElem = document.getElementById(`summary-dup-${pId}`);
            const synMap = partySynergies[pId];

            // 3-1. 시너지 추천: 파티에 없는 시너지 계산
            if (recElem) {
                if (partyCounts[pId] === 0) {
                    recElem.textContent = '파티원 추가 필요';
                    recElem.className = 'summary-val mute';
                    recElem.removeAttribute('title');
                } else {
                    const activeNorms = new Set(Object.keys(synMap).map(k => k.replace(/\s+/g, '')));
                    const missing = importantSynergies.filter(s => !activeNorms.has(s.norm));
                    if (missing.length === 0) {
                        recElem.textContent = '모두 보유';
                        recElem.className = 'summary-val';
                        recElem.title = '주요 4대 시너지를 모두 갖추었습니다.';
                    } else {
                        const shortList = missing.map(s => s.shortName).join(', ');
                        const fullList = missing.map(s => s.fullName).join(', ');
                        recElem.textContent = shortList;
                        recElem.className = 'summary-val acc';
                        recElem.title = `부족한 시너지: ${fullList}`;
                    }
                }
            }

            // 3-2. 중복 경고: 파티 내 동일 시너지 중복 보유 여부
            if (dupElem) {
                const duplicates = Object.keys(synMap).filter(name => synMap[name].length > 1);
                if (duplicates.length > 0) {
                    dupElem.textContent = duplicates.join(', ');
                    dupElem.className = 'summary-val bad';
                } else {
                    dupElem.textContent = '없음';
                    dupElem.className = 'summary-val mute';
                }
            }
        });
    }
};
