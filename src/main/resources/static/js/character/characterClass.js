import {synergyData} from './synergyData.js';

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

                // 각 시너지 그룹(예: 피해 증가, 방어력 감소)별로 처리
                synergyInfo.synergies.forEach(synergyGroup => {
                    html += `
                        <div class="synergy-group">
                            <h4 class="synergy-name">${synergyGroup.name}</h4>
                            <ul class="synergy-sources">
                    `;

                    // 각 시너지 그룹에 속한 출처(스킬)들을 목록으로 표시
                    synergyGroup.sources.forEach(source => {
                        let rateInfo = source.baseRate ? `[${source.baseRate}]` : '';
                        if (source.tripodName) {
                            rateInfo += ` ${source.tripodName} [${source.tripodRate}]`;
                        }

                        // [수정] 각 아이템이 헤더와 상세정보 div를 갖도록 구조 변경
                        html += `
                            <li class="synergy-item" style="list-style-type: none;">
                                <div class="synergy-item-header">
                                    <img src="${source.skillImg}" alt="${source.skillName}" class="skill-icon">
                                    <span class="skill-name">${source.skillName}</span>
                                    <span class="rate-info">${rateInfo.trim()}</span>
                                </div>
                                <div class="synergy-item-details hidden">
<!--                                    <h4>트라이포드</h4>  이자리에 스킬 설명 들어오자. -->     
                                    <p>${source.tripodName}</p>
                                    <img src="${source.tripodImg}" alt="" class="skill-icon"></p>
                                    <p>${source.tripodDescription}</p> 
                                </div>
                            </li>
                        `;
                    });

                    html += `
                            </ul>
                        </div>
                    `;
                });

                synergyDetailsContainer.innerHTML = html;
                synergyDetailsContainer.scrollIntoView({behavior: 'smooth', block: 'start'});
            } else {
                synergyDetailsContainer.innerHTML = `<h3>${className}</h3><p>해당 직업에 대한 시너지 정보가 없습니다.</p>`;
                synergyDetailsContainer.scrollIntoView({behavior: 'smooth', block: 'start'});
            }
        });
    });

    // [수정] 이벤트 리스너 로직 변경: 하위 div를 토글하는 방식으로 변경
    synergyDetailsContainer.addEventListener('click', (event) => {
        // 클릭된 요소가 헤더 부분이 맞는지 확인
        const clickedHeader = event.target.closest('.synergy-item-header');
        if (!clickedHeader) return;

        // 부모 li(.synergy-item)와 하위 상세정보 div(.synergy-item-details)를 찾음
        const clickedItem = clickedHeader.parentElement;
        const detailsDiv = clickedItem.querySelector('.synergy-item-details');

        if (detailsDiv) {
            // active 클래스를 토글하여 CSS로 확장/축소 효과를 줄 수 있음
            clickedItem.classList.toggle('active');
            // hidden 클래스를 토글하여 상세 정보 div를 보여주거나 숨김
            detailsDiv.classList.toggle('hidden');
        }
    });
});