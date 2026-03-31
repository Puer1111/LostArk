/**
 * 공통 유틸리티 함수 모듈
 */
export const commonFunction = {
    /**
     * 특정 컨테이너에 로딩 상태를 렌더링합니다.
     * @param {HTMLElement} container - 로딩 표시를 할 부모 요소
     * @param {string} message - 표시할 메시지 (기본값: '데이터를 불러오는 중...')
     */
    renderLoading: (container, message = '데이터를 불러오는 중...') => {
        if (!container) return;
        container.innerHTML = `
            <div class="loading-state" style="text-align: center; padding: 50px 0;">
                <div class="spinner" style="margin-bottom: 20px;">
                    <!-- CSS로 애니메이션 효과를 줄 수 있는 스피너 구조 -->
                    <div class="loading-spinner"></div>
                </div>
                <h2 style="color: #e2bb59;">${message}</h2>
                <p style="color: #b0b0b0;">잠시만 기다려 주세요.</p>
            </div>
        `;
    }
};
