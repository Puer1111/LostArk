import {jobData} from '../character/synergyDataV2.js';
import {raidConfigs} from '../character/raidConfigs.js';
import {classFunction} from './classFunction.js';
import {raidFunction} from './raidFunction.js';

/**
 * 실시간 레이드 파티 시뮬레이터 & 추천기
 */
document.addEventListener('DOMContentLoaded', () => {
    const MAX_SLOTS = 8;

    let raidState = {
        slots: Array(9).fill(null), // 1~8번 슬롯 사용
        selectedRaid: 'none',
        selectedSlotId: null
    };

    const elements = {
        classSelection: document.getElementById('class-selection'),
        raidBoard: document.querySelector('.raid-board'),
        partySlots: document.querySelectorAll('.party-slot'),
        categorySelect: document.getElementById('category-select'),
        raidSelect: document.getElementById('raid-select'),
        categoryOptions: document.getElementById('category-options'),
        raidOptions: document.getElementById('raid-options'),
        categoryCustomBox: document.getElementById('category-custom-box'),
        raidCustomBox: document.getElementById('raid-custom-box'),
        resetBtn: document.getElementById('reset-btn'),
        shareBtn: document.getElementById('share-btn'),
        recommendationList: document.getElementById('recommendation-list'),
        totalSummary: document.getElementById('synergy-total-summary'),
        searchInput: document.getElementById('character-search-input'),
        searchBtn: document.getElementById('character-search-btn'),
        partyContainers: document.querySelectorAll('.party-container'),
        partyDetailSections: document.querySelectorAll('.party-detail-section')
    };

    const init = () => {
        document.querySelectorAll('.job-item').forEach(item => {
            item.setAttribute('draggable', 'true');
        });
        raidFunction.initRaidSelects(elements);
        bindEvents();
        loadFromLocalStorage();
        loadFromUrl();
        updateAll();
    };

    const bindEvents = () => {
        // 커스텀 드롭다운
        [elements.categoryCustomBox, elements.raidCustomBox].forEach(box => {
            if (!box) return;
            box.querySelector('.custom-select-trigger').addEventListener('click', (e) => {
                const isOpen = box.classList.contains('open');
                raidFunction.closeAllCustomSelects(elements);
                if (!isOpen) box.classList.add('open');
                e.stopPropagation();
            });
        });

        [elements.categoryOptions, elements.raidOptions].forEach(container => {
            if (!container) return;
            container.addEventListener('click', (e) => {
                const option = e.target.closest('.custom-option');
                if (!option) return;

                const value = option.dataset.value;
                const box = container.closest('.custom-select-box');
                const trigger = box.querySelector('.custom-select-trigger');
                const realSelect = box.querySelector('select');

                trigger.textContent = option.textContent;
                realSelect.value = value;
                realSelect.dispatchEvent(new Event('change'));
                raidFunction.closeAllCustomSelects(elements);
                e.stopPropagation();
            });
        });

        document.addEventListener('click', () => raidFunction.closeAllCustomSelects(elements));

        // 사이드바 기능 초기화 (classFunction.js)
        classFunction.initSidebarEvents(elements);

        elements.raidBoard.addEventListener('dragover', (e) => {
            const slot = e.target.closest('.party-slot');
            if (slot && !slot.classList.contains('disabled')) {
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
            if (slot && !slot.classList.contains('disabled')) {
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

            if (e.target.classList.contains('remove-btn')) {
                removeSlot(slotId);
                return;
            }
            if (e.target.classList.contains('role-btn')) {
                const index = parseInt(e.target.dataset.index);
                handleEngravingSwitch(slotId, index);
                return;
            }
            raidFunction.handleSlotSelection(elements, raidState, slotId);
        });

        elements.categorySelect.addEventListener('change', (e) => {
            raidFunction.handleCategoryChange(e, elements, raidState, updateAll);
        });

        elements.raidSelect.addEventListener('change', (e) => {
            raidFunction.handleRaidChange(e, raidState, () => raidFunction.updateRaidLayout(elements, raidState, MAX_SLOTS), updateAll);
        });

        elements.resetBtn.addEventListener('click', () => {
            if (confirm('모든 구성을 초기화하시겠습니까?')) {
                raidState.slots = Array(9).fill(null);
                raidState.selectedSlotId = null;
                localStorage.removeItem('raidState_v1');
                updateAll();
            }
        });

        elements.shareBtn.addEventListener('click', handleShare);
        elements.searchBtn.addEventListener('click', handleCharacterSearch);
        elements.searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleCharacterSearch();
        });
    };

    const updateSlotsUI = () => {
        elements.partySlots.forEach(slot => {
            const slotId = parseInt(slot.dataset.slotId);
            const data = raidState.slots[slotId];
            const config = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];

            if (slotId > config.maxPlayers) {
                slot.classList.add('disabled');
                slot.innerHTML = '<div class="slot-disabled">비활성</div>';
                slot.style.backgroundImage = 'none';
                return;
            }
            slot.classList.remove('disabled');

            if (!data) {
                slot.innerHTML = `<div class="slot-empty"><span>슬롯 ${slotId}</span></div>`;
                slot.style.backgroundImage = 'none';
                slot.classList.remove('has-character');
                return;
            }

            slot.classList.add('has-character');

            // 검색된 캐릭터 이미지 처리
            if (data.searchData && data.searchData.CharacterImage) {
                slot.style.backgroundImage = `linear-gradient(rgba(0,0,0,0.6), rgba(0,0,0,0.6)), url('${data.searchData.CharacterImage}')`;
            } else {
                slot.style.backgroundImage = 'none';
            }

            const currentSynergy = jobData[data.jobName].synergies[data.activeEngravingIndex];
            
            let html = `
                <div class="character-card">
                    <div class="card-header">
                        <div class="job-title-row">
                            <span class="job-name">${data.jobName}</span>
                            <button class="remove-btn" title="제거">×</button>
                        </div>
                        ${data.searchData ? `
                            <div class="search-info">
                                <span class="user-nickname">${data.searchData.CharacterName}</span>
                                <span class="item-level">Lv.${data.searchData.ItemAvgLevel}</span>
                            </div>
                        ` : ''}
                    </div>
                    
                    <div class="engraving-selector">
            `;

            jobData[data.jobName].synergies.forEach((syn, idx) => {
                const isActive = idx === data.activeEngravingIndex;
                html += `<button class="role-btn ${isActive ? 'active' : ''}" data-index="${idx}">${syn.name}</button>`;
            });

            html += `
                    </div>
                    <div class="synergy-preview">
                        <div class="preview-title">보유 시너지</div>
                        <div class="effect-tags">
            `;

            currentSynergy.effects.forEach(effect => {
                html += `<span class="effect-tag">${effect.type}</span>`;
            });

            html += `
                        </div>
                    </div>
                </div>
            `;
            slot.innerHTML = html;
        });
    };

    const handleCharacterSearch = async () => {
        const characterName = elements.searchInput.value.trim();
        if (!characterName) {
            alert('캐릭터명을 입력해주세요.');
            return;
        }

        elements.searchBtn.disabled = true;
        try {
            const response = await fetch(`/api/characters/${encodeURIComponent(characterName)}/profiles`);
            if (!response.ok) throw new Error('캐릭터를 찾을 수 없습니다.');

            const profile = await response.json();
            if (raidState.selectedSlotId) {
                addJobToSlot(raidState.selectedSlotId, profile.CharacterClassName, profile);
            } else {
                alert('캐릭터를 배치할 슬롯을 먼저 선택해주세요.');
            }
        } catch (error) {
            alert(error.message);
        } finally {
            elements.searchBtn.disabled = false;
        }
    };

    const addJobToSlot = (slotId, jobName, searchData = null) => {
        const config = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];
        if (slotId > config.maxPlayers) return;

        const jobInfo = jobData[jobName];
        if (!jobInfo) return;

        raidState.slots[slotId] = {
            jobName,
            engravings: jobInfo.synergies.map(s => s.name),
            activeEngravingIndex: 0,
            searchData: searchData
        };
        updateAll();
        saveToLocalStorage();
    };

    const removeSlot = (slotId) => {
        raidState.slots[slotId] = null;
        updateAll();
        saveToLocalStorage();
    };

    const handleEngravingSwitch = (slotId, index) => {
        if (raidState.slots[slotId]) {
            raidState.slots[slotId].activeEngravingIndex = index;
            updateAll();
            saveToLocalStorage();
        }
    };

    const updateAll = () => {
        updateSlotsUI();
        analyzeSynergy();
    };

    const analyzeSynergy = () => {
        const partySynergies = [{}, {}, {}];
        const raidConfigData = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];

        raidState.slots.forEach((data, i) => {
            if (!data || i > raidConfigData.maxPlayers) return;
            const partyId = i <= 4 ? 1 : 2;
            const synergy = jobData[data.jobName].synergies[data.activeEngravingIndex];

            [partyId, 0].forEach(pIdx => {
                synergy.effects.forEach(effect => {
                    if (!partySynergies[pIdx][effect.type]) partySynergies[pIdx][effect.type] = [];
                    partySynergies[pIdx][effect.type].push({jobName: data.jobName, value: effect.value});
                });
            });
        });
        renderSynergyReport(partySynergies);
        renderRecommendations(partySynergies[0]);
    };

    const renderSynergyReport = (partySynergies) => {
        renderSection(elements.totalSummary, partySynergies[0]);
        renderSection(document.getElementById('synergy-party-1'), partySynergies[1]);
        renderSection(document.getElementById('synergy-party-2'), partySynergies[2]);
    };

    const renderSection = (container, synergies) => {
        if (!container) return;
        if (!synergies || Object.keys(synergies).length === 0) {
            container.innerHTML = '<p class="empty-msg">정보 없음</p>';
            return;
        }
        let html = '<div class="synergy-list">';
        Object.entries(synergies).forEach(([type, items]) => {
            const isDuplicate = items.length > 1;
            html += `
                <div class="synergy-item ${isDuplicate ? 'duplicate' : ''}">
                    <span class="s-type">${type}</span>
                    <span class="s-value">${items[0].value}</span>
                    ${isDuplicate ? `<span class="warn-icon">⚠️</span>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    };

    const renderRecommendations = (totalSynergies) => {
        const config = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];
        const currentCount = raidState.slots.filter((s, i) => s && i <= config.maxPlayers).length;
        if (currentCount === 0) {
            elements.recommendationList.innerHTML = '<p>파티원을 추가하면 최적의 클래스를 추천해 드립니다.</p>';
            return;
        }
        const missingSynergies = [];
        const importantTypes = ['치명타 저항률 감소', '피해 증가', '방어력 감소', '공격력 증가'];
        importantTypes.forEach(type => {
            if (!totalSynergies[type]) missingSynergies.push(type);
        });
        let html = '<ul>';
        if (missingSynergies.length > 0) {
            missingSynergies.forEach(s => {
                html += `<li><strong>${s}</strong> 시너지가 부족합니다.</li>`;
            });
        } else {
            html += '<li>현재 주요 시너지가 잘 갖춰져 있습니다!</li>';
        }
        html += '</ul>';
        elements.recommendationList.innerHTML = html;
    };

    const saveToLocalStorage = () => localStorage.setItem('raidState_v1', JSON.stringify(raidState));
    const loadFromLocalStorage = () => {
        const saved = localStorage.getItem('raidState_v1');
        if (saved) {
            const loadedState = JSON.parse(saved);
            raidState = loadedState;
            if (raidState.selectedRaid !== 'none') {
                const config = raidConfigs[raidState.selectedRaid];
                if (config) {
                    elements.categorySelect.value = config.category;
                    elements.categoryCustomBox.querySelector('.custom-select-trigger').textContent = config.category;
                    elements.categorySelect.dispatchEvent(new Event('change'));
                    elements.raidSelect.value = raidState.selectedRaid;
                    elements.raidCustomBox.querySelector('.custom-select-trigger').textContent = config.name;
                }
            }
            raidFunction.updateRaidLayout(elements, raidState, MAX_SLOTS);
        }
    };

    const handleShare = () => {
        const stateStr = btoa(encodeURIComponent(JSON.stringify(raidState)));
        const url = `${window.location.origin}${window.location.pathname}?state=${stateStr}`;
        navigator.clipboard.writeText(url).then(() => alert('공유 URL이 복사되었습니다.'));
    };

    const loadFromUrl = () => {
        const params = new URLSearchParams(window.location.search);
        const stateParam = params.get('state');
        if (stateParam) {
            try {
                raidState = JSON.parse(decodeURIComponent(atob(stateParam)));
                if (raidState.selectedRaid !== 'none') {
                    const config = raidConfigs[raidState.selectedRaid];
                    if (config) {
                        elements.categorySelect.value = config.category;
                        elements.categoryCustomBox.querySelector('.custom-select-trigger').textContent = config.category;
                        elements.categorySelect.dispatchEvent(new Event('change'));
                        elements.raidSelect.value = raidState.selectedRaid;
                        elements.raidCustomBox.querySelector('.custom-select-trigger').textContent = config.name;
                    }
                }
                raidFunction.updateRaidLayout(elements, raidState, MAX_SLOTS);
            } catch (e) {
                console.error('URL 데이터 로드 실패', e);
            }
        }
    };

    init();
});
