document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname;
    const mainNavItems = document.querySelectorAll('.nav-bar li[data-nav-section]');
    const secondaryNavs = document.querySelectorAll('.secondary-nav-container .secondary-nav');

    let activeSection = null;

    // Find which main section is active by checking its sub-links
    mainNavItems.forEach(item => {
        const section = item.dataset.navSection;
        const secondaryNav = document.querySelector(`.secondary-nav[data-nav-section="${section}"]`);
        if (secondaryNav) {
            const links = secondaryNav.querySelectorAll('a');
            links.forEach(link => {
                if (currentPath.startsWith(link.getAttribute('href'))) {
                    activeSection = section;
                }
            });
        }
    });

    // If an active section is found, update the UI
    if (activeSection) {
        // Add active class to the main nav item for the underline
        const activeMainNavItem = document.querySelector(`.nav-bar li[data-nav-section="${activeSection}"]`);
        if (activeMainNavItem) {
            activeMainNavItem.classList.add('active-nav');
        }

        // Show the corresponding secondary nav bar
        const activeSecondaryNav = document.querySelector(`.secondary-nav[data-nav-section="${activeSection}"]`);
        if (activeSecondaryNav) {
            activeSecondaryNav.style.display = 'flex';
        }

        // Also, mark the specific active link in the secondary nav
        const secondaryLinks = activeSecondaryNav.querySelectorAll('a');
        secondaryLinks.forEach(link => {
            if (currentPath.startsWith(link.getAttribute('href'))) {
                link.classList.add('active-sub-nav');
            }
        });
    }

    // --- 헤더 검색 기능 로직 ---
    const searchInput = document.getElementById('search-Character');
    const searchButton = document.getElementById('search-Character-btn');
    const recentSearchList = document.getElementById('recent-search-list');

    // 최근 검색어 관련 상수
    const RECENT_SEARCH_KEY = 'recentSearches';
    const MAX_RECENT_COUNT = 5;

    // 최근 검색어 로드 및 렌더링
    function renderRecentSearches() {
        if (!recentSearchList) return;
        
        const searches = JSON.parse(localStorage.getItem(RECENT_SEARCH_KEY) || '[]');
        
        if (searches.length === 0) {
            recentSearchList.style.display = 'none';
            return;
        }

        recentSearchList.innerHTML = searches.map(term => `
            <div class="recent-search-item">
                <span class="recent-search-text">${term}</span>
                <button class="delete-search-btn" data-term="${term}">삭제</button>
            </div>
        `).join('');

        // 삭제 버튼 이벤트 연결
        document.querySelectorAll('.delete-search-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                deleteRecentSearch(e.target.dataset.term);
            });
        });

        // 항목 클릭 시 검색 실행
        document.querySelectorAll('.recent-search-item').forEach(item => {
            item.addEventListener('click', () => {
                const term = item.querySelector('.recent-search-text').textContent;
                searchInput.value = term;
                performHeaderSearch(term);
            });
        });

        // 리스트 표시
        recentSearchList.style.display = 'block';
    }

    // 최근 검색어 저장
    function saveRecentSearch(term) {
        if (!term.trim()) return;
        let searches = JSON.parse(localStorage.getItem(RECENT_SEARCH_KEY) || '[]');
        
        // 중복 제거 후 맨 앞으로 추가
        searches = searches.filter(s => s !== term);
        searches.unshift(term);
        
        // 최대 5개 유지
        if (searches.length > MAX_RECENT_COUNT) {
            searches = searches.slice(0, MAX_RECENT_COUNT);
        }
        
        localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(searches));
    }

    // 최근 검색어 삭제
    function deleteRecentSearch(term) {
        let searches = JSON.parse(localStorage.getItem(RECENT_SEARCH_KEY) || '[]');
        searches = searches.filter(s => s !== term);
        localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(searches));
        renderRecentSearches();
    }

    // 공통 검색 실행 함수
    function performHeaderSearch(forcedTerm) {
        const term = forcedTerm || (searchInput ? searchInput.value.trim() : "");
        if (!term) {
            alert("검색할 캐릭터 이름을 입력해 주세요!");
            return;
        }
        
        saveRecentSearch(term);
        window.location.href = `/character/${term}`;
    }

    // 검색창 포커스 시 최근 검색어 표시
    if (searchInput) {
        searchInput.addEventListener('focus', renderRecentSearches);
        
        // 외부 클릭 시 리스트 숨기기
        document.addEventListener('click', (e) => {
            if (recentSearchList && !e.target.closest('.search-wrapper')) {
                recentSearchList.style.display = 'none';
            }
        });

        searchInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                performHeaderSearch();
            }
        });
    }

    if (searchButton) {
        searchButton.addEventListener('click', () => performHeaderSearch());
    }
});