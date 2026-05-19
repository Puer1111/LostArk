import {raidConfigs} from '../character/raidConfigs.js';
import {classFunction} from './classFunction.js';
import {raidFunction} from './raidFunction.js';

/**
 * 실시간 레이드 파티 시뮬레이터 - 상태 관리 컨테이너
 * (캡슐화, 참조 유지, 타입 검증 담당)
 */
class RaidStateContainer {
    constructor() {
        this._state = {
            slots: Array(9).fill(null),
            selectedRaid: 'none',
            selectedSlotId: null
        };
    }

    // 외부에서는 이 getter를 통해 읽기 전용으로 접근하거나 참조 유지
    get state() {
        return this._state;
    }

    /**
     * 외부 데이터(LocalStorage, URL)를 안전하게 주입 (Sanitization)
     */
    hydrate(data) {
        if (!data || typeof data !== 'object') return;

        // 1. slots 검증 및 정제
        if (Array.isArray(data.slots) && data.slots.length === 9) {
            this._state.slots = data.slots.map(slot => {
                if (!slot || typeof slot !== 'object') return null;
                return {
                    jobName: typeof slot.jobName === 'string' ? slot.jobName : '',
                    activeEngravingIndex: Number.isInteger(slot.activeEngravingIndex) ? slot.activeEngravingIndex : 0,
                    searchData: (slot.searchData && typeof slot.searchData === 'object') ? slot.searchData : null
                };
            });
        }

        // 2. selectedRaid 검증
        if (typeof data.selectedRaid === 'string' && raidConfigs[data.selectedRaid]) {
            this._state.selectedRaid = data.selectedRaid;
        }

        // 3. selectedSlotId 검증 (정수 혹은 null만 허용)
        if (Number.isInteger(data.selectedSlotId) || data.selectedSlotId === null) {
            this._state.selectedSlotId = data.selectedSlotId;
        }
    }

    reset() {
        this._state.slots = Array(9).fill(null);
        this._state.selectedSlotId = null;
        this._state.selectedRaid = 'none';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const MAX_SLOTS = 8;
    const raidManager = new RaidStateContainer();

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
        // 커스텀 셀렉트 박스
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

        // 클래스 사이드바 & 검색 (매니저 전달)
        classFunction.initSidebarEvents(elements, raidManager, updateAll, saveToLocalStorage);

        // 슬롯 선택
        elements.raidBoard.addEventListener('click', (e) => {
            const slot = e.target.closest('.party-slot');
            if (!slot) return;
            const slotId = parseInt(slot.dataset.slotId);
            if (e.target.closest('.remove-btn') || e.target.closest('.role-btn')) return;

            raidFunction.handleSlotSelection(elements, raidManager, slotId);
        });

        // 레이드 설정 변경
        elements.categorySelect.addEventListener('change', (e) => {
            raidFunction.handleCategoryChange(e, elements, raidManager, updateAll);
        });

        elements.raidSelect.addEventListener('change', (e) => {
            raidFunction.handleRaidChange(e, raidManager, () => raidFunction.updateRaidLayout(elements, raidManager, MAX_SLOTS), updateAll);
        });

        elements.resetBtn.addEventListener('click', () => {
            if (confirm('모든 구성을 초기화하시겠습니까?')) {
                raidManager.reset();
                localStorage.removeItem('raidState_v1');
                updateAll();
            }
        });

        elements.shareBtn.addEventListener('click', handleShare);
    };

    const updateSlotsUI = () => {
        const { state } = raidManager;
        elements.partySlots.forEach(slot => {
            const slotId = parseInt(slot.dataset.slotId);
            const data = state.slots[slotId];
            const config = raidConfigs[state.selectedRaid] || raidConfigs['none'];

            if (slotId > config.maxPlayers) {
                slot.classList.add('disabled');
                slot.innerHTML = '<div class="slot-disabled">비활성</div>';
                slot.style.backgroundImage = 'none';
                return;
            }
            slot.classList.remove('disabled');

            if (!data) {
                slot.innerHTML = `<span>슬롯 ${slotId}</span>`;
                slot.style.backgroundImage = 'none';
                slot.classList.remove('has-character');
                slot.classList.toggle('selected', slotId === state.selectedSlotId);
                return;
            }

            classFunction.renderJobCard(slot, data.jobName, data.searchData, data.activeEngravingIndex, raidManager, updateAll, saveToLocalStorage);
            slot.classList.toggle('selected', slotId === state.selectedSlotId);
        });
    };

    const updateAll = () => {
        updateSlotsUI();
        raidFunction.analyzeSynergy(elements, raidManager);
    };

    const saveToLocalStorage = () => localStorage.setItem('raidState_v1', JSON.stringify(raidManager.state));
    
    const loadFromLocalStorage = () => {
        const saved = localStorage.getItem('raidState_v1');
        if (saved) {
            try {
                const loadedData = JSON.parse(saved);
                raidManager.hydrate(loadedData);
                
                const { state } = raidManager;
                if (state.selectedRaid !== 'none') {
                    const config = raidConfigs[state.selectedRaid];
                    if (config) {
                        // UI 수동 갱신 (이벤트 발생시키지 않음)
                        elements.categorySelect.value = config.category;
                        elements.categoryCustomBox.querySelector('.custom-select-trigger').textContent = config.category;
                        
                        // 레이드 목록 UI만 갱신
                        raidFunction.updateRaidOptionsUI(config.category, elements);
                        
                        elements.raidSelect.value = state.selectedRaid;
                        elements.raidCustomBox.querySelector('.custom-select-trigger').textContent = config.name;
                    }
                }
                raidFunction.updateRaidLayout(elements, raidManager, MAX_SLOTS);
            } catch (e) {
                console.error('로컬 스토리지 로드 실패', e);
            }
        }
    };

    const handleShare = () => {
        const stateStr = btoa(encodeURIComponent(JSON.stringify(raidManager.state)));
        const url = `${window.location.origin}${window.location.pathname}?state=${stateStr}`;
        navigator.clipboard.writeText(url).then(() => alert('공유 URL이 복사되었습니다.'));
    };

    const loadFromUrl = () => {
        const params = new URLSearchParams(window.location.search);
        const stateParam = params.get('state');
        if (stateParam) {
            try {
                const loadedData = JSON.parse(decodeURIComponent(atob(stateParam)));
                raidManager.hydrate(loadedData);

                const { state } = raidManager;
                if (state.selectedRaid !== 'none') {
                    const config = raidConfigs[state.selectedRaid];
                    if (config) {
                        // UI 수동 갱신
                        elements.categorySelect.value = config.category;
                        elements.categoryCustomBox.querySelector('.custom-select-trigger').textContent = config.category;
                        
                        raidFunction.updateRaidOptionsUI(config.category, elements);
                        
                        elements.raidSelect.value = state.selectedRaid;
                        elements.raidCustomBox.querySelector('.custom-select-trigger').textContent = config.name;
                    }
                }
                raidFunction.updateRaidLayout(elements, raidManager, MAX_SLOTS);
            } catch (e) {
                console.error('URL 데이터 로드 실패', e);
            }
        }
    };

    init();
});
