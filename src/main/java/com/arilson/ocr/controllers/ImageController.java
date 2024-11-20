package com.arilson.ocr.controllers;

import com.arilson.ocr.services.ImageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/process")
    public String processImage(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return "Por favor, selecione uma imagem para upload.";
        }

        return imageService.enviarImagemParaFila(file);
    }
}
