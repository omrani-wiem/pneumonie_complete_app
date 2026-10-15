package com.example.pneumonia.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class MlClient {

    private final RestTemplate rest;
    private final String baseUrl;

    public MlClient(@Value("${ml.service.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(30_000);
        f.setReadTimeout(120_000);   // le service hébergé gratuitement peut être endormi
        this.rest = new RestTemplate(f);
    }

    /** Appelle le service ML. Lève RestClientException si le service est injoignable ou répond mal. */
    public MlResult predict(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultipartFile source = file;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(source.getBytes()) {
            @Override
            public String getFilename() {
                return source.getOriginalFilename();
            }
        });

        MlResult r = rest.postForObject(baseUrl + "/predict", new HttpEntity<>(body, headers), MlResult.class);
        if (r == null || !("PNEUMONIA".equals(r.label()) || "NORMAL".equals(r.label()))) {
            throw new RestClientException("Réponse invalide du service de prédiction");
        }
        return r;
    }
}