import { synergyData } from './synergyData.js';

document.addEventListener('DOMContentLoaded', () => {
    const sidebarItems = document.querySelectorAll('.class-sidebar ul li');
    const classGroups = document.querySelectorAll('.class-group');
    const classNames = document.querySelectorAll('.class-name');
    const synergyDetailsContainer = document.getElementById('synergy-details-container');

    // 사이드바 클릭 이벤트 처리
    sidebarItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();

            // 모든 사이드바 아이템에서 active 클래스 제거
            sidebarItems.forEach(i => i.classList.remove('active'));
            // 클릭된 아이템에 active 클래스 추가
            item.classList.add('active');

            const classType = item.getAttribute('data-class-type');

            // 모든 클래스 그룹에서 active 클래스 제거
            classGroups.forEach(group => group.classList.remove('active'));
            // 해당하는 클래스 그룹에 active 클래스 추가
            const targetGroup = document.querySelector(`.class-group[data-class-type="${classType}"]`);
            if (targetGroup) {
                targetGroup.classList.add('active');
            }
        });
    });

    // 직업 이름 클릭 이벤트 처리 (시너지 정보 표시)
    classNames.forEach(classNameElement => {
        classNameElement.addEventListener('click', () => {
            const className = classNameElement.querySelector('span').textContent.trim();
            const synergyInfo = synergyData[className];

            if (synergyInfo && synergyInfo.synergies) {
                let html = `<h3>${className} 시너지 정보</h3>`;

                html += '<div class="synergy-section"><h4>파티 시너지</h4>';
                synergyInfo.synergies.forEach(item => {
                    html += `
                        <div class="synergy-item">
                            <div class="synergy-name">${item.name}</div>
                            <div class="synergy-effect">${item.rate}</div>
                        </div>
                    `;
                });
                html += '</div>';

                synergyDetailsContainer.innerHTML = html;
                synergyDetailsContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
            } else {
                synergyDetailsContainer.innerHTML = `<h3>${className}</h3><p>해당 직업에 대한 시너지 정보가 없습니다.</p>`;
                synergyDetailsContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
});
