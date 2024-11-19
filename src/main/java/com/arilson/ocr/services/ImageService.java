package com.arilson.ocr.services;

import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import net.sourceforge.tess4j.*;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class ImageService {

    @Value("${tesseract.data.path}")
    private String tesseractPath;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${rabbitmq.routing.reply-key}")
    private String replyRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public ImageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String ProcessaImagem(MultipartFile file) {
        try {
            File tempImg = File.createTempFile("imagemTemp", file.getOriginalFilename());
            file.transferTo(tempImg);

            BufferedImage processaImagem = processaImagem(tempImg);

            String textoExtraido = extraiTexto(processaImagem);

            tempImg.delete();

            return "Texto:" + textoExtraido;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedImage processaImagem(File file) throws IOException {
        //Carrega imagem usando OpenCV
        Mat imagem = imread(file.getAbsolutePath());

        // Converter para escala de cinza
        Mat grayImagem = new Mat();
        cvtColor(imagem, grayImagem, COLOR_BGR2GRAY);

        //Remove ruídos
        Mat blurredImagem = new Mat();
        GaussianBlur(grayImagem, blurredImagem, new Size(5, 5), 0);

        //Ajusta Brilho e Contraste
        Mat equalizedImagem = new Mat();
        equalizeHist(blurredImagem, equalizedImagem);

        Mat binarizedImagem = new Mat();
        threshold(equalizedImagem, binarizedImagem, 0, 255, THRESH_BINARY | THRESH_OTSU);

        // Dilatação para refinar o texto
        Mat kernel = getStructuringElement(MORPH_RECT, new Size(2, 2));
        dilate(binarizedImagem, binarizedImagem, kernel);

        //converter a Mat para BufferedImage
        return Java2DFrameUtils.toBufferedImage(binarizedImagem);
    }

    private String extraiTexto(BufferedImage imagem) throws TesseractException {
        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath(tesseractPath);
        tesseract.setLanguage("por");

        return tesseract.doOCR(imagem);
    }
}
