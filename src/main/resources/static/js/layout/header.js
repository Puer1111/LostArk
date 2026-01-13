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

    // 공통 검색 실행 함수
    function performHeaderSearch() {
        if (!searchInput || !searchInput.value) {
            alert("검색할 캐릭터 이름을 입력해 주세요!");
            return;
        }
        const characterName = searchInput.value;
        // window.location.href = `/character/expedition/${characterName}`;
        window.location.href = `/character/${characterName}`;
    }

    // 1. 검색 버튼 클릭 이벤트
    if (searchButton) {
        searchButton.addEventListener('click', performHeaderSearch);
    }

    // 2. Enter 키 입력 이벤트 (기존 로직을 공통 함수 사용으로 변경)
    if (searchInput) {
        searchInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                performHeaderSearch(); // 공통 함수 호출
            }
        });
    }
});