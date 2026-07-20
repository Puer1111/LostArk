/**
 * 사이드바 클래스 선택 및 드래그 기능 관련 모듈
 */
import {jobData} from "../character/synergyDataV3.js";
import {commonFunction} from "../common/commonFunction.js";

export const classFunction = {
    /**
     * 사이드바와 관련된 이벤트를 초기화합니다.
     */
    initSidebarEvents: (elements, raidManager, updateAll, saveToLocalStorage) => {
        if (!elements || !elements.classSelection) return;

        // 1. 사이드바 그룹(전사, 무도가 등) 열기/닫기
        elements.classSelection.addEventListener('click', (e) => {
            const header = e.target.closest('.class-group h3');
            if (header) {
                const jobList = document.getElementById('jobs-' + header.dataset.class);
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
        const jobInfo = jobData.find(job => job.className === jobName);
        if (!jobInfo) return;

        // 배경 설정
        if (searchData && searchData.characterImage) {
            slot.style.backgroundImage = `linear-gradient(rgba(0,0,0,0.1), rgba(0,0,0,0.4)), url('${searchData.characterImage}')`;
        } else {
            slot.style.backgroundImage = 'none';
        }

        const mainSynergies = jobInfo.skills ? jobInfo.skills.filter(s => s.priority === 'main') : [];

        slot.classList.add('has-character');
        
        let html = `
            <div class="character-card">
                <button class="remove-btn" title="제거">×</button>
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

        if (searchData) {
            html += `
                <div class="engraving-selector">
                    <span class="fixed-engraving">${jobInfo.classEngravings[activeIndex || 0].engravingName}</span>
                </div>
            `;
        } else {
            html += `
                <div class="engraving-selector">
                    ${jobInfo.classEngravings.map((eng, idx) => `
                        <button class="role-btn ${idx === (activeIndex || 0) ? 'active' : ''}" data-index="${idx}">${eng.engravingName}</button>
                    `).join('')}
                </div>
            `;
        }

        html += `
                    <div class="synergy-preview">
                        <div class="effect-tags">
                            ${mainSynergies.length > 0 
                                ? mainSynergies.map(skill => `<span class="effect-tag">${skill.name}</span>`).join('')
                                : '<span class="effect-tag none">메인 시너지 없음</span>'}
                        </div>
                    </div>
                </div>
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
};
