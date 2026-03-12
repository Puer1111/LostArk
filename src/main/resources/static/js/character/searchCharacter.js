document.addEventListener("DOMContentLoaded", function () {
    // 아크 그리드 툴팁 처리 함수 호출
    processArkGridTooltips();

    // 젬 효과 집계 처리 함수 호출
    processGemEffectsAggregation();

    // 각인 아이콘 위치 적용 함수 호출
    applyEngravingIcons();

    // 탭 시스템 초기화
    initTabSystem();

    // 아크 그리드 상세보기 버튼 초기화
    initArkGridDetailButtons();

    // 원정대 카드 클릭 이벤트 초기화
    initExpeditionCards();
});

function initExpeditionCards() {
    const cards = document.querySelectorAll('.expedition-card');
    cards.forEach(card => {
        card.addEventListener('click', function (event) {
            // 클릭된 요소가 이미 a 태그이거나 a 태그의 자식인 경우 중복 이동 방지
            if (event.target.tagName === 'A' || event.target.closest('a')) return;

            const link = this.querySelector('.card-header a');
            if (link) {
                const href = link.getAttribute('href');
                // href가 유효한 캐릭터 이름을 포함하고 있는지 확인 (단순히 /character/ 인 경우 차단)
                if (href && href !== '/character' && href !== '/character/') {
                    window.location.href = href;
                }
            }
        });
    });
}

function initArkGridDetailButtons() {
    // 이미 이벤트가 등록되어 있다면 다시 등록하지 않음 (중복 방지)
    if (document.arkGridEventRegistered) return;

    document.addEventListener('click', function (event) {
        const button = event.target.closest('.arkGrid-detail-btn');
        if (button) {
            event.preventDefault();
            button.classList.toggle('active');

            // 버튼이 속한 arkGrid-slot-box 안에서 상세 패널을 찾음
            const slotBox = button.closest('.arkGrid-slot-box');
            if (slotBox) {
                const panel = slotBox.querySelector('.arkGrid-detail-panel');
                if (panel) {
                    // active 클래스 여부에 따라 보이기/숨기기
                    panel.style.display = button.classList.contains('active') ? 'block' : 'none';
                }
            }
            
            console.log('Button clicked, active class toggled:', button.classList.contains('active'));
        }
    });

    // 등록 완료 표시
    document.arkGridEventRegistered = true;
}

function initTabSystem() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', function (event) {
            event.preventDefault(); // 기본 동작(폼 제출 등) 차단
            event.stopPropagation(); // 이벤트 전파 차단 (원정대 카드 클릭 오작동 방지)
            
            const target = this.dataset.target;

            // 버튼 활성화 스타일 변경
            tabButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // 탭 내용 Show/Hide 처리
            tabContents.forEach(content => {
                if (content.id === target) {
                    content.style.display = 'block';
                } else {
                    content.style.display = 'none';
                }
            });
        });
    });
}


function processArkGridTooltips() {
    const tooltips = document.querySelectorAll('.ark-grid-tooltip');

    tooltips.forEach(tooltip => {
        const totalPoints = parseInt(tooltip.dataset.totalPoints, 10);
        if (isNaN(totalPoints)) return;

        // 컨테이너 찾기 (구조적 유연성 확보)
        let contentContainer = tooltip.querySelector('.ItemPartBox .Element_001') || 
                               tooltip.querySelector('.Element_001') || 
                               (tooltip.classList.contains('Element_001') ? tooltip : null) ||
                               tooltip;

        const rawHtml = contentContainer.innerHTML;
        const lines = rawHtml.split(/<br\s*\/?>/i);
        const pointPattern = /\[(\d+)[Pp]\]/;
        const percentagePattern = /\d+(\.\d+)?%/;

        let currentRequiredPoints = -1; // 포인트 요구사항을 다음 줄까지 유지하기 위한 변수

        const processedLines = lines.map(line => {
            if (line.trim() === '') return line;

            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = line;

            // 해당 줄에서 새로운 포인트 요구사항이 있는지 확인
            const linePointMatch = line.match(pointPattern);
            if (linePointMatch) {
                currentRequiredPoints = parseInt(linePointMatch[1], 10);
            }

            const isActive = (currentRequiredPoints === -1 || totalPoints >= currentRequiredPoints);
            // 비활성 상태일 때만 회색(#808080)을 적용하고, 활성 상태면 스타일을 건드리지 않음
            const targetColor = isActive ? null : '#808080';

            if (targetColor) {
                Array.from(tempDiv.childNodes).forEach(node => {
                    if (node.nodeType === Node.TEXT_NODE) {
                        if (node.textContent.trim() !== '') {
                            const span = document.createElement('span');
                            span.style.color = targetColor;
                            span.textContent = node.textContent;
                            node.replaceWith(span);
                        }
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        // 모든 하위 요소(FONT, SPAN 등)의 색상을 회색으로 덮어씀
                        node.style.color = targetColor;
                        node.querySelectorAll('*').forEach(el => {
                            el.style.color = targetColor;
                        });
                    }
                });
            }
            return tempDiv.innerHTML;
        });

        contentContainer.innerHTML = processedLines.join('<br>');
    });
}

