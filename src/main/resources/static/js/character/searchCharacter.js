document.addEventListener("DOMContentLoaded", function () {
    // 아크 그리드 툴팁 처리 함수 호출
    processArkGridTooltips();

    // 아크 그리드 등급 색상 적용
    applyArkGridGradeColors();

    // 젬 효과 집계 처리 함수 호출
    processGemEffectsAggregation();

    // 각인 아이콘 위치 적용 함수 호출
    applyEngravingIcons();

    // 탭 시스템 초기화
    initTabSystem();

    // 아크 그리드 상세보기 버튼 초기화
    initArkGridDetailButtons();

    // 정보 갱신 버튼 초기화
    initRefreshButton();
});

/**
 * 정보 갱신 버튼 초기화 및 쿨타임 관리
 */
function initRefreshButton() {
    const refreshBtn = document.getElementById('refresh-btn');
    const cooldownText = document.getElementById('refresh-cooldown');
    const expeditionTab = document.getElementById('expedition');
    if (!refreshBtn || !expeditionTab) return;

    const characterName = expeditionTab.dataset.characterName;
    const cooldownKey = `refresh_cooldown_${characterName}`;
    const COOLDOWN_MS = 60 * 1000; // 1분

    // 쿨타임 상태 업데이트 함수
    const updateCooldownStatus = () => {
        const lastRefresh = localStorage.getItem(cooldownKey);
        if (lastRefresh) {
            const remaining = COOLDOWN_MS - (Date.now() - parseInt(lastRefresh));
            if (remaining > 0) {
                refreshBtn.disabled = true;
                const seconds = Math.ceil(remaining / 1000);
                cooldownText.textContent = `${seconds}초 후 다시 가능`;
                setTimeout(updateCooldownStatus, 1000);
                return true;
            }
        }
        refreshBtn.disabled = false;
        cooldownText.textContent = '';
        return false;
    };

    // 초기 상태 확인
    updateCooldownStatus();

    refreshBtn.addEventListener('click', async () => {
        if (refreshBtn.disabled) return;

        try {
            refreshBtn.disabled = true;
            refreshBtn.textContent = '갱신 중...';
            
            const response = await fetch(`/character/api/refresh/${encodeURIComponent(characterName)}`);
            if (!response.ok) throw new Error('Refresh failed');

            // 쿨타임 기록
            localStorage.setItem(cooldownKey, Date.now().toString());
            
            // 페이지 새로고침
            window.location.reload();
        } catch (error) {
            console.error('Failed to refresh character data:', error);
            alert('정보 갱신에 실패했습니다. 잠시 후 다시 시도해주세요.');
            refreshBtn.disabled = false;
            refreshBtn.innerHTML = '<span class="refresh-icon">🔄</span> 정보 갱신';
        }
    });
}

/**
 * 원정대 데이터를 비동기로 로드하여 렌더링함
 */
async function loadExpeditionData() {
    const expeditionTab = document.getElementById('expedition');
    if (!expeditionTab) return;

    const characterName = expeditionTab.dataset.characterName;
    const loadingDiv = document.getElementById('expedition-loading');
    const containerDiv = document.getElementById('expedition-container');
    const errorDiv = document.getElementById('expedition-error');

    try {
        const response = await fetch(`/character/api/expedition/${encodeURIComponent(characterName)}`);
        if (!response.ok) throw new Error('Network response was not ok');
        
        const expeditions = await response.json();
        
        if (!expeditions || expeditions.length === 0) {
            loadingDiv.style.display = 'none';
            errorDiv.querySelector('p').textContent = '원정대 정보가 없습니다.';
            errorDiv.style.display = 'block';
            return;
        }

        // 데이터 렌더링 (이미지와 전투력은 지연 로드 처리)
        containerDiv.innerHTML = expeditions.map(char => createExpeditionCardHtml(char)).join('');
        
        loadingDiv.style.display = 'none';
        containerDiv.style.display = 'grid';

        // 카드 상세 정보(이미지, 전투력) 지연 로드 시작
        lazyLoadCardDetails(expeditions);

    } catch (error) {
        console.error('Failed to load expedition data:', error);
        loadingDiv.style.display = 'none';
        errorDiv.style.display = 'block';
    }
}

/**
 * 원정대 카드 HTML 생성
 */
function createExpeditionCardHtml(char) {
    const bgStyle = (char.characterImage) ? `style="background-image: url('${char.characterImage}');"` : '';
    const combatPower = char.combatPower ? char.combatPower : '-';
    const charName = char.characterName || '';
    const href = charName ? `/character/${encodeURIComponent(charName)}` : '#';
    
    return `
        <a href="${href}" class="expedition-card" data-character-name="${charName}" ${bgStyle} style="text-decoration: none;">
            <div class="card-header">
                <div class="server-name">${char.serverName}</div>
            </div>
            <div class="card-body">
                <div class="info-row name-row">
                    <span class="label">이름</span>
                    <span class="value character-name">${charName || '-'}</span>
                </div>
                <div class="info-row">
                    <span class="label">클래스</span>
                    <span class="value">${char.characterClassName}</span>
                </div>
                <div class="info-row">
                    <span class="label">아이템 레벨</span>
                    <span class="value">${char.itemAvgLevel}</span>
                </div>
                <div class="info-row">
                    <span class="label">전투력</span>
                    <span class="value combat-power">${combatPower}</span>
                </div>
            </div>
        </a>
    `;
}

/**
 * 각 캐릭터 카드의 상세 정보(이미지, 전투력)를 서버 및 Open API 과부하 방지를 위해 순차적 지연 로드 수행
 */
