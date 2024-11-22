package com.arilson.ocr.services;

import com.arilson.ocr.model.OcrMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

@Service
public class RabbitMQConsumer {

    private final ImageService imageService;
    private final ResultadoService resultadoService;
    private final RabbitTemplate rabbitTemplate;
    private final JedisService jedisService;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.reply-key}")
    private String replyRoutingKey;

    public RabbitMQConsumer(ImageService imageService, ResultadoService resultadoService, RabbitTemplate rabbitTemplate, JedisService jedisService) {
        this.imageService = imageService;
        this.resultadoService = resultadoService;
        this.rabbitTemplate = rabbitTemplate;
        this.jedisService = jedisService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void processarMensagem(OcrMessage mensagem) {
        try {
            File file = new File(mensagem.filePath());

            BufferedImage bufferedImage = imageService.processaImagem(file);
            String textoExtraido = imageService.extraiTexto(bufferedImage);

            OcrMessage mensagemOcr = new OcrMessage(
                    mensagem.id(),
                    mensagem.filePath(),
                    LocalDateTime.now(),
                    "CONCLUIDO",
                    textoExtraido
            );

            jedisService.save(mensagemOcr.id(), textoExtraido);
            rabbitTemplate.convertAndSend(exchange, replyRoutingKey, mensagemOcr);

            file.delete();
        } catch (Exception e) {
            OcrMessage erroMensagem = new OcrMessage(
                    mensagem.id(),
                    mensagem.filePath(),
                    LocalDateTime.now(),
                    "ERRO",
                    e.getMessage()
            );

            jedisService.save(erroMensagem.id(), "ERRO: " + e.getMessage());
            rabbitTemplate.convertAndSend(exchange, replyRoutingKey, erroMensagem);
            e.printStackTrace();
        }
    }
}
