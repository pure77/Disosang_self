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
// DOM이 모두 로드되었을 때 스크립트 실행
document.addEventListener('DOMContentLoaded', function() {

    // --- 1. 탭 전환 기능 ---
    // ... (기존 탭 전환 코드 ... (생략)) ...
    const tabHome = document.getElementById('tab-home');
    const tabReview = document.getElementById('tab-review');
    const panelHome = document.getElementById('panel-home');
    const panelReview = document.getElementById('panel-review');

    tabHome.addEventListener('click', function(e) {
        e.preventDefault();
        panelHome.style.display = 'block';
        panelReview.style.display = 'none';
        tabHome.classList.add('active');
        tabReview.classList.remove('active');
    });

    tabReview.addEventListener('click', function(e) {
        e.preventDefault();
        panelHome.style.display = 'none';
        panelReview.style.display = 'block';
        tabReview.classList.add('active');
        tabHome.classList.remove('active');
    });


    // --- 2. 별점 클릭 및 후기 폼 표시 기능 ---
    // ... (기존 새 리뷰 별점 코드 ... (생략)) ...
    const reviewStars = document.querySelectorAll('#review-stars span');
    const reviewForm = document.getElementById('review-form');
    const ratingInput = document.getElementById('rating');

    reviewStars.forEach(star => {
        star.addEventListener('click', function() {
            const rating = this.dataset.value;
            ratingInput.value = rating;

            reviewStars.forEach(s => {
                if (s.dataset.value <= rating) {
                    s.innerHTML = '★';
                    s.classList.add('filled');
                } else {
                    s.innerHTML = '☆';
                    s.classList.remove('filled');
                }
            });
            reviewForm.style.display = 'block';
        });
    });


    //
    // ▼▼▼ [이 아래로 코드가 추가되었습니다] ▼▼▼
    //

    // --- 3. 수정 폼 내부의 별점 클릭 기능 ---
    // '.edit-stars' 클래스를 가진 모든 별점 세트에 대해 이벤트 리스너 설정
    const allEditStars = document.querySelectorAll('.edit-stars');
    allEditStars.forEach(starSet => {
        const stars = starSet.querySelectorAll('span');
        const editForm = starSet.closest('.review-edit-form');
        const ratingInput = editForm.querySelector('.edit-rating-input');

        // 폼이 처음 보일 때, input의 초기 값(th:value)에 따라 별을 채움
        fillStars(stars, ratingInput.value);

        stars.forEach(star => {
            star.addEventListener('click', function() {
                const rating = this.dataset.value;
                ratingInput.value = rating;
                fillStars(stars, rating);
            });
        });
    });

    // 별 채우기 헬퍼 함수
    function fillStars(starElements, rating) {
        starElements.forEach(s => {
            if (s.dataset.value <= rating) {
                s.innerHTML = '★';
                s.classList.add('filled');
            } else {
                s.innerHTML = '☆';
                s.classList.remove('filled');
            }
        });
    }


    // --- 4. 수정 / 삭제 / 취소 버튼 이벤트 처리 (이벤트 위임 사용) ---
    const reviewList = document.querySelector('.review-list');

    if (reviewList) {
        reviewList.addEventListener('click', function(e) {

            const reviewItem = e.target.closest('.review-item');
            if (!reviewItem) return; // 리뷰 아이템 밖에서 클릭한 건 무시

            // (1) '삭제' 버튼 클릭 시
        if (e.target.matches('.btn-delete')) {
                    // fetch() 대신 form의 기본 동작(submit)을 가로채서 확인창만 띄웁니다.
                    e.preventDefault();

                    if (confirm('정말 삭제하시겠습니까?')) {
                        // '확인'을 누르면, 버튼이 속한 <form>을 찾아 제출(submit)시킵니다.
                        const deleteForm = e.target.closest('form.delete-form');
                        if (deleteForm) {
                            deleteForm.submit();
                        }
                    }
                    // '취소'를 누르면 아무 일도 일어나지 않습니다.
                }

            // (2) '수정' 버튼 클릭 시
            if (e.target.matches('.btn-edit')) {
                e.preventDefault();
                const displayArea = reviewItem.querySelector('.review-display');
                const editForm = reviewItem.querySelector('.review-edit-form');

                // 기존 내용 숨기고, 수정 폼 보여주기
                displayArea.style.display = 'none';
                editForm.style.display = 'block';

                // 폼이 보일 때 현재 별점으로 다시 채워줌 (필수)
                const ratingInput = editForm.querySelector('.edit-rating-input');
                const stars = editForm.querySelectorAll('.edit-stars span');
                fillStars(stars, ratingInput.value);
            }

            // (3) '취소' 버튼 클릭 시
            if (e.target.matches('.btn-edit-cancel')) {
                e.preventDefault();
                const displayArea = reviewItem.querySelector('.review-display');
                const editForm = reviewItem.querySelector('.review-edit-form');

                // 수정 폼 숨기고, 기존 내용 보여주기
                editForm.style.display = 'none';
                displayArea.style.display = 'block';
            }

            // (4) '수정 완료' (제출) 버튼은 <form>의 기본 submit 이벤트를 사용합니다.
            // (fetch를 사용하려면 e.preventDefault() 후 btn-edit-save 클릭을 잡아야 함)
        });
    }
    if (window.location.hash === '#panel-review') {
            // '후기' 탭(id='tab-review')을 찾아서 강제로 click() 이벤트를 실행
            const reviewTab = document.getElementById('tab-review');
            if (reviewTab) {
                reviewTab.click();
            }
        }



});
    //리뷰정렬
     function changeSort(sortType) {
         // 현재 URL에서 쿼리 파라미터 조작
         const urlParams = new URLSearchParams(window.location.search);

         // storeId 등 기존 파라미터는 유지하고 sort만 변경
         urlParams.set('sort', sortType);

         // 탭 상태 유지를 위해 hash(#panel-review)도 유지하면 좋음
        window.location.href = window.location.pathname + '?' + urlParams.toString() + '#panel-review';
     }
