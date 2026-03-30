import {raidConfigs} from '../character/raidConfigs.js';
import {classFunction} from './classFunction.js';
import {raidFunction} from './raidFunction.js';

/**
 * 실시간 레이드 파티 시뮬레이터 & 추천기 - 메인 오케스트레이터
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
        // 직업 아이템 드래그 설정
        document.querySelectorAll('.job-item').forEach(item => {
            item.setAttribute('draggable', 'true');
        });

        // 레이드 선택기 초기화
        raidFunction.initRaidSelects(elements);
        
        // 이벤트 바인딩
        bindEvents();
        
        // 데이터 로드
        loadFromLocalStorage();
        loadFromUrl();
        
        // 전체 UI 업데이트
        updateAll();
    };

    const bindEvents = () => {
        // 1. 커스텀 셀렉트 박스 이벤트 (레이드 관련)
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

        // 2. 클래스 사이드바 & 파티 슬롯 연계 기능 초기화 (classFunction)
        classFunction.initSidebarEvents(elements, raidState, updateAll, saveToLocalStorage);

        // 3. 레이드 보드 클릭 (슬롯 선택)
        elements.raidBoard.addEventListener('click', (e) => {
            const slot = e.target.closest('.party-slot');
            if (!slot) return;
            const slotId = parseInt(slot.dataset.slotId);

            // 카드 내부의 버튼(제거, 각인 변경) 클릭은 classFunction에서 처리하므로 무시
            if (e.target.closest('.remove-btn') || e.target.closest('.role-btn')) return;

            raidFunction.handleSlotSelection(elements, raidState, slotId);
        });

        // 4. 레이드 설정 변경
        elements.categorySelect.addEventListener('change', (e) => {
            raidFunction.handleCategoryChange(e, elements, raidState, updateAll);
        });

        elements.raidSelect.addEventListener('change', (e) => {
            raidFunction.handleRaidChange(e, raidState, () => raidFunction.updateRaidLayout(elements, raidState, MAX_SLOTS), updateAll);
        });

        // 5. 기타 액션
        elements.resetBtn.addEventListener('click', () => {
            if (confirm('모든 구성을 초기화하시겠습니까?')) {
                raidState.slots = Array(9).fill(null);
                raidState.selectedSlotId = null;
                localStorage.removeItem('raidState_v1');
                updateAll();
            }
        });

        elements.shareBtn.addEventListener('click', handleShare);
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
                slot.innerHTML = `<span>슬롯 ${slotId}</span>`;
                slot.style.backgroundImage = 'none';
                slot.classList.remove('has-character');
                // 선택 상태 유지
                slot.classList.toggle('selected', slotId === raidState.selectedSlotId);
                return;
            }

            // 직업 카드 렌더링 호출
            classFunction.renderJobCard(slot, data.jobName, data.searchData, data.activeEngravingIndex, raidState, updateAll, saveToLocalStorage);
            // 선택 상태 유지
            slot.classList.toggle('selected', slotId === raidState.selectedSlotId);
        });
    };

    const updateAll = () => {
        updateSlotsUI();
        raidFunction.analyzeSynergy(elements, raidState);
    };

    const saveToLocalStorage = () => localStorage.setItem('raidState_v1', JSON.stringify(raidState));
    
    const loadFromLocalStorage = () => {
        const saved = localStorage.getItem('raidState_v1');
        if (saved) {
            try {
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
            } catch (e) {
                console.error('로컬 스토리지 로드 실패', e);
            }
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
