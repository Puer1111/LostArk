document.addEventListener("DOMContentLoaded", function () {
    // 캐릭터 조회 페이지 내의 '원정대' 버튼 기능
    const pageExpeditionBtn = document.getElementById('btn-expedition');
    if (pageExpeditionBtn) {
        pageExpeditionBtn.addEventListener("click", function() {
            // 페이지에 표시된 캐릭터 이름을 가져옵니다.
            const characterNameSpan = document.querySelector('.character-name');
            if (!characterNameSpan || !characterNameSpan.textContent) {
                alert("페이지의 캐릭터 이름을 찾을 수 없습니다.");
                return;
            }
            const characterName = characterNameSpan.textContent;
            window.location.href = `/character/expedition/${characterName}`;
        });
    }

    // 장비 툴팁 호버 기능
    const equipmentAreas = document.querySelectorAll('.equipment-area');

    equipmentAreas.forEach(area => {
        // 각 area 바로 앞에 있는 tooltip-content 요소를 찾습니다.
        const tooltip = area.previousElementSibling;

        // 해당 요소가 실제로 tooltip-content 클래스를 가지고 있는지 확인합니다.
        if (tooltip && tooltip.classList.contains('tooltip-content')) {
            area.addEventListener('mouseover', () => {
                tooltip.style.display = 'block';
            });

            area.addEventListener('mouseout', () => {
                tooltip.style.display = 'none';
            });
        }
    });
});
