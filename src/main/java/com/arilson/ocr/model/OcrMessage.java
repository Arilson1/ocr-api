package com.arilson.ocr.model;

import java.time.LocalDateTime;

public record OcrMessage(String id, String filePath, LocalDateTime timeStamp, String status, String result) {

    public OcrMessage(String id, String filePath) {
        this(id, filePath, LocalDateTime.now(), "PROCESSANDO", null);
    }
}
