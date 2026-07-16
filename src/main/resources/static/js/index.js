document.addEventListener('DOMContentLoaded', () => {
    const timerContainers = document.querySelectorAll('.island-info-main[data-start-times]');
    
    function updateTimers() {
        const now = new Date();
        
        timerContainers.forEach(container => {
            const startTimesStr = container.getAttribute('data-start-times');
            const display = container.querySelector('.timer-display');
            if (!startTimesStr || !display) return;
            
            // Thymeleaf 날짜 문자열(예: 2026-07-16T16:00)을 로컬 타임존 브라우저 시간으로 안전하게 파싱하기 위해 포맷 가공
            const times = startTimesStr.split(',').map(t => {
                let s = t.trim();
                // 'T'가 포함된 경우 브라우저가 UTC로 인식하는 것을 방지하기 위해 '-'를 '/'로 변경하고 브라우저 로컬 파싱 보장
                const d = new Date(s.replace('T', ' ').replace(/-/g, '/'));
                // 50분 시작 시간(대기 시간)인 경우, 실제 시작 시간인 정각(10분 뒤)으로 파싱 단계에서 미리 보정합니다.
                if (!isNaN(d.getTime()) && d.getMinutes() === 50) {
                    d.setTime(d.getTime() + 10 * 60 * 1000);
                }
                return d;
            }).filter(d => !isNaN(d.getTime()));
            
            // 현재 시간 기준 진행 중이거나 미래인 일정 필터링
            // 로스트아크 이벤트(카오스게이트, 필드보스 등)는 매 정각 시작 후 1분(60초) 동안만 입장이 가능합니다.
            // 즉, 시작 시간 정각부터 시작 시간 + 59초까지는 해당 이벤트가 진행(입장) 중이며,
            // 시작 시간 + 1분이 되는 시점(예: 16:01:00)부터 완전히 만료되어 다음 시간(예: 17시) 이벤트로 넘어가야 합니다.
            const futureTimes = times.filter(t => {
                const limitTime = new Date(t.getTime() + 60 * 1000); // 시작 시간 + 1분
                return limitTime > now;
            });
            
            if (futureTimes.length === 0) {
                display.textContent = "오늘의 일정이 모두 종료되었습니다.";
                display.style.color = "#adb5bd";
                return;
            }
            
            // 오름차순 정렬 후 가장 가까운 시간 타겟
            futureTimes.sort((a, b) => a - b);
            const nextEvent = futureTimes[0];
            
            const diffMs = nextEvent - now;
            
            // 만약 현재 시각이 시작 시각을 이미 지나서 1분 이내 입장 기간인 경우
            if (diffMs <= 0) {
                display.textContent = `${nextEvent.getHours()}시 등장 - 진행 중 (입장 가능)`;
                display.style.color = "#2ecc71"; // 초록색 강조
                display.style.fontWeight = "bold";
                return;
            }
            
            const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
            const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
            const diffSecs = Math.floor((diffMs % (1000 * 60)) / 1000);
            
            // 1시간 이내 접근 시 오렌지 강조
            if (diffMs <= 1000 * 60 * 60) {
                display.style.color = "#e67e22";
                display.style.fontWeight = "bold";
            } else {
                display.style.color = "#868e96";
                display.style.fontWeight = "normal";
            }
            
            let timeString = "";
            if (diffHrs > 0)
                timeString += `${diffHrs}시간 `;
                 timeString += `${diffMins}분 ${diffSecs}초 남음`;
            
            display.textContent = `${nextEvent.getHours()}시 등장 - ${timeString}`;
        });
    }
    
    // 1초마다 타이머 갱신
    updateTimers();
    setInterval(updateTimers, 1000);
});
