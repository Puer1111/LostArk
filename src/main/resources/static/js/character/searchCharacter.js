document.addEventListener("DOMContentLoaded", function () {
    // 아크 그리드 툴팁 처리 함수 호출
    processArkGridTooltips();

    // 젬 효과 집계 처리 함수 호출
    processGemEffectsAggregation();

    // 각인 아이콘 위치 적용 함수 호출
    applyEngravingIcons();

    // 탭 시스템 초기화
    initTabSystem();
});

function initTabSystem() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', function () {
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

        const contentContainer = tooltip.querySelector('.ItemPartBox .Element_001');
        if (!contentContainer) return;

        const rawHtml = contentContainer.innerHTML;
        const lines = rawHtml.split(/<br\s*\/?>/i);
        const pointPattern = /\[(\d+)[Pp]\]/;
        const percentagePattern = /\d+(\.\d+)?%/; // 숫자.숫자% 또는 숫자% 패턴

        const processedLines = lines.map(line => {
            if (line.trim() === '') return line;

            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = line;

            let requiredPoints = -1;
            // 라인 전체에서 [XXP] 패턴을 찾아 requiredPoints를 결정
            const linePointMatch = line.match(pointPattern);
            if (linePointMatch) {
                requiredPoints = parseInt(linePointMatch[1], 10);
            }

            const isActive = (requiredPoints === -1 || totalPoints >= requiredPoints);
            const targetColor = isActive ? '#FFFFFF' : '#808080';

            // 임시 div의 모든 자식 노드를 순회하며 색상 변경
            // NodeList는 실시간 컬렉션이므로, 변경 시 문제가 발생할 수 있어 Array.from으로 복사
            Array.from(tempDiv.childNodes).forEach(node => {
                if (node.nodeType === Node.TEXT_NODE) {
                    // 텍스트 노드인 경우, span으로 감싸서 색상 적용
                    const span = document.createElement('span');
                    span.style.color = targetColor;
                    span.textContent = node.textContent;
                    node.replaceWith(span);
                } else if (node.nodeType === Node.ELEMENT_NODE && node.tagName === 'FONT') {
                    const fontContent = node.textContent;
                    // [XXP] 또는 퍼센트 패턴을 포함하는 FONT 태그는 색상 변경하지 않음
                    if (!pointPattern.test(fontContent) && !percentagePattern.test(fontContent)) {
                        node.style.color = targetColor;
                    }
                }
            });
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
