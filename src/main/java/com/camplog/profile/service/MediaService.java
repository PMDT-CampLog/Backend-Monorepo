package com.camplog.profile.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class MediaService {

    @Value("${app.media.storage-mode:local}")
    private String storageMode;

    @Value("${app.media.s3-bucket:camplog-dev-user-media}")
    private String s3Bucket;

    @Value("${app.media.s3-region:us-east-1}")
    private String s3Region;

    @Value("${app.media.cdn-base-url:http://localhost:3333/media}")
    private String cdnBaseUrl;

    @Value("${app.media.local-upload-dir:./uploads}")
    private String localUploadDir;

    @Value("${app.media.max-avatar-size:2097152}")
    private long maxAvatarSize;

    @Value("${app.media.max-cover-size:5242880}")
    private long maxCoverSize;

    @Value("${app.media.max-post-image-size:10485760}")
    private long maxPostImageSize;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if ("s3".equals(storageMode)) {
            this.s3Client = S3Client.builder()
                    .region(Region.of(s3Region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            log.info("MediaService inicializado no modo S3 (Bucket: {})", s3Bucket);
        } else {
            log.info("MediaService inicializado no modo LOCAL (Dir: {})", localUploadDir);
            try {
                Files.createDirectories(Paths.get(localUploadDir));
            } catch (IOException e) {
                log.error("Falha ao criar diretório de uploads local", e);
            }
        }
    }

    /**
     * Upload de avatar (redimensiona para 400x400 e converte para JPEG).
     */
    public MediaUploadResult uploadAvatar(MultipartFile file, String userId) {
        validateImage(file, maxAvatarSize, "avatar");
        String key = String.format("avatars/%s/avatar_%s.jpg", userId, UUID.randomUUID().toString().substring(0, 8));
        byte[] processed = resizeImage(file, 400, 400);
        return upload(key, processed, "image/jpeg");
    }

    /**
     * Upload de capa (redimensiona para 1200x400 e converte para JPEG).
     */
    public MediaUploadResult uploadCover(MultipartFile file, String userId) {
        validateImage(file, maxCoverSize, "capa");
        String key = String.format("covers/%s/cover_%s.jpg", userId, UUID.randomUUID().toString().substring(0, 8));
        byte[] processed = resizeImage(file, 1200, 400);
        return upload(key, processed, "image/jpeg");
    }

    /**
     * Upload de imagem de post (redimensiona para max 1200px de largura, mantendo aspect ratio).
     */
    public MediaUploadResult uploadPostImage(MultipartFile file, String userId, String postId) {
        validateImage(file, maxPostImageSize, "post");
        String key = String.format("posts/%s/%s/%s.jpg", userId, postId, UUID.randomUUID().toString().substring(0, 8));
        byte[] processed = resizeImage(file, 1200, 1200);
        return upload(key, processed, "image/jpeg");
    }

    /**
     * Deleta um arquivo de mídia pela chave.
     */
    public void deleteMedia(String mediaKey) {
        if ("s3".equals(storageMode)) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(mediaKey)
                        .build());
                log.info("Mídia deletada do S3: {}", mediaKey);
            } catch (Exception e) {
                log.error("Falha ao deletar mídia do S3: {}", mediaKey, e);
            }
        } else {
            try {
                Path filePath = Paths.get(localUploadDir, mediaKey);
                Files.deleteIfExists(filePath);
                log.info("Mídia deletada localmente: {}", mediaKey);
            } catch (IOException e) {
                log.error("Falha ao deletar mídia local: {}", mediaKey, e);
            }
        }
    }

    private MediaUploadResult upload(String key, byte[] data, String contentType) {
        if ("s3".equals(storageMode)) {
            return uploadToS3(key, data, contentType);
        } else {
            return uploadToLocal(key, data);
        }
    }

    private MediaUploadResult uploadToS3(String key, byte[] data, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(data)
            );
            String url = cdnBaseUrl + "/" + key;
            log.info("Upload S3 realizado com sucesso: {}", key);
            return new MediaUploadResult(url, key);
        } catch (Exception e) {
            log.error("Falha no upload para S3: {}", key, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha no upload da mídia.");
        }
    }

    private MediaUploadResult uploadToLocal(String key, byte[] data) {
        try {
            Path filePath = Paths.get(localUploadDir, key);
            Files.createDirectories(filePath.getParent());
            Files.copy(new ByteArrayInputStream(data), filePath, StandardCopyOption.REPLACE_EXISTING);
            String url = cdnBaseUrl + "/" + key;
            log.info("Upload local realizado com sucesso: {}", filePath);
            return new MediaUploadResult(url, key);
        } catch (IOException e) {
            log.error("Falha no upload local: {}", key, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha no upload da mídia.");
        }
    }

    private void validateImage(MultipartFile file, long maxSize, String type) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo de " + type + " é obrigatório.");
        }
        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("O arquivo de %s excede o tamanho máximo permitido de %d MB.", type, maxSize / (1024 * 1024)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo deve ser uma imagem válida (JPEG, PNG, WebP).");
        }
    }

    private byte[] resizeImage(MultipartFile file, int maxWidth, int maxHeight) {
        try (InputStream input = file.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.of(input)
                    .size(maxWidth, maxHeight)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Falha ao processar imagem", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar a imagem.");
        }
    }

    /**
     * Resultado de um upload de mídia contendo a URL pública e a chave de armazenamento.
     */
    public record MediaUploadResult(String url, String key) {}
}
