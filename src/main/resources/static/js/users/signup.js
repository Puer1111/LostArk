document.addEventListener('DOMContentLoaded', function () {

    // --- Element Selectors ---
    const userIdInput = document.getElementById('userId');
    const checkIdButton = document.getElementById('id-check-button');
    const userPasswordInput = document.getElementById('userPassword');
    const userPasswordCheckInput = document.getElementById('passwordConfirm');
    const signupButton = document.getElementById('btn-signup');
    const emailInput = document.getElementById('userEmail');
    const emailVerificationButton = document.getElementById('email-verification-button');
    const verificationCodeInput = document.getElementById('verificationCode');
    const codeVerificationButton = document.getElementById('code-verification-button');
    const verificationCodeGroup = document.getElementById('verification-code-group');

    // --- Dynamic DIV Creation for Status Messages ---
    let idStatusDiv;
    if (checkIdButton) { // 기준을 아이디 입력칸이 아닌 버튼으로 변경
        idStatusDiv = document.createElement('div');
        idStatusDiv.id = 'id-check-status';
        // 버튼을 감싸고 있는 부모 요소(.input-group)의 바로 다음에 div를 삽입
        checkIdButton.parentNode.after(idStatusDiv);
    }

    let passwordStatusDiv;
    if (userPasswordCheckInput) {
        passwordStatusDiv = document.createElement('div');
        passwordStatusDiv.id = 'password-match-status';
        userPasswordCheckInput.after(passwordStatusDiv);
    }

    // --- 1. Password Match Logic ---
    const checkPasswords = () => {
        if (!passwordStatusDiv) return;

        const password = userPasswordInput.value;
        const passwordCheck = userPasswordCheckInput.value;

        if (passwordCheck) {
            if (password === passwordCheck) {
                passwordStatusDiv.innerHTML = '비밀번호가 일치합니다.';
                passwordStatusDiv.style.color = 'green';
            } else {
                passwordStatusDiv.innerHTML = '비밀번호가 일치하지 않습니다.';
                passwordStatusDiv.style.color = 'red';
            }
        } else {
            passwordStatusDiv.innerHTML = '';
        }
    };

    if (userPasswordInput) {
        userPasswordInput.addEventListener('keyup', checkPasswords);
    }
    if (userPasswordCheckInput) {
        userPasswordCheckInput.addEventListener('keyup', checkPasswords);
    }

    // --- 2. ID Duplication Check Logic ---
    if (checkIdButton) {
        checkIdButton.addEventListener('click', async function () {
            if (!idStatusDiv) return;

            const userId = userIdInput.value.trim();
            if (!userId) {
                idStatusDiv.innerHTML = '아이디를 입력해주세요.';
                idStatusDiv.style.color = 'red';
                return;
            }

            try {
                const response = await fetch(`/users/check-id/${userId}`);
                const message = await response.text();

                if (response.ok) {
                    idStatusDiv.innerHTML = message;
                    idStatusDiv.style.color = 'green';
                } else {
                    idStatusDiv.innerHTML = message;
                    idStatusDiv.style.color = 'red';
                }
            } catch (error) {
                console.error('ID Check Error:', error);
                idStatusDiv.innerHTML = '네트워크 오류로 아이디를 확인할 수 없습니다.';
                idStatusDiv.style.color = 'red';
            }
        });
    }

    // --- Email Verification Logic ---
    if (emailVerificationButton) {
        emailVerificationButton.addEventListener('click', async function () {
            const email = emailInput.value.trim();
            if (!email) {
                alert('이메일을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch('/email/send-verification', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ email: email })
                });

                const message = await response.text();
                alert(message);

                if (response.ok) {
                    verificationCodeGroup.style.display = 'block';
                }
            } catch (error) {
                console.error('Email Verification Error:', error);
                alert('이메일 인증 코드 발송 중 오류가 발생했습니다.');
            }
        });
    }

    if (codeVerificationButton) {
        codeVerificationButton.addEventListener('click', async function () {
            const email = emailInput.value.trim();
            const code = verificationCodeInput.value.trim();

            if (!email || !code) {
                alert('이메일과 인증코드를 모두 입력해주세요.');
                return;
            }

            try {
                const response = await fetch('/email/verify-code', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ email: email, code: code })
                });

                const message = await response.text();
                alert(message);

                if (response.ok) {
                    signupButton.disabled = false;
                }
            } catch (error) {
                console.error('Code Verification Error:', error);
                alert('인증코드 확인 중 오류가 발생했습니다.');
            }
        });
    }

    // --- 3. Signup Request Logic ---
    if (signupButton) {
        signupButton.addEventListener('click', async function () {
            const userId = userIdInput.value.trim();
            const userPassword = userPasswordInput.value.trim();
            const userPasswordCheck = userPasswordCheckInput.value.trim();
            const userNickname = document.getElementById('userNickName').value.trim();
            const userEmail = document.getElementById('userEmail').value.trim();

            if (!userId || !userPassword || !userPasswordCheck || !userNickname || !userEmail) {
                alert('모든 필드를 입력해주세요.');
                return;
            }

            if (userPassword !== userPasswordCheck) {
                alert('비밀번호가 일치하지 않습니다. 다시 확인해주세요.');
                return;
            }

            const userData = {
                userId: userId,
                userPassword: userPassword,
                userNickName: userNickname,
                userEmail: userEmail
            };

            try {
                const response = await fetch('/users/signup', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(userData)
                });

                if (response.ok) {
                    alert('회원가입이 성공적으로 완료되었습니다. 로그인 페이지로 이동합니다.');
                    window.location.href = '/users/login';
                } else {
                    const errorMessage = await response.text();
                    alert(`회원가입 실패: ${errorMessage}`);
                }
            } catch (error) {
                console.error('Signup Error:', error);
                alert('회원가입 중 오류가 발생했습니다.');
            }
        });
    }
});