async function lazyLoadCardDetails(expeditions) {
    const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
    
    for (const char of expeditions) {
        if (!char.characterName) continue;
        
        // 80ms 의 의도적인 딜레이를 주어 API Rate Limit (403, 429) 완전 방지
        await sleep(80);
        
        fetch(`/character/api/simplified/${encodeURIComponent(char.characterName)}`)
            .then(res => {
                if (!res.ok) throw new Error();
                return res.json();
            })
            .then(data => {
                const card = document.querySelector(`.expedition-card[data-character-name="${char.characterName}"]`);
                if (card) {
                    if (data.characterImage) {
                        card.style.backgroundImage = `url('${data.characterImage}')`;
                    }
                    const powerSpan = card.querySelector('.combat-power');
                    if (powerSpan && data.combatPower) {
                        powerSpan.textContent = data.combatPower;
                    }
                }
            })
            .catch(err => {
                console.warn(`Failed to lazy load profile image and combat power for: ${char.characterName}`);
            });
    }
}

function initArkGridDetailButtons() {
    // 이미 이벤트가 등록되어 있다면 다시 등록하지 않음 (중복 방지)
    if (document.arkGridEventRegistered) return;

    document.addEventListener('click', function (event) {
        // 1. 기존 아크 그리드 상세보기 토글
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
            return;
        }

        // 2. 아크 그리드 전체 보석 합산 토글
        const totalGemsBtn = event.target.closest('.arkGrid-total-gems-btn');
        if (totalGemsBtn) {
            event.preventDefault();
            totalGemsBtn.classList.toggle('active');
            
            const content = totalGemsBtn.nextElementSibling;
            if (content && content.classList.contains('arkGrid-total-gems-content')) {
                const isVisible = totalGemsBtn.classList.contains('active');
                content.style.display = isVisible ? 'block' : 'none';
                totalGemsBtn.querySelector('span').innerText = isVisible ? '📊 아크 그리드 전체 보석 합산 접기' : '📊 아크 그리드 전체 보석 합산 보기';
            }
        }
    });

    // 등록 완료 표시
    document.arkGridEventRegistered = true;
}

function initTabSystem() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    let expeditionLoaded = false; // 최초 1회 로드 체크용 플래그

    tabButtons.forEach(button => {
        button.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();

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

            // 아바타 탭 클릭 시 좌측 캐릭터 전신 이미지 카드 노출 제어
            const leftImageCard = document.getElementById('left-character-image-card');
            if (leftImageCard) {
                leftImageCard.style.display = (target === 'avatar') ? 'flex' : 'none';
            }

            // [원정대] 탭 클릭 시 최초 1회만 Lazy Loading 수행
            if (target === 'expedition' && !expeditionLoaded) {
                loadExpeditionData();
                expeditionLoaded = true;
            }
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

/**
 * 아크 그리드 아이템, 젬, 그리고 룬의 등급에 따라 아이콘 테두리 및 배경색 적용
 */
function applyArkGridGradeColors() {
    const gradeColorMap = {
        '에스더': '#3CF2E6',
        '고대': '#E3C7A1',
        '유물': '#FA5D00',
        '전설': '#F99200',
        '영웅': '#CE43FC',
        '희귀': '#00B5FF',
        '고급': '#91FE02',
        '일반': '#FFFFFF'
    };

    // 1. 아크 그리드 슬롯 아이콘 처리
    document.querySelectorAll('.arkGrid-item').forEach(item => {
        const gradeElement = item.querySelector('.arkGrid-grade');
        const iconImg = item.querySelector('.arkGrid-icon-wrapper img');
        const wrapper = item.querySelector('.arkGrid-icon-wrapper');
        
        if (gradeElement && iconImg) {
            const grade = gradeElement.textContent.trim();
            const color = gradeColorMap[grade];
            if (color) {
                iconImg.style.borderColor = color;
                iconImg.style.boxShadow = `0 0 8px ${color}66`;
                if (wrapper) {
                    wrapper.style.background = `radial-gradient(circle, ${color}44 0%, transparent 75%)`;
                }
            }
        }
    });

    // 2. 아크 그리드 젬 상세 정보 처리
    document.querySelectorAll('.arkGrid-gem-info').forEach(info => {
        const iconImg = info.querySelector('.arkGrid-gem-icon-img');
        if (iconImg) {
            const grade = iconImg.getAttribute('title');
            const color = gradeColorMap[grade];
            if (color) {
                info.style.borderLeft = `4px solid ${color}`;
                iconImg.style.borderColor = color;
                iconImg.style.boxShadow = `0 0 5px ${color}66`;
                info.style.background = `linear-gradient(90deg, ${color}15 0%, rgba(0,0,0,0.2) 100%)`;
            }
        }
    });

    // 3. 스킬 룬 아이콘 처리
    document.querySelectorAll('.rune-icon-img').forEach(img => {
        // classList에서 'rune-border-'로 시작하는 클래스를 찾아 등급 추출
        const gradeClass = Array.from(img.classList).find(c => c.startsWith('rune-border-'));
        if (gradeClass) {
            const grade = gradeClass.replace('rune-border-', '');
            const color = gradeColorMap[grade];
            if (color) {
                img.style.borderColor = color;
                // 전설 등급 이상의 경우 광채 효과 추가
                if (grade === '전설' || grade === '유물' || grade === '고대' || grade === '에스더') {
                    img.style.boxShadow = `0 0 5px ${color}`;
                }
            }
        }
    });
}

