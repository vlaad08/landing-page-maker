package com.vlad.landing_page_maker.controller;

import com.vlad.landing_page_maker.controller.dto.GenerateRequest;
import com.vlad.landing_page_maker.service.GeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class GeneratorController {

    private final GeneratorService generatorService;

    public GeneratorController(GeneratorService generatorService){
        this.generatorService = generatorService;
    };

    @CrossOrigin(origins = "*")
    @PostMapping(
            value = "/generate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<byte[]> generatePage(@ModelAttribute GenerateRequest request) {
        try {
            byte[] zipBytes = generatorService.generate(request.context(), request.siteName());

            String safeFileName = request.siteName()
                    .replaceAll("[^a-zA-Z0-9-_]", "_");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + ".zip\"")
                    .body(zipBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error generating: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