// 삭제할 이미지 경로를 저장할 배열 (또는 폼 내부에 hidden input 생성)
function removeExistingPhoto(button, photoUrl) {
    if (!confirm("이 사진을 삭제하시겠습니까?")) return;

    // 1. 화면에서 사진 요소 제거
    const container = button.parentElement;
    const form = container.closest('form');
    container.style.display = 'none';

    // 2. 서버로 전송할 '삭제 대상 리스트'에 추가
    // hidden input을 생성하여 삭제할 파일의 URL이나 ID를 담아 전송합니다.
    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'deletedPhotos'; // 서버 컨트롤러에서 받을 파라미터명
    hiddenInput.value = photoUrl;

    form.querySelector('.deleted-photos-container').appendChild(hiddenInput);
}
// --- 5. 사진 미리보기 기능 추가 ---

    // 공통 미리보기 처리 함수
    function handleImagePreview(input, previewContainer) {
        if (!input || !previewContainer) return;

        input.addEventListener('change', function(e) {
            previewContainer.innerHTML = ''; // 기존 미리보기 초기화
            const files = Array.from(e.target.files);

            files.forEach(file => {
                const reader = new FileReader();
                reader.onload = function(event) {
                    const div = document.createElement('div');
                    div.className = 'preview-item';
                    div.innerHTML = `<img src="${event.target.result}">`;
                    previewContainer.appendChild(div);
                };
                reader.readAsDataURL(file);
            });
        });
    }

    // (1) 새 리뷰 등록 폼 미리보기
    const addPhotosInput = document.getElementById('photos');
    const addPreviewContainer = document.getElementById('image-preview');
    handleImagePreview(addPhotosInput, addPreviewContainer);

    // (2) 리뷰 수정 폼 미리보기 (여러 개일 수 있으므로 반복문 처리)
    const editForms = document.querySelectorAll('.review-edit-form');
    editForms.forEach(form => {
        const editInput = form.querySelector('.edit-photos-input');
        const editPreview = form.querySelector('.edit-image-preview');
        handleImagePreview(editInput, editPreview);
    });