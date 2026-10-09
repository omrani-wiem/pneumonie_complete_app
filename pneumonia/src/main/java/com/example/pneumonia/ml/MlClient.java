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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component 
public class MlClient {

    private final RestTemplate rest;
    private final String baseUrl;
    private final boolean mock;

    public MlClient(@Value("${ml.service.url}") String baseUrl,  @Value("${ml.mock}") boolean mock){

        this.baseUrl = baseUrl;
        this.mock = mock;

         SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(30_000);
        f.setReadTimeout(120_000);   // le Space gratuit peut être endormi
        this.rest = new RestTemplate(f);
    }
     
     public MlResult predict(MultipartFile file) throws IOException {
        if (mock) {
            return new MlResult("PNEUMONIA", 0.87, null);//je doit encore regarder ca 
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        return rest.postForObject(baseUrl + "/predict",
                new HttpEntity<>(body, headers), MlResult.class);
    }
}