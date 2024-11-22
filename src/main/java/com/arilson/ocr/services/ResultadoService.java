package com.arilson.ocr.services;

import com.arilson.ocr.model.OcrMessage;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ResultadoService {

    private final JedisService jedisService;

    public ResultadoService(JedisService jedisService) {
        this.jedisService = jedisService;
    }

    public Optional<String> buscarResultado(String id) {
        return Optional.ofNullable(jedisService.find(id));
    }
}
