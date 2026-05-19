document.addEventListener('DOMContentLoaded', function () {
    const userIdInput = document.getElementById('userId');
    const userPasswordInput = document.getElementById('userPassword');
    const loginBtn = document.getElementById('btn-login');

    if (loginBtn) {
        loginBtn.addEventListener("click", function () {
            checkLogin();
        });
    }

    async function checkLogin() {
        if (!userIdInput || !userPasswordInput) return;

        const userData = {
            username: userIdInput.value,
            password: userPasswordInput.value
        };

        // Convert to URL-encoded form data
        const formData = new URLSearchParams();
        for (const key in userData) {
            formData.append(key, userData[key]);
        }

        const url = '/users/login';
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formData
            });

            if (response.ok) {
                window.location.href = "/"; // Redirect to home on success
            } else {
                alert("로그인에 실패했습니다. 아이디 또는 비밀번호를 확인해주세요.");
            }
        } catch (error) {
            console.error("Login API 호출 실패", error);
            alert("로그인 API 호출에 실패했습니다.");
        }
    }
});