import { commonFunction } from '../common/commonFunction.js';

document.addEventListener('DOMContentLoaded', () => {
    const navItems = document.querySelectorAll('.nav-item');
    const marketContainer = document.querySelector('.market-container');

    navItems.forEach(item => {
        item.addEventListener('click', async (e) => {
            e.preventDefault();

            // 기존 활성화 클래스 제거 및 현재 항목 활성화
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');

            const categoryName = item.textContent;
            const categoryValue = item.getAttribute('data-category');

            // 공통 로딩 렌더링 함수 사용
            commonFunction.renderLoading(marketContainer, `${categoryName} 데이터를 조회 중입니다...`);

            // 2초 대기
            await new Promise(resolve => setTimeout(resolve, 1500));
            // 실제 API 호출
            try {
                const response = await fetch(`/market/api/items/${categoryValue}`);
                const data = await response.json();

                if (data && data.Items) {
                    updateMarketContent(categoryName, data.Items);
                } else {
                    marketContainer.innerHTML = `<p class="error-msg">데이터를 불러오는 중 오류가 발생했습니다.</p>`;
                }
            } catch (error) {
                console.error('API 호출 에러:', error);
                marketContainer.innerHTML = `<p class="error-msg">서버와의 통신이 원활하지 않습니다.</p>`;
            }
        });
    });

    function updateMarketContent(categoryName, items) {
        let html = `
            <h1 class="market-title">${categoryName} 실시간 시세</h1>
            <div class="market-items-grid">
        `;

        items.forEach(item => {
            html += `
                <div class="market-item-card ${item.Grade}">
                    <div class="item-icon">
                        <img src="${item.Icon}" alt="${item.Name}">
                    </div>
                    <div class="item-info">
                        <div class="item-name">${item.Name}</div>
                        <div class="item-grade">${item.Grade}</div>
                        <div class="item-price">
                            <span class="price-label">최저가:</span>
                            <span class="price-value">${item.CurrentMinPrice.toLocaleString()}</span>
                        </div>
                    </div>
                </div>
            `;
        });

        html += `</div>`;
        marketContainer.innerHTML = html;
    }
});
