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
});