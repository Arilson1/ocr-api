package com.arilson.ocr.services;

import com.arilson.ocr.model.OcrMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResultadoService {
    private final Map<String, OcrMessage> resultados = new ConcurrentHashMap<>();

    public void salvarResultado(OcrMessage resultado) {
        resultados.put(resultado.id(), resultado);
    }

    public Optional<OcrMessage> buscarResultado(String id) {
        return Optional.ofNullable(resultados.get(id));
    }
}
