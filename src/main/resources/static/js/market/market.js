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

    // 등급 순서 정의 (높은 등급이 먼저 오도록)
    const gradeOrder = {
        '고대': 7,
        '유물': 6,
        '전설': 5,
        '영웅': 4,
        '희귀': 3,
        '고급': 2,
        '일반': 1
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
                    sortSelect.value = 'name';

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

        items.forEach(item => {
            const isGem = currentCategoryValue === 'Gems';
            const price = getPrice(item);
            let priceLabel = isGem ? '즉시 구매가' : '최저가';

            // 백엔드에서 주입된 Signal 데이터 처리
            let signalHtml = '';
            if (item.Signal) {
                const signal = item.Signal;
                const statusClass = `signal-${signal.status.toLowerCase()}`;
                const diffText = signal.diffRate !== 0 ? `(${signal.diffRate > 0 ? '▲' : '▼'}${Math.abs(signal.diffRate)}%)` : '';
                signalHtml = `<div class="price-signal ${statusClass}">${signal.message} ${diffText}</div>`;
            }

            html += `
                <div class="market-item-card ${item.Grade}">
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
    }
});
