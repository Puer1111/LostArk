import {raidConfigs} from '../character/raidConfigs.js';
import {jobData} from '../character/synergyDataV2.js';

export const raidFunction = {
    // 레이드 선택 옵션 초기화
    initRaidSelects: (elements) => {
        const categories = [...new Set(Object.values(raidConfigs).map(c => c.category))].filter(c => c !== '기본');
        elements.categoryOptions.innerHTML = '<div class="custom-option" data-value="none">종류 선택</div>';
        elements.categorySelect.innerHTML = '<option value="none">종류 선택</option>';

        categories.forEach(cat => {
            const option = document.createElement('option');
            option.value = cat;
            option.textContent = cat;
            elements.categorySelect.appendChild(option);

            const div = document.createElement('div');
            div.className = 'custom-option';
            div.dataset.value = cat;
            div.textContent = cat;
            elements.categoryOptions.appendChild(div);
        });
    },

    // 레이드 종류에 따른 레이아웃 업데이트 (파티 슬롯 노출 여부 등)
    updateRaidLayout: (elements, raidManager, MAX_SLOTS) => {
        const { state } = raidManager;
        const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];
        elements.partyContainers.forEach(container => {
            const partyId = parseInt(container.dataset.partyId);
            container.style.display = (partyId <= config.partyCount) ? 'block' : 'none';
        });
        elements.partyDetailSections.forEach((section, index) => {
            const partyId = index + 1;
            section.style.display = (partyId <= config.partyCount) ? 'block' : 'none';
        });
        for (let i = 1; i <= MAX_SLOTS; i++) {
            if (i > config.maxPlayers) state.slots[i] = null;
        }
    },

    // 슬롯 선택 핸들링
    handleSlotSelection: (elements, raidManager, slotId) => {
        const { state } = raidManager;
        const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];
        if (slotId > config.maxPlayers) return;

        // 선택 상태 토글 (매니저 내부 상태 업데이트)
        state.selectedSlotId = (state.selectedSlotId === slotId) ? null : slotId;

        elements.partySlots.forEach(slot => {
            const id = parseInt(slot.dataset.slotId);
            slot.classList.toggle('selected', id === state.selectedSlotId);
        });
    },

    // 시너지 분석 및 리포트 출력
    analyzeSynergy: (elements, raidManager) => {
        const { state } = raidManager;
        const partySynergies = [{}, {}, {}]; // [공격대 전체, 1파티, 2파티]
        const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];

        state.slots.forEach((data, i) => {
            if (!data || i > config.maxPlayers) return;
            const partyId = i <= 4 ? 1 : 2;
            
            const jobInfo = jobData.find(j => j.className === data.jobName);
            if (!jobInfo) return;

            const engraving = jobInfo.classEngravings[data.activeEngravingIndex || 0];

            [partyId, 0].forEach(pIdx => {
                engraving.skills.forEach(skill => {
                    if (skill.priority === 'main') {
                        if (!partySynergies[pIdx][skill.name]) partySynergies[pIdx][skill.name] = [];
                        partySynergies[pIdx][skill.name].push({jobName: data.jobName});
                    }
                });
            });
        });

        raidFunction.renderSynergyReport(elements, partySynergies);
        raidFunction.renderRecommendations(elements, raidManager, partySynergies[0]);
    },

    renderSynergyReport: (elements, partySynergies) => {
        raidFunction.renderSection(document.getElementById('synergy-party-1'), partySynergies[1]);
        raidFunction.renderSection(document.getElementById('synergy-party-2'), partySynergies[2]);
    },

    renderSection: (container, synergies) => {
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
                    ${isDuplicate ? `<span class="warn-icon">⚠️ 중복</span>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    },

    renderRecommendations: (elements, raidManager, totalSynergies) => {
        const { state } = raidManager;
        const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];
        const currentCount = state.slots.filter((s, i) => s && i <= config.maxPlayers).length;
        if (currentCount === 0) {
            elements.recommendationList.innerHTML = '<p>파티원을 추가하면 최적의 클래스를 추천해 드립니다.</p>';
            return;
        }
        const missingSynergies = [];
        const importantTypes = ['치명타 저항 감소', '피해 증가', '방어력 감소', '공격력 증가'];
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
    },

    // 카테고리 변경 시 레이드 목록 업데이트
    handleCategoryChange: (e, elements, raidManager, updateAll) => {
        const category = e.target.value;
        const { state } = raidManager;
        elements.raidSelect.innerHTML = '<option value="none">레이드 선택</option>';
        elements.raidOptions.innerHTML = '<div class="custom-option" data-value="none">레이드 선택</div>';
        elements.raidCustomBox.querySelector('.custom-select-trigger').textContent = '레이드 선택';

        if (category !== 'none') {
            Object.entries(raidConfigs).forEach(([key, config]) => {
                if (config.category === category) {
                    const option = document.createElement('option');
                    option.value = key;
                    option.textContent = config.name;
                    elements.raidSelect.appendChild(option);

                    const div = document.createElement('div');
                    div.className = 'custom-option';
                    div.dataset.value = key;
                    div.textContent = config.name;
                    elements.raidOptions.appendChild(div);
                }
            });
        }
        state.selectedRaid = 'none';
        updateAll();
    },

    // 레이드 선택 변경 시 처리
    handleRaidChange: (e, raidManager, updateRaidLayout, updateAll) => {
        const { state } = raidManager;
        state.selectedRaid = e.target.value;
        updateRaidLayout();
        updateAll();
    },

    closeAllCustomSelects: (elements) => {
        elements.categoryCustomBox?.classList.remove('open');
        elements.raidCustomBox?.classList.remove('open');
    }
};