function processGemEffectsAggregation() {
    const arkGridContainers = document.querySelectorAll('.arkGrid-item-container');
    const globalAggregatedEffects = {}; // Global aggregation object

    arkGridContainers.forEach(container => {
        const gemEffectSpans = container.querySelectorAll('.gem-effects-data span[data-gem-effects]');

        gemEffectSpans.forEach(span => {
            try {
                const gemEffects = JSON.parse(span.dataset.gemEffects); // This will be a Map<String, Integer>
                for (const effectName in gemEffects) {
                    if (gemEffects.hasOwnProperty(effectName)) {
                        globalAggregatedEffects[effectName] = (globalAggregatedEffects[effectName] || 0) + gemEffects[effectName];
                    }
                }
            } catch (e) {
                console.error("Error parsing gem effects JSON:", e);
            }
        });
    });

    // Display the global aggregated effects
    if (Object.keys(globalAggregatedEffects).length > 0) {
        // Display the global aggregated effects in the div class="section arkGrid"
        const arkGridSection = document.querySelector('.section.arkGrid');
        if (arkGridSection) {
            // Check if the global aggregated effects div already exists
            if (arkGridSection.querySelector('.global-aggregated-gem-effects')) {
                console.log("Global aggregated gem effects already displayed. Skipping re-render.");
                return; // Exit if already displayed
            }

            const globalAggregatedEffectsDiv = document.createElement('div');
            globalAggregatedEffectsDiv.classList.add('global-aggregated-gem-effects');

            let effectsHtml = "<strong>전체 젬 효과 집계:</strong><br>";
            for (const effectName in globalAggregatedEffects) {
                if (globalAggregatedEffects.hasOwnProperty(effectName)) {
                    effectsHtml += `<span>${effectName} Lv.${globalAggregatedEffects[effectName]}</span><br>`;
                }
            }
            globalAggregatedEffectsDiv.innerHTML = effectsHtml;
            // Insert at the beginning of the arkGridSection, after the arkGridList.
            const arkGridList = arkGridSection.querySelector('.arkGrid-list');
            if (arkGridList) {
                arkGridList.after(globalAggregatedEffectsDiv);
            } else {
                arkGridSection.prepend(globalAggregatedEffectsDiv);
            }
        } else {
            console.log("No .section.arkGrid found to display global aggregated gem effects.");
        }
    }
}

// 각인 등급에 따른 cdn 위치 조정.
function applyEngravingIcons() {
    const engravingIconMap = {
        // 여기에 다른 등급과 background-position 매핑을 추가하세요.
        '전설': '-84px 0',
        '유물': '-112px 0',

    };

    document.querySelectorAll('em.engraving-icon').forEach(emElement => {
        const parentDiv = emElement.closest('.engraving-item-content'); // Find the closest parent with this class
        if (parentDiv) {
            const grade = parentDiv.dataset.grade;
            if (grade && engravingIconMap[grade]) {
                emElement.style.backgroundPosition = engravingIconMap[grade];
            }

            const level = parentDiv.dataset.level;
            const engravingInfoGroup = emElement.closest('.engraving-info-group'); // Find the new wrapper div

            if (level && engravingInfoGroup) {
                // Prevent duplicate level spans
                if (!engravingInfoGroup.querySelector('.engraving-level')) {
                    const levelSpan = document.createElement('span');
                    levelSpan.textContent = "x "+level;
                    levelSpan.classList.add('engraving-level'); // Add a class for potential styling
                    engravingInfoGroup.appendChild(levelSpan); // Append to the new wrapper div
                }
            }
        }
    });
}
