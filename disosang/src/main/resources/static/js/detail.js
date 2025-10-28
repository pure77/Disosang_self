// DOM이 모두 로드되었을 때 스크립트 실행
document.addEventListener('DOMContentLoaded', function() {

    // --- 1. 탭 전환 기능 ---
    const tabHome = document.getElementById('tab-home');
    const tabReview = document.getElementById('tab-review');
    const panelHome = document.getElementById('panel-home');
    const panelReview = document.getElementById('panel-review');

    // '홈' 탭 클릭 시
    tabHome.addEventListener('click', function(e) {
        e.preventDefault(); // 링크 기본 동작(페이지 이동) 방지

        // 패널 보이기/숨기기
        panelHome.style.display = 'block';
        panelReview.style.display = 'none';

        // 탭 활성화 스타일 변경
        tabHome.classList.add('active');
        tabReview.classList.remove('active');
    });

    // '후기' 탭 클릭 시
    tabReview.addEventListener('click', function(e) {
        e.preventDefault();

        // 패널 보이기/숨기기
        panelHome.style.display = 'none';
        panelReview.style.display = 'block';

        // 탭 활성화 스타일 변경
        tabReview.classList.add('active');
        tabHome.classList.remove('active');
    });


    // --- 2. 별점 클릭 및 후기 폼 표시 기능 ---
    const reviewStars = document.querySelectorAll('#review-stars span');
    const reviewForm = document.getElementById('review-form');
    const ratingInput = document.getElementById('rating');

    reviewStars.forEach(star => {
        star.addEventListener('click', function() {
            const rating = this.dataset.value; // 클릭한 별의 data-value (1~5)
            console.log("별점 클릭됨:", rating); // 👈 브라우저 콘솔에서 확인용 로그
            ratingInput.value = rating;

            // 클릭한 별까지 색상 채우기
            reviewStars.forEach(s => {
                if (s.dataset.value <= rating) {
                    s.innerHTML = '★'; // 채워진 별
                    s.classList.add('filled');
                } else {
                    s.innerHTML = '☆'; // 빈 별
                    s.classList.remove('filled');
                }
            });

            // 별점을 클릭하면 후기 작성 폼을 보여줌
            reviewForm.style.display = 'block';
        });
    });

});