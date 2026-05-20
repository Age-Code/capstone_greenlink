package com.greenlink.greenlink.service;

import com.greenlink.greenlink.common.constants.S3Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    public String uploadUserPlantImage(
            MultipartFile file,
            Long userPlantId
    ) {
        validateImageFile(file);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = createStoredFilename(originalFilename, userPlantId);
        String key = S3Paths.USER_PLANT_IMAGE_PREFIX + storedFilename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return createS3Url(key);
        } catch (IOException e) {
            throw new IllegalStateException("S3 이미지 업로드에 실패했습니다.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 필요합니다.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        long maxSize = 20 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 파일은 20MB 이하만 업로드할 수 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        String extension = getExtension(originalFilename).toLowerCase();

        Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "webp");

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("jpg, jpeg, png, webp 파일만 업로드할 수 있습니다.");
        }
    }

    private String createStoredFilename(String originalFilename, Long userPlantId) {
        String extension = getExtension(originalFilename).toLowerCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        if (userPlantId == null) {
            return "grow-space-" + timestamp + "-" + uuid + "." + extension;
        }

        return "user-plant-" + userPlantId + "-" + timestamp + "-" + uuid + "." + extension;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        return filename.substring(dotIndex + 1);
    }

    private String createS3Url(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
