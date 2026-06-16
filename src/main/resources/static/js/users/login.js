document.addEventListener('DOMContentLoaded', function () {
    const userIdInput = document.getElementById('userId');
    const userPasswordInput = document.getElementById('userPassword');
    const loginBtn = document.getElementById('btn-login');

    if (loginBtn) {
        loginBtn.addEventListener("click", function () {
            checkLogin();
        });
    }

    // 엔터키 입력 시 로그인 요청
    [userIdInput, userPasswordInput].forEach(input => {
        if (input) {
            input.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    checkLogin();
                }
            });
        }
    });

    async function checkLogin() {
        if (!userIdInput || !userPasswordInput) return;

        const userData = {
            userId: userIdInput.value,
            userPassword: userPasswordInput.value
        };

        const url = '/users/login';
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });

            if (response.ok) {
                const result = await response.json();
                // 토큰은 서버에서 HttpOnly 쿠키로 설정했으므로 JS에서 저장할 필요 없음
                // UI에서 사용할 수 있도록 유저 아이디만 저장
                localStorage.setItem('userId', result.userId);
                
                alert("로그인에 성공했습니다!");
                window.location.href = "/"; // 메인 페이지로 이동
            } else {
                const errorMsg = await response.text();
                alert(errorMsg || "로그인에 실패했습니다. 아이디 또는 비밀번호를 확인해주세요.");
            }
        } catch (error) {
            console.error("Login API 호출 실패", error);
            alert("로그인 API 호출에 실패했습니다.");
        }
    }
});