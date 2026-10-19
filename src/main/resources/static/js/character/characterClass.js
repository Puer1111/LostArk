import {synergyService} from './synergyService.js';

document.addEventListener('DOMContentLoaded', async () => {
    const sidebarUl = document.getElementById('class-sidebar-ul');
    const detailsContainer = document.getElementById('class-details-container');
    const synergyDetailsContainer = document.getElementById('synergy-details-container');

    // 1. 파티 시뮬레이터와 동일하게 DB(API)로부터 시너지 데이터 비동기 로드
    let jobList = [];
    try {
        jobList = await synergyService.fetchJobData();
    } catch (error) {
        console.error('시너지 데이터 로드 오류:', error);
    }

    if (!jobList || jobList.length === 0) {
        if (sidebarUl) sidebarUl.innerHTML = '<li style="color: #888; cursor: default;">직업군 없음</li>';
        if (detailsContainer) detailsContainer.innerHTML = '<p style="color: #888; padding: 20px;">등록된 직업 데이터가 없습니다.</p>';
        return;
    }

    // 2. DB 데이터의 classGroup을 기준으로 동적 그룹화 (하드코딩 없음)
    const grouped = new Map();
    jobList.forEach(job => {
        const group = (job.classGroup && job.classGroup.trim()) || '기타';
        if (!grouped.has(group)) {
            grouped.set(group, []);
        }
        grouped.get(group).push(job);
    });

    // 3. 좌측 직업군 사이드바 및 우측 클래스 목록 동적 생성
    let sidebarHtml = '';
    let detailsHtml = '';
    let isFirst = true;

    grouped.forEach((jobs, groupName) => {
        const activeClass = isFirst ? 'active' : '';

        // 좌측 사이드바: 직업군
        sidebarHtml += `
            <li class="${activeClass}" data-group="${groupName}">
                <a href="#">${groupName}</a>
            </li>
        `;

        // 우측 영역: 해당 직업군의 클래스 목록
        detailsHtml += `
            <div class="class-group ${activeClass}" data-group="${groupName}">
                ${jobs.map(job => `
                    <div class="class-name">
                        <span>${job.className}</span>
                    </div>
                `).join('')}
            </div>
        `;

        if (isFirst) isFirst = false;
    });

    if (sidebarUl) sidebarUl.innerHTML = sidebarHtml;
    if (detailsContainer) detailsContainer.innerHTML = detailsHtml;

    // 4. 사이드바 직업군 클릭 시 우측 클래스 목록 전환
    if (sidebarUl) {
        sidebarUl.addEventListener('click', (e) => {
            const item = e.target.closest('li');
            if (!item) return;
            e.preventDefault();

            sidebarUl.querySelectorAll('li').forEach(li => li.classList.remove('active'));
            item.classList.add('active');

            const selectedGroup = item.getAttribute('data-group');
            if (detailsContainer) {
                detailsContainer.querySelectorAll('.class-group').forEach(group => group.classList.remove('active'));
                const targetGroup = detailsContainer.querySelector(`.class-group[data-group="${selectedGroup}"]`);
                if (targetGroup) {
                    targetGroup.classList.add('active');
                }
            }
        });
    }

    // 5. 클래스(직업) 클릭 시 하단에 시너지 및 스킬 정보 렌더링
    if (detailsContainer) {
        detailsContainer.addEventListener('click', (e) => {
            const classNameElement = e.target.closest('.class-name');
            if (!classNameElement) return;

            // 선택된 클래스 표시
            detailsContainer.querySelectorAll('.class-name').forEach(el => el.classList.remove('active'));
            classNameElement.classList.add('active');

            const className = classNameElement.querySelector('span')?.textContent.trim();
            if (!className) return;

            renderSynergyDetails(className);
        });
    }

    // 6. 하단 시너지 및 스킬 정보 렌더링
    function renderSynergyDetails(className) {
        const jobInfo = synergyService.findJobByName(className);
        const skills = jobInfo ? (jobInfo.skills || []) : [];

        if (skills.length > 0) {
            let html = `<h3>${className} 시너지 정보</h3>`;

            skills.forEach(synergyGroup => {
                html += `
                    <div class="synergy-group">
                        <h4 class="synergy-name">${synergyGroup.name}</h4>
                        <ul class="synergy-sources">
                `;

                (synergyGroup.sources || []).forEach(source => {
                    let rateInfo = source.baseRate ? `[${source.baseRate}]` : '';
                    if (source.tripodName) {
                        rateInfo += ` ${source.tripodName} [${source.tripodRate || ''}]`;
                    }

                    const defaultSkillImg = "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='32'%3E%3Crect width='32' height='32' fill='%23333'/%3E%3Ctext x='50%25' y='50%25' fill='%23777' font-size='10' text-anchor='middle' dy='.3em'%3ESkill%3C/text%3E%3C/svg%3E";
                    const defaultTripodImg = "data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='32'%3E%3Crect width='32' height='32' fill='%23333'/%3E%3Ctext x='50%25' y='50%25' fill='%23777' font-size='10' text-anchor='middle' dy='.3em'%3ETripod%3C/text%3E%3C/svg%3E";

                    const skillImgSrc = source.skillImg && source.skillImg.trim() !== '' ? source.skillImg : defaultSkillImg;
                    const tripodImgSrc = source.tripodImg && source.tripodImg.trim() !== '' ? source.tripodImg : defaultTripodImg;

                    html += `
                        <li class="synergy-item" style="list-style-type: none;">
                            <div class="synergy-item-header">
                                <img src="${skillImgSrc}" alt="${source.skillName}" class="skill-icon" onerror="this.onerror=null; this.src='${defaultSkillImg}'">
                                <span class="skill-name">${source.skillName}</span>
                                <span class="rate-info">${rateInfo.trim()}</span>
                            </div>
                            <div class="synergy-item-details hidden">
                                <div class="detail-section skill-section">
                                    <div class="section-title">스킬 설명</div>
                                    <p class="section-description">${source.skillDescription || '설명이 없습니다.'}</p>
                                </div>
                                ${source.tripodName ? `
                                <div class="detail-section tripod-section">
                                    <div class="section-title">트라이포드: ${source.tripodName} <span class="tripod-rate">[${source.tripodRate || ''}]</span></div>
                                    <div class="tripod-content">
                                        <img src="${tripodImgSrc}" alt="${source.tripodName}" class="tripod-icon" onerror="this.onerror=null; this.src='${defaultTripodImg}'">
                                        <p class="section-description">${source.tripodDescription || '트라이포드 설명이 없습니다.'}</p>
                                    </div>
                                </div>
                                ` : ''}
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
            synergyDetailsContainer.innerHTML = `<h3>${className}</h3><p style="padding: 15px; color: #888;">해당 직업에 대한 시너지 정보가 등록되어 있지 않습니다.</p>`;
            synergyDetailsContainer.scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    }

    // 7. 스킬 상세정보 div 아코디언 토글 이벤트
    if (synergyDetailsContainer) {
        synergyDetailsContainer.addEventListener('click', (event) => {
            const clickedHeader = event.target.closest('.synergy-item-header');
            if (!clickedHeader) return;

            const clickedItem = clickedHeader.parentElement;
            const detailsDiv = clickedItem.querySelector('.synergy-item-details');

            if (detailsDiv) {
                clickedItem.classList.toggle('active');
                detailsDiv.classList.toggle('hidden');
            }
        });
    }
});