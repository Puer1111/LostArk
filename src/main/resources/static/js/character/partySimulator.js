import {synergyData} from './synergyData.js';

document.addEventListener('DOMContentLoaded', () => {
    // --- Element Selectors ---
    const jobItems = document.querySelectorAll('.job-item');
    const simulatorArea = document.getElementById('simulator-area');
    const classGroupHeaders = document.querySelectorAll('.class-group h3');

    // --- Event Listeners ---

    // 1. Class selection accordion
    classGroupHeaders.forEach(header => {
        header.addEventListener('click', () => {
            const className = header.dataset.class;
            toggleJobs(className);
        });
    });

    // 2. Drag and Drop for party members
    jobItems.forEach(item => {
        item.setAttribute('draggable', 'true');
        item.addEventListener('dragstart', handleDragStart);
        item.addEventListener('dragend', handleDragEnd);
    });

    simulatorArea.addEventListener('dragover', handleDragOver);
    simulatorArea.addEventListener('drop', handleDrop);

    // Prevent h3 from being draggable
    document.querySelectorAll('.class-group h3').forEach(title => {
        title.setAttribute('draggable', 'false');
    });

    // --- Functions ---

    function toggleJobs(className) {
        const jobList = document.getElementById('jobs-' + className);
        if (!jobList) return;

        const isAlreadyOpen = jobList.style.display === 'block';

        const allJobLists = document.querySelectorAll('.job-list');
        allJobLists.forEach(el => {
            if (el.id !== 'jobs-' + className) {
                el.style.display = 'none';
            }
        });

        jobList.style.display = isAlreadyOpen ? 'none' : 'block';
    }

    function handleDragStart(event) {
        event.stopPropagation();
        const jobName = event.target.textContent.trim();
        event.dataTransfer.setData('text/plain', jobName);
        event.dataTransfer.effectAllowed = 'copy';
        event.target.style.opacity = '0.5';
    }

    function handleDragEnd(event) {
        event.stopPropagation();
        event.target.style.opacity = '1';
    }

    function handleDragOver(event) {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
    }

    function handleDrop(event) {
        event.preventDefault();
        event.stopPropagation();

        const jobName = event.dataTransfer.getData('text/plain');

        if (jobName) {
            const isAlreadyThere = simulatorArea.querySelector(`[data-job-name="${jobName}"]`);

            if (!isAlreadyThere) {
                const newItem = document.createElement('div');
                newItem.className = 'party-member-item';
                newItem.textContent = jobName;
                newItem.dataset.jobName = jobName;

                newItem.addEventListener('dblclick', (e) => {
                    e.stopPropagation();
                    newItem.remove();
                    updatePartySynergies(); // Update synergies on removal
                });

                simulatorArea.appendChild(newItem);
                updatePartySynergies(); // Update synergies on addition
            }
        }
    }

    function updatePartySynergies() {
        const partyMembers = document.querySelectorAll('#simulator-area .party-member-item');
        const synergyDetailsArea = document.querySelector('.synergy-details-area');

        // Key: synergyName, Value: Array of job names (providers)
        const synergyProviders = new Map();

        // 1. Collect all synergies and their providers
        partyMembers.forEach(member => {
            const jobName = member.dataset.jobName;
            const jobData = synergyData[jobName];
            if (jobData && jobData.synergies) {
                const uniqueSynergiesForJob = [...new Set(jobData.synergies.map(s => s.name))];

                uniqueSynergiesForJob.forEach(synergyName => {
                    if (!synergyProviders.has(synergyName)) {
                        synergyProviders.set(synergyName, []);
                    }
                    synergyProviders.get(synergyName).push(jobName);
                });
            }
        });

        // 2. Clear the area and render the new nested list
        synergyDetailsArea.innerHTML = '';

        if (synergyProviders.size > 0) {
            const title = document.createElement('h2');
            title.textContent = '현재 파티 시너지';
            synergyDetailsArea.appendChild(title);

            const mainList = document.createElement('ul');
            mainList.classList.add('synergy-main-list');

            // 한글 가나다순으로 시너지 이름을 정렬합니다.
            const sortedSynergies = new Map([...synergyProviders.entries()].sort((a, b) => a[0].localeCompare(b[0], 'ko-KR')));

            sortedSynergies.forEach((providers, synergyName) => {
                const mainListItem = document.createElement('li');
                mainListItem.textContent = synergyName;

                const subList = document.createElement('ul');
                subList.classList.add('synergy-provider-list');

                // 시너지를 제공하는 직업 이름도 가나다순으로 정렬합니다.
                providers.sort((a, b) => a.localeCompare(b, 'ko-KR')).forEach(providerName => {
                    const providerItem = document.createElement('li');
                    providerItem.textContent = providerName;
                    subList.appendChild(providerItem);
                });

                mainListItem.appendChild(subList);
                mainList.appendChild(mainListItem);
            });

            synergyDetailsArea.appendChild(mainList);
        }
    }
});