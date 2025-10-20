document.addEventListener("DOMContentLoaded", function () {
    const signUpBtn = document.getElementById('btn-signUp');
    signUpBtn.addEventListener("click", function () {
        window.location.href = "/users/signup";
    })
    getExpedition();
})

// 캐릭터 이름으로 원정대 전체 검색
async function getExpedition() {
    const searchCharacterBtn = document.getElementById('search-Character-btn');
    searchCharacterBtn.addEventListener("click", function () {
        const characterName = document.getElementById('search-Character').value;
        if (!characterName) {
            alert("캐릭터 이름을 입력해 주세요!");
            return;
        }
        // window.location.href = `/character/allExpedition/${characterName}`;
        window.location.href = `/character/${characterName}`;
    });
}

