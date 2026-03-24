import {raidConfigs} from '../character/raidConfigs.js';

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
    updateRaidLayout: (elements, raidState, MAX_SLOTS) => {
        const config = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];
        elements.partyContainers.forEach(container => {
            const partyId = parseInt(container.dataset.partyId);
            container.style.display = (partyId <= config.partyCount) ? 'block' : 'none';
        });
        elements.partyDetailSections.forEach((section, index) => {
            const partyId = index + 1;
            section.style.display = (partyId <= config.partyCount) ? 'block' : 'none';
        });
        for (let i = 1; i <= MAX_SLOTS; i++) {
            if (i > config.maxPlayers) raidState.slots[i] = null;
        }
    },

    // 슬롯 선택 핸들링
    handleSlotSelection: (elements, raidState, slotId) => {
        const config = raidConfigs[raidState.selectedRaid] || raidConfigs['none'];
        if (slotId > config.maxPlayers) return;
        raidState.selectedSlotId = (raidState.selectedSlotId === slotId) ? null : slotId;
        elements.partySlots.forEach(slot => {
            const id = parseInt(slot.dataset.slotId);
            slot.classList.toggle('selected', id === raidState.selectedSlotId);
        });
    },

    // 카테고리 변경 시 레이드 목록 업데이트
    handleCategoryChange: (e, elements, raidState, updateAll) => {
        const category = e.target.value;
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
        raidState.selectedRaid = 'none';
        updateAll();
    },

    // 레이드 선택 변경 시 처리
    handleRaidChange: (e, raidState, updateRaidLayout, updateAll) => {
        raidState.selectedRaid = e.target.value;
        updateRaidLayout();
        updateAll();
    },

    closeAllCustomSelects: (elements) => {
        elements.categoryCustomBox?.classList.remove('open');
        elements.raidCustomBox?.classList.remove('open');
    }
};
