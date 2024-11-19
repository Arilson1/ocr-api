package com.arilson.ocr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrMessage {
    private String id;
    private String filePath;
    private LocalDateTime timestamp;
    private String status;
    private String result;

    public OcrMessage(String id, String filePath) {
        this.id = id;
        this.filePath = filePath;
        this.timestamp = LocalDateTime.now();
        this.status = "PROCESSANDO";
    }
}
