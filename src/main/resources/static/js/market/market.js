import {commonFunction} from '../common/commonFunction.js';

document.addEventListener('DOMContentLoaded', () => {
    const navItems = document.querySelectorAll('.nav-item');
    const contentArea = document.getElementById('market-content-area');
    const sortSelect = document.getElementById('sort-select');
    const sortContainer = document.querySelector('.sort-container');
    const marketTitle = document.querySelector('.market-title');
    const quickFilters = document.getElementById('quick-filters');

    let currentItems = [];
    let currentCategoryValue = '';

    // 탭별 퀵 필터 구성
    const tabConfig = {
        'Enhancement': [
            {label: '전체', filter: () => true},
            {label: '재련재료', filter: (item) => item.CategoryCode === 50010},
            {label: '재련추가재료', filter: (item) => item.CategoryCode === 50020 || item.CategoryCode === 51100},
            {label: '무기 추가재료', filter: (item) => item.CategoryCode === 51100},
            {label: '아크그리드 재료', filter: (item) => item.CategoryCode === 230000}
        ],
        'Gems': [
            {label: '전체', filter: () => true},
            {label: '겁화', filter: (item) => item.Name && item.Name.includes('겁화')},
            {label: '작열', filter: (item) => item.Name && item.Name.includes('작열')},
            {label: '멸화', filter: (item) => item.Name && item.Name.includes('멸화')},
            {label: '홍염', filter: (item) => item.Name && item.Name.includes('홍염')}
        ]
        // 다른 탭들도 필요시 여기에 추가 가능
    };

    // 등급 순서 정의 (높을수록 상위 등급)
    const gradeOrder = {
        '에스더': 7,
        '고대': 6,
        '유물': 5,
        '전설': 4,
        '영웅': 3,
        '희귀': 2,
        '고급': 1,
        '일반': 0
    };

    navItems.forEach(item => {
        item.addEventListener('click', async (e) => {
            e.preventDefault();

            // 기존 활성화 클래스 제거 및 현재 항목 활성화
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');

            const categoryName = item.textContent;
            currentCategoryValue = item.getAttribute('data-category');

            // 퀵 필터 영역 초기화
            quickFilters.innerHTML = '';

            // 공통 로딩 렌더링 함수 사용
            commonFunction.renderLoading(contentArea, `${categoryName} 데이터를 조회 중입니다...`);

            // 데이터 로딩 중에는 정렬 UI 숨김
            sortContainer.style.display = 'none';

            // 실제 API 호출
            try {
                const apiUrl = currentCategoryValue === 'Gems'
                    ? '/market/api/gems'
                    : `/market/api/items/${currentCategoryValue}`;

                const response = await fetch(apiUrl);
                const data = await response.json();

                if (data && data.Items) {
                    currentItems = data.Items;
                    marketTitle.textContent = `${categoryName} 시세`;
                    sortContainer.style.display = 'flex';
                    sortSelect.value = 'price-desc';

                    // 퀵 필터 버튼 생성
                    renderQuickFilters(currentCategoryValue);

                    renderItems(currentItems);
                } else {
                    contentArea.innerHTML = `<p class="error-msg">데이터를 불러오는 중 오류가 발생했습니다.</p>`;
                }
            } catch (error) {
                console.error('API 호출 에러:', error);
                contentArea.innerHTML = `<p class="error-msg">서버와의 통신이 원활하지 않습니다.</p>`;
            }
        });
    });

    // 퀵 필터 렌더링 함수
    function renderQuickFilters(category) {
        quickFilters.innerHTML = '';
        const configs = tabConfig[category];
        if (!configs) return;

        configs.forEach((config, index) => {
            const btn = document.createElement('button');
            btn.className = 'filter-btn';
            btn.textContent = config.label;
            if (index === 0) btn.classList.add('active'); // 첫 번째(전체) 버튼 활성화

            btn.addEventListener('click', () => {
                // 버튼 활성화 상태 변경
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                // 데이터 필터링 후 렌더링
                const filteredItems = currentItems.filter(config.filter);
                renderItems(filteredItems);
            });

            quickFilters.appendChild(btn);
        });
    }

    // 정렬 선택 이벤트
    sortSelect.addEventListener('change', () => {
        sortItems(sortSelect.value);
    });

    // 아이템 정렬 함수
    function sortItems(type) {
        // 현재 활성화된 필터 버튼 찾기
        const activeBtn = document.querySelector('.filter-btn.active');
        let itemsToSort = [...currentItems];

        if (activeBtn) {
            const config = tabConfig[currentCategoryValue]?.find(c => c.label === activeBtn.textContent);
            if (config) {
                itemsToSort = currentItems.filter(config.filter);
            }
        }

        switch (type) {
            case 'name':
                itemsToSort.sort((a, b) => a.Name.localeCompare(b.Name));
                break;
            case 'grade':
                itemsToSort.sort((a, b) => (gradeOrder[b.Grade] || 0) - (gradeOrder[a.Grade] || 0));
                break;
            case 'price-asc':
                itemsToSort.sort((a, b) => getPrice(a) - getPrice(b));
                break;
            case 'price-desc':
                itemsToSort.sort((a, b) => getPrice(b) - getPrice(a));
                break;
        }

        renderItems(itemsToSort);
    }

    // 가격 추출 헬퍼 함수
    function getPrice(item) {
        if (currentCategoryValue === 'Gems' && item.AuctionInfo) {
            return item.AuctionInfo.BuyPrice || 0;
        }
        return item.CurrentMinPrice || 0;
    }

    // 아이템 렌더링 함수
    function renderItems(items) {
        if (items.length === 0) {
            contentArea.innerHTML = `<p class="info-msg">표시할 아이템이 없습니다.</p>`;
            return;
        }

        let html = `<div class="market-items-grid">`;

        items.forEach((item, index) => {
            const isGem = currentCategoryValue === 'Gems';
            const price = getPrice(item);
            let priceLabel = isGem ? '즉시 구매가' : '최저가';

            // 백엔드에서 주입된 Signal 데이터 처리
            let signalHtml = '<div class="price-signal empty"></div>';
            if (item.Signal) {
                const signal = item.Signal;
                const statusClass = `signal-${signal.status.toLowerCase()}`;
                const diffText = signal.diffRate !== 0 ? `(${signal.diffRate > 0 ? '▲' : '▼'}${Math.abs(signal.diffRate)}%)` : '';
                signalHtml = `<div class="price-signal ${statusClass}">${signal.message} ${diffText}</div>`;
            }

            html += `
                <div class="market-item-card ${item.Grade}" data-item-name="${item.Name}" data-item-icon="${item.Icon}" data-item-grade="${item.Grade}">
                    <div class="item-icon">
                        <img src="${item.Icon}" alt="${item.Name}">
                    </div>
                    <div class="item-info">
                        <div class="item-name">${item.Name}</div>
                        <div class="item-grade">${item.Grade}</div>
                        ${signalHtml}
                        <div class="item-price">
                            <span class="price-label">${priceLabel}</span>
                            <span class="price-value">${price.toLocaleString()}</span>
                        </div>
                    </div>
                </div>
            `;
        });

        html += `</div>`;
        contentArea.innerHTML = html;

        // 카드가 렌더링된 후 아이템 클릭 이벤트 등록
        attachCardClickEvents();
    }

    // 모달 및 Chart.js 관련 요소
    const modal = document.getElementById('price-history-modal');
    const modalCloseBtn = document.getElementById('modal-close-btn');
    const modalItemIcon = document.getElementById('modal-item-icon');
    const modalItemName = document.getElementById('modal-item-name');
    const modalItemGrade = document.getElementById('modal-item-grade');
    let chartInstance = null;

    // 아이템 카드 클릭 이벤트 바인딩
    function attachCardClickEvents() {
        document.querySelectorAll('.market-item-card').forEach(card => {
            card.addEventListener('click', () => {
                const itemName = card.getAttribute('data-item-name');
                const itemIcon = card.getAttribute('data-item-icon');
                const itemGrade = card.getAttribute('data-item-grade');

                openPriceHistoryModal(itemName, itemIcon, itemGrade);
            });
        });
    }

    // 모달 닫기 이벤트
    if (modalCloseBtn) {
        modalCloseBtn.addEventListener('click', closeModal);
    }
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });
    }

    function closeModal() {
        if (modal) modal.style.display = 'none';
    }

    // 7일 시세 추이 모달 열기 및 차트 렌더링
    async function openPriceHistoryModal(itemName, itemIcon, itemGrade) {
        modalItemName.textContent = itemName;
        modalItemIcon.src = itemIcon;
        modalItemGrade.textContent = itemGrade;
        modal.style.display = 'flex';

        try {
            const response = await fetch(`/market/api/history/${encodeURIComponent(itemName)}?days=14`);
            const historyData = await response.json();

            renderChart(historyData);
        } catch (error) {
            console.error('시세 추이 조회 실패:', error);
        }
    }

    // Chart.js 렌더링 함수
    function renderChart(historyData) {
        const ctx = document.getElementById('priceHistoryChart').getContext('2d');

        if (chartInstance) {
            chartInstance.destroy(); // 기존 차트 파괴 후 재렌더링
        }

        const labels = historyData.map(d => d.summaryDate);
        const dataPoints = historyData.map(d => d.avgPrice);

        // 14일 간 최저가 계산
        const minVal = dataPoints.length > 0 ? Math.min(...dataPoints) : 0;
        // 1,000G(1k) 단위 내림 처리 (예: 2,300G -> 2,000G 시작)
        const yMin = Math.floor(minVal / 1000) * 1000;

        chartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels.length > 0 ? labels : ['데이터 없음'],
                datasets: [{
                    label: '일일 평균가 (골드)',
                    data: dataPoints.length > 0 ? dataPoints : [0],
                    borderColor: '#4a90e2',
                    backgroundColor: 'rgba(74, 144, 226, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.3,
                    pointBackgroundColor: '#4a90e2',
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `평균가: ${context.raw.toLocaleString()} 골드`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        }
                    },
                    y: {
                        min: yMin, // 14일 최저가를 기준으로 Y축 하단 고정
                        ticks: {
                            stepSize: 1000, // 1k (1,000G) 단위로 눈금 고정
                            callback: function(value) {
                                return value.toLocaleString() + ' G';
                            }
                        }
                    }
                }
            }
        });
    }
});
