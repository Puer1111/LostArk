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
            // Insert at the beginning of the arkGridSection, after the h3
            const h3Element = arkGridSection.querySelector('h3');
            if (h3Element) {
                h3Element.after(globalAggregatedEffectsDiv);
            } else {
                arkGridSection.prepend(globalAggregatedEffectsDiv);
            }
        } else {
            console.log("No .section.arkGrid found to display global aggregated gem effects.");
        }
    }
}


document.addEventListener("DOMContentLoaded", function () {
    // 캐릭터 조회 페이지 내의 '원정대' 버튼 기능
    const pageExpeditionBtn = document.getElementById('btn-expedition');
    if (pageExpeditionBtn) {
        pageExpeditionBtn.addEventListener("click", function () {
            // 페이지에 표시된 캐릭터 이름을 가져옵니다.
            const characterNameSpan = document.querySelector('.character-name');
            if (!characterNameSpan || !characterNameSpan.textContent) {
                alert("페이지의 캐릭터 이름을 찾을 수 없습니다.");
                return;
            }
            const characterName = characterNameSpan.textContent;
            window.location.href = `/character/expedition/${characterName}`;
        });
    }

    // 장비 툴팁 호버 기능
    const equipmentAreas = document.querySelectorAll('.equipment-area');

    equipmentAreas.forEach(area => {
        // 각 area 바로 앞에 있는 tooltip-content 요소를 찾습니다.
        const tooltip = area.previousElementSibling;

        // 해당 요소가 실제로 tooltip-content 클래스를 가지고 있는지 확인합니다.
        if (tooltip && tooltip.classList.contains('tooltip-content')) {
            area.addEventListener('mouseover', () => {
                tooltip.style.display = 'block';
            });

            area.addEventListener('mouseout', () => {
                tooltip.style.display = 'none';
            });
        }
    });

    // 아크 그리드 툴팁 처리 함수 호출
    processArkGridTooltips();

    // 젬 효과 집계 처리 함수 호출
    processGemEffectsAggregation();
});