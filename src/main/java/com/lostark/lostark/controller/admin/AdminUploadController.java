package com.lostark.lostark.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/upload")
public class AdminUploadController {

    @Value("${file.upload-dir.skills:C:/lostark-storage/skills/}")
    private String skillsUploadDir;

    /**
     * 관리자 전용 스킬 이미지 업로드
     */
    @PostMapping("/skill")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadSkillImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "파일이 비어있습니다."));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "skill.png";
        }

        // 확장자 추출 및 이미지 검증
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx > -1) {
            ext = originalFilename.substring(dotIdx).toLowerCase();
        }

        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp") && !ext.equals(".gif")) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "이미지 파일(.png, .jpg, .jpeg, .webp, .gif)만 업로드 가능합니다."));
        }

        String uploadPath = skillsUploadDir.endsWith("/") ? skillsUploadDir : skillsUploadDir + "/";
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 고유 파일명 생성 (UUID_원본명)
        String cleanOriginalName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String savedFileName = UUID.randomUUID().toString() + "_" + cleanOriginalName;
        File destination = new File(dir, savedFileName);

        try {
            file.transferTo(destination);
            String webUrl = "/uploads/skills/" + savedFileName;
            log.info("관리자 스킬 이미지 업로드 성공: {} -> {}", originalFilename, webUrl);
            return ResponseEntity.ok(Map.of("url", webUrl, "fileName", savedFileName));
        } catch (IOException e) {
            log.error("스킬 이미지 파일 저장 실패: {}", originalFilename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "파일 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
