package com.camplog.pokedex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class DataPlatformClient {

    private final RestTemplate restTemplate;

    @Value("${data-platform.url:http://localhost:8000}")
    private String dataPlatformUrl;

    @Value("${data-platform.secret:camp-log-data-sec-123}")
    private String dataPlatformSecret;

    public DataPlatformClient() {
        this.restTemplate = new RestTemplate();
    }

    public String uploadMedia(MultipartFile file, String folder) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-CampLog-Signature", dataPlatformSecret);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.bin";
                }
            });
            body.add("folder", folder);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    dataPlatformUrl + "/api/v1/storage/upload",
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("url");
            } else {
                throw new RuntimeException("Data platform retornou erro: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo multipart", e);
        }
    }
}
