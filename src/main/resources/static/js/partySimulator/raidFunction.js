const selectedRaid = (raidManager) => raidManager.getSelectedRaidConfig();

function appendRaidOption(elements, raid) {
    const option = document.createElement('option');
    option.value = String(raid.id);
    option.textContent = raid.raidName;
    elements.raidSelect.appendChild(option);

    const customOption = document.createElement('div');
    customOption.className = 'custom-option';
    customOption.dataset.value = String(raid.id);
    customOption.textContent = raid.raidName;
    elements.raidOptions.appendChild(customOption);
}

export const raidFunction = {
    fetchRaidData: async () => {
        const response = await fetch('/character/api/raids');
        if (!response.ok) {
            throw new Error(`Raid data request failed (${response.status})`);
        }
        return response.json();
    },

    initRaidSelects: (elements, raids) => {
        const categories = [...new Set(raids.map((raid) => raid.raidCategory))];
        elements.categoryOptions.replaceChildren();
        elements.categorySelect.length = 1;
        elements.categoryOptions.appendChild(createPlaceholder(elements.categorySelect.options[0]));

        categories.forEach((category) => {
            const option = document.createElement('option');
            option.value = category;
            option.textContent = category;
            elements.categorySelect.appendChild(option);

            const customOption = document.createElement('div');
            customOption.className = 'custom-option';
            customOption.dataset.value = category;
            customOption.textContent = category;
            elements.categoryOptions.appendChild(customOption);
        });
    },

    updateRaidOptionsUI: (category, elements, raids) => {
        elements.raidSelect.length = 1;
        elements.raidOptions.replaceChildren();
        elements.raidOptions.appendChild(createPlaceholder(elements.raidSelect.options[0]));
        raids.filter((raid) => raid.raidCategory === category)
                .forEach((raid) => appendRaidOption(elements, raid));
    },

    updateRaidLayout: (elements, raidManager, MAX_SLOTS) => {
        const config = selectedRaid(raidManager);
        elements.partyContainers.forEach((container) => {
            const partyId = Number.parseInt(container.dataset.partyId, 10);
            container.style.display = partyId <= config.partyCount ? 'block' : 'none';
        });
        elements.partyDetailSections?.forEach((section, index) => {
            section.style.display = index + 1 <= config.partyCount ? 'block' : 'none';
        });
        for (let slotId = 1; slotId <= MAX_SLOTS; slotId++) {
            if (slotId > config.memberCount) raidManager.state.slots[slotId] = null;
        }
    },

    handleSlotSelection: (elements, raidManager, slotId) => {
        const config = selectedRaid(raidManager);
        if (slotId > config.memberCount) return;

        const { state } = raidManager;
        state.selectedSlotId = state.selectedSlotId === slotId ? null : slotId;
        elements.partySlots.forEach((slot) => {
            const id = Number.parseInt(slot.dataset.slotId, 10);
            slot.classList.toggle('selected', id === state.selectedSlotId);
        });
    },

    handleCategoryChange: (event, elements, raidManager, raids, updateAll) => {
        const category = event.target.value;
        raidFunction.updateRaidOptionsUI(category, elements, raids);
        elements.raidCustomBox.querySelector('.custom-select-trigger').textContent =
                elements.raidSelect.options[0].textContent;
        raidManager.state.selectedRaid = 'none';
        updateAll();
    },

    handleRaidChange: (event, raidManager, updateRaidLayout, updateAll) => {
        raidManager.state.selectedRaid = event.target.value;
        updateRaidLayout();
        updateAll();
    },

    closeAllCustomSelects: (elements) => {
        elements.categoryCustomBox?.classList.remove('open');
        elements.raidCustomBox?.classList.remove('open');
    }
};

function createPlaceholder(option) {
    const customOption = document.createElement('div');
    customOption.className = 'custom-option';
    customOption.dataset.value = option.value;
    customOption.textContent = option.textContent;
    return customOption;
}
