/**
 * 사이드바 클래스 선택 및 드래그 기능 관련 모듈
 */
import {raidConfigs} from "../character/raidConfigs.js";
import {jobData} from "../character/synergyDataV2.js";

export const classFunction = {
    initSidebarEvents: (elements) => {
        if (!elements || !elements.classSelection) return;

        // 사이드바 그룹 열기/닫기
        elements.classSelection.addEventListener('click', (e) => {
            const header = e.target.closest('.class-group h3');
            if (header) {
                const jobList = document.getElementById('jobs-' + header.dataset.class);
                if (!jobList) return;

                const isCurrentlyOpen = jobList.style.display === 'flex';
                document.querySelectorAll('.job-list').forEach(list => list.style.display = 'none');
                jobList.style.display = isCurrentlyOpen ? 'none' : 'flex';
            }
        });

        // 드래그 시작 로직 (원인 2 해결: closest 사용)
        elements.classSelection.addEventListener('dragstart', (e) => {
            const jobItem = e.target.closest('.job-item');
            if (jobItem) {
                e.dataTransfer.setData('text/plain', jobItem.textContent.trim());
                e.dataTransfer.effectAllowed = 'move';
            }
        });
    }
};
