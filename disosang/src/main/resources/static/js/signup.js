// 1. 필요한 HTML 요소들을 가져옵니다.
const passwordInput = document.getElementById('password');
const passwordConfirmInput = document.getElementById('password-confirm');
const messageDiv = document.getElementById('password-message');

// 2. 실시간 비밀번호 일치 확인 함수
function checkPasswordMatch() {
    const password = passwordInput.value;
    const passwordConfirm = passwordConfirmInput.value;

    if (passwordConfirm === '') {
        messageDiv.innerHTML = '';
        return;
    }

    if (password === passwordConfirm) {
        messageDiv.innerHTML = '비밀번호가 일치합니다.';
        messageDiv.classList.remove('error');
        messageDiv.classList.add('success');
    } else {
        messageDiv.innerHTML = '비밀번호가 일치하지 않습니다.';
        messageDiv.classList.remove('success');
        messageDiv.classList.add('error');
    }
}

// 3. 비밀번호 관련 입력 필드에만 이벤트 리스너를 추가합니다.
passwordInput.addEventListener('keyup', checkPasswordMatch);
passwordConfirmInput.addEventListener('keyup', checkPasswordMatch);