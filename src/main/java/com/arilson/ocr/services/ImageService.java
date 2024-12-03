package com.arilson.ocr.services;

import com.arilson.ocr.model.OcrMessage;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.UUID;

import static org.bytedeco.opencv.global.opencv_core.bitwise_not;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class ImageService {

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${tesseract.data.path}")
    private String tesseractPath;

    @Value("${tesseract.language}")
    private String tesseractLanguage;

    private final RabbitTemplate rabbitTemplate;
    private final CorretorService corretorService;

    public ImageService(RabbitTemplate rabbitTemplate, CorretorService corretorService) {
        this.rabbitTemplate = rabbitTemplate;
        this.corretorService = corretorService;
    }

    public String enviarImagemParaFila(MultipartFile file) {
        try {
            File tempImg = File.createTempFile("imagemTemp", file.getOriginalFilename());
            file.transferTo(tempImg);

            String id = UUID.randomUUID().toString();

            OcrMessage message = new OcrMessage(id, tempImg.getAbsolutePath());

            rabbitTemplate.convertAndSend(exchange, routingKey, message);

            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage processaImagem(File file) throws IOException {
// Leitura da imagem
        Mat imagem = imread(file.getAbsolutePath());
        if (imagem.channels() > 1) {
            cvtColor(imagem, imagem, COLOR_BGR2GRAY);
        }

// Inversão de cores
        bitwise_not(imagem, imagem);

// Detecção de bordas
        Mat edges = new Mat();
        Canny(imagem, edges, 75, 200);

// Encontrar contornos
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(edges, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

// Criar uma imagem de resultado com fundo preto
        Mat resultado = Mat.zeros(imagem.size(), imagem.type()).asMat();

// Preencher cada contorno externo individualmente com branco
        IntPointer hierarchyPointer = new IntPointer(hierarchy.createBuffer());
        for (long i = 0; i < contours.size(); i++) {
            int parent = hierarchyPointer.get(4 * i + 3);

            Mat contour = contours.get(i);
            double area = contourArea(contour);

            if (area > 50) { // Filtro para área mínima
                MatVector contourVector = new MatVector(contour);

                if (parent == -1) {
                    fillPoly(resultado, contourVector, Scalar.WHITE);
                } else {
                    fillPoly(resultado, contourVector, Scalar.BLACK);
                }
            }
        }

// Salvar imagem processada
        salvarImagem(edges);
        salvarImagem(resultado);

        return Java2DFrameUtils.toBufferedImage(resultado);
    }


    public String extraiTexto(BufferedImage imagem) throws TesseractException {
        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath(tesseractPath);
        tesseract.setLanguage(tesseractLanguage);
        tesseract.setOcrEngineMode(2); // Legacy + LSTM

        // Configurações avançadas do Tesseract
        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setPageSegMode(6);

        // Remoção de caracteres problemáticos mais abrangente
        tesseract.setTessVariable("tessedit_char_blacklist", "{}[]()$#@%^&*<>~`|\\");

        // Ajustes para melhorar precisão
        tesseract.setTessVariable("textord_heavy_nr", "1");
        tesseract.setTessVariable("textord_force_make_prop_words", "1");
        tesseract.setTessVariable("edges_max_children_per_outline", "40");
        tesseract.setTessVariable("textord_min_linesize", "3.0");

        String textoExtraido = tesseract.doOCR(imagem);

//        return corretorService.corrigirTexto(textoExtraido);
        return textoExtraido;

    }

    public void salvarImagem(Mat imagem) {
        try {
            // Diretório para salvar as imagens
            String directoryPath = "c:" + File.separator + "imagens";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Gera um nome único para o arquivo
            String fileName = UUID.randomUUID().toString() + ".png";
            String filePath = directoryPath + File.separator + fileName;

            // Salva o Mat como arquivo de imagem usando OpenCV
            if (!imwrite(filePath, imagem)) {
                throw new IOException("Erro ao salvar a imagem processada.");
            }

            System.out.println("Imagem processada salva em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao salvar a imagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
