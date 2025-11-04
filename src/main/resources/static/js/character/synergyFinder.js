document.addEventListener('DOMContentLoaded', () => {
    const classSelector = document.getElementById('class-selector');
    const synergyResults = document.getElementById('synergy-results');

    // Populate the dropdown
    for (const className in synergyData) {
        const option = document.createElement('option');
        option.value = className;
        option.textContent = className;
        classSelector.appendChild(option);
    }

    // Function to render the grid
    const renderGrid = () => {
        const selectedClass = classSelector.value;
        const selectedClassSynergies = new Set(synergyData[selectedClass]?.synergies || []);

        synergyResults.innerHTML = '';

        for (const className in synergyData) {
            const resultCard = document.createElement('div');
            resultCard.classList.add('result-card');

            const partnerSynergies = new Set(synergyData[className].synergies);
            let isComplementary = false;

            for (const synergy of partnerSynergies) {
                if (!selectedClassSynergies.has(synergy)) {
                    isComplementary = true;
                    break;
                }
            }

            if (selectedClass && className !== selectedClass && isComplementary) {
                resultCard.classList.add('highlight');
            }

            resultCard.innerHTML = `
                <h3>${className}</h3>
                <ul>
                    ${synergyData[className].synergies.map(s => `<li>${s}</li>`).join('')}
                </ul>
            `;
            synergyResults.appendChild(resultCard);
        }
    };

    // Listen for changes
    classSelector.addEventListener('change', renderGrid);

    // Initial render
    renderGrid();
});
