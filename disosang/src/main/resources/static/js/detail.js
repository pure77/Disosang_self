// DOM이 모두 로드되었을 때 스크립트 실행
document.addEventListener('DOMContentLoaded', function() {

    // =========================================
    // [NEW] 1. 사진 그리드 및 모달(확대) 기능
    // =========================================

    // HTML(Thymeleaf)에서 전달받은 reviewPhotoList 확인 (없으면 빈 배열)
    const allImages = (typeof reviewPhotoList !== 'undefined') ? reviewPhotoList : [];

    const photoGrid = document.getElementById('photo-grid');
    const noPhotoText = document.getElementById('no-photo-text');

    // 모달 요소 가져오기
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('fullImage');
    const closeModalBtn = document.querySelector('.close-modal');
    const prevBtn = document.querySelector('.modal-nav.prev');
    const nextBtn = document.querySelector('.modal-nav.next');

    let currentImageIndex = 0;

    // (1) 상단 사진 그리드 그리기
    if (allImages.length > 0) {
        if(noPhotoText) noPhotoText.style.display = 'none';
        renderPhotoGrid(allImages);
    } else {
        if(photoGrid) photoGrid.style.display = 'none';
        if(noPhotoText) noPhotoText.style.display = 'flex';
    }

    function renderPhotoGrid(images) {
        if (!photoGrid) return;

        const count = images.length;

        // CSS 클래스 결정을 위한 타입 설정 (1~5개 이상)
        let typeClass = 'type-1';
        if (count === 2) typeClass = 'type-2';
        else if (count === 3) typeClass = 'type-3';
        else if (count === 4) typeClass = 'type-4';
        else if (count >= 5) typeClass = 'type-5';

        photoGrid.className = `photo-grid ${typeClass}`;
        photoGrid.innerHTML = ''; // 초기화

        // 최대 5장까지만 그리드에 표시
        const displayCount = Math.min(count, 5);

        for (let i = 0; i < displayCount; i++) {
            const container = document.createElement('div');
            container.className = `grid-item item-${i}`;

            const img = document.createElement('img');
            img.src = images[i];

            container.appendChild(img);

            // 5번째 사진이고, 실제 사진이 더 많다면 "+N" 오버레이 추가
            if (i === 4 && count > 5) {
                const overlay = document.createElement('div');
                overlay.className = 'more-overlay';
                overlay.innerText = `+${count - 5}`;
                container.appendChild(overlay);
            }

            // 클릭 시 모달 열기 (클로저 문제 해결을 위해 즉시 실행 함수 사용)
            (function(index) {
                container.onclick = function() {
                    openModal(index);
                };
            })(i);

            photoGrid.appendChild(container);
        }
    }

    // (2) 리뷰 리스트의 작은 사진들도 모달 연결
    const reviewListImages = document.querySelectorAll('.review-photo img');
    reviewListImages.forEach((img) => {
        img.style.cursor = 'pointer';
        img.onclick = function() {
            // 이미지 주소로 전체 리스트에서 인덱스 찾기
            const src = img.getAttribute('src');
            // 정확한 매칭을 위해 findIndex 사용
            let index = allImages.findIndex(url => url === src);
            if(index === -1) index = 0; // 혹시 못 찾으면 첫 번째 사진으로
            openModal(index);
        };
    });


    // --- 모달 제어 함수들 ---
    function openModal(index) {
        if (!modal || allImages.length === 0) return;
        currentImageIndex = index;

        modal.style.display = "flex"; // CSS에서 none이었던 것을 flex로 변경
        if(modalImg) modalImg.src = allImages[currentImageIndex];

        // 스크롤 방지
        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        if (!modal) return;
        modal.style.display = "none";
        document.body.style.overflow = "auto"; // 스크롤 복구
    }

    // 닫기 버튼 이벤트
    if (closeModalBtn) closeModalBtn.onclick = closeModal;

    // 배경 클릭 시 닫기
    window.onclick = function(event) {
        if (event.target == modal) {
            closeModal();
        }
    };

    // 화살표 버튼 이벤트
    if (prevBtn) {
        prevBtn.onclick = function(e) {
            e.stopPropagation();
            changeSlide(-1);
        };
    }
    if (nextBtn) {
        nextBtn.onclick = function(e) {
            e.stopPropagation();
            changeSlide(1);
        };
    }

    function changeSlide(step) {
        currentImageIndex += step;
        // 순환 구조 (처음 <-> 끝)
        if (currentImageIndex < 0) currentImageIndex = allImages.length - 1;
        if (currentImageIndex >= allImages.length) currentImageIndex = 0;

        if(modalImg) modalImg.src = allImages[currentImageIndex];
    }

    // 키보드 이벤트 (화살표 좌우, ESC)
    document.addEventListener('keydown', (e) => {
        if (modal && modal.style.display === "flex") {
            if (e.key === "ArrowLeft") changeSlide(-1);
            if (e.key === "ArrowRight") changeSlide(1);
            if (e.key === "Escape") closeModal();
        }
    });


    // =========================================
    // 2. 탭 전환 기능 (홈 <-> 후기)
    // =========================================
    const tabHome = document.getElementById('tab-home');
    const tabReview = document.getElementById('tab-review');
    const panelHome = document.getElementById('panel-home');
    const panelReview = document.getElementById('panel-review');

    if(tabHome && tabReview && panelHome && panelReview) {
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
    }

    // URL 해시(#panel-review)가 있으면 후기 탭 자동 클릭
    if (window.location.hash === '#panel-review' && tabReview) {
        tabReview.click();
    }


    // =========================================
    // 3. 별점 및 리뷰 작성 폼 기능
    // =========================================
    const reviewStars = document.querySelectorAll('#review-stars span');
    const reviewForm = document.getElementById('review-form');
    const ratingInput = document.getElementById('rating');

    if (reviewStars.length > 0) {
        reviewStars.forEach(star => {
            star.addEventListener('click', function() {
                const rating = this.dataset.value;
                if(ratingInput) ratingInput.value = rating;

                reviewStars.forEach(s => {
                    if (s.dataset.value <= rating) {
                        s.innerHTML = '★';
                        s.classList.add('filled');
                    } else {
                        s.innerHTML = '☆';
                        s.classList.remove('filled');
                    }
                });
                if(reviewForm) reviewForm.style.display = 'block';
            });
        });
    }


    // =========================================
    // 4. 리뷰 수정 / 삭제 기능
    // =========================================

    // (A) 수정 폼 별점 채우기 로직
    const allEditStars = document.querySelectorAll('.edit-stars');
    allEditStars.forEach(starSet => {
        const stars = starSet.querySelectorAll('span');
        const editForm = starSet.closest('.review-edit-form');
        const rInput = editForm.querySelector('.edit-rating-input');

        fillStars(stars, rInput.value); // 초기값 설정

        stars.forEach(star => {
            star.addEventListener('click', function() {
                const rating = this.dataset.value;
                rInput.value = rating;
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

    // (B) 버튼 이벤트 위임 (삭제, 수정, 취소)
    const reviewList = document.querySelector('.review-list');
    if (reviewList) {
        reviewList.addEventListener('click', function(e) {
            const reviewItem = e.target.closest('.review-item');
            if (!reviewItem) return;

            // 삭제 버튼
            if (e.target.matches('.btn-delete')) {
                e.preventDefault();
                if (confirm('정말 삭제하시겠습니까?')) {
                    const deleteForm = e.target.closest('form.delete-form');
                    if (deleteForm) deleteForm.submit();
                }
            }

            // 수정 버튼
            if (e.target.matches('.btn-edit')) {
                e.preventDefault();
                const displayArea = reviewItem.querySelector('.review-display');
                const editForm = reviewItem.querySelector('.review-edit-form');

                displayArea.style.display = 'none';
                editForm.style.display = 'block';

                // 별점 다시 그리기
                const rInput = editForm.querySelector('.edit-rating-input');
                const stars = editForm.querySelectorAll('.edit-stars span');
                fillStars(stars, rInput.value);
            }

            // 취소 버튼
            if (e.target.matches('.btn-edit-cancel')) {
                e.preventDefault();
                const displayArea = reviewItem.querySelector('.review-display');
                const editForm = reviewItem.querySelector('.review-edit-form');

                editForm.style.display = 'none';
                displayArea.style.display = 'block';
            }
        });
    }


    // =========================================
    // 5. 이미지 미리보기 기능 (통합)
    // =========================================
    function handleImagePreview(input, previewContainer) {
        if (!input || !previewContainer) return;

        input.addEventListener('change', function(e) {
            previewContainer.innerHTML = '';
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

    // 새 리뷰 폼 미리보기
    handleImagePreview(document.getElementById('photos'), document.getElementById('image-preview'));

    // 수정 폼 미리보기 (여러 개)
    document.querySelectorAll('.review-edit-form').forEach(form => {
        handleImagePreview(form.querySelector('.edit-photos-input'), form.querySelector('.edit-image-preview'));
    });

}); // DOMContentLoaded 끝


// =========================================
// 6. 전역 함수 (HTML inline 호출용)
// =========================================

// 리뷰 정렬
function changeSort(sortType) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set('sort', sortType);
    window.location.href = window.location.pathname + '?' + urlParams.toString() + '#panel-review';
}

// 기존 사진 삭제 (수정 시)
function removeExistingPhoto(button, photoUrl) {
    if (!confirm("이 사진을 삭제하시겠습니까?")) return;

    const container = button.parentElement;
    const form = container.closest('form');
    container.style.display = 'none';

    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'deletedPhotos';
    hiddenInput.value = photoUrl;

    form.querySelector('.deleted-photos-container').appendChild(hiddenInput);
}