package com.pmh.disosang.review.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload.dir}")
    private String uploadDir; // 파일 저장 경로

    @Value("${file.upload.url-prefix}")
    private String urlPrefix; // 웹 접근 URL 프리픽스

    public String uploadFile(MultipartFile file) throws IOException {
        //1. 디렉토리 생성 (없을시)
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // 2,파일명을 UUID와 확장자로만 구성하여 URL-Safe하게 만듭니다.
        String uuid = UUID.randomUUID().toString();
        String extension = "";

        // 파일 확장자 추출 (있는 경우)
        if (file.getOriginalFilename() != null) {
            int lastDot = file.getOriginalFilename().lastIndexOf('.');
            if (lastDot > 0) {
                extension = file.getOriginalFilename().substring(lastDot); // .png, .jpg 등
            }
        }

        // UUID + 확장자로 파일명 생성 (특수 문자 [ ] 완벽히 제거)
        String savedFileName = uuid + extension;

        //3.파일 저장 경로 설정
        Path filePath = Paths.get(uploadDir, savedFileName);

        //4.파일 저장
        file.transferTo(filePath.toFile());
        log.info("파일 저장 완료{}", filePath);

        //5.웹 접근 가능한 URL 반환
        return urlPrefix + savedFileName; // 예: /images/reviews/a1b2c3d4_photo.jpg


    }

    public void deleteFile(String fileUrl) {
        // fileUrl 예: /images/reviews/uuid.jpg
        // urlPrefix 예: /images/reviews/

        try {
            // URL에서 파일명만 추출
            String fileName = fileUrl.replace(urlPrefix, "");
            Path filePath = Paths.get(uploadDir, fileName);
            File file = filePath.toFile();

            if (file.exists()) {
                if (file.delete()) {
                    log.info("물리적 파일 삭제 성공: {}", filePath);
                } else {
                    log.warn("물리적 파일 삭제 실패: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", fileUrl, e);
        }
    }
}
