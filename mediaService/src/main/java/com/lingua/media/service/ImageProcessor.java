package com.lingua.media.service;

import com.lingua.media.config.ApplicationProperties;
import com.lingua.media.exception.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded picture into the JPEG that is stored: decoded, scaled down if it is larger
 * than the configured box, and flattened on a white background.
 */
@Component
public class ImageProcessor {

    public static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    public static final String OUTPUT_EXTENSION = "jpg";

    /** Refused before decoding: a small file can declare a huge picture and exhaust the memory. */
    static final long MAX_PIXELS = 40_000_000L;

    private final int maxWidth;

    private final int maxHeight;

    public ImageProcessor(ApplicationProperties properties) {
        this.maxWidth = properties.media().imageMaxWidth();
        this.maxHeight = properties.media().imageMaxHeight();
    }

    /**
     * @param filename only used in error messages.
     * @return the bytes of the JPEG to store.
     */
    public byte[] toJpeg(byte[] content, String filename) {
        BufferedImage image = decode(content, filename);
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            image = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, maxWidth, maxHeight);
        }
        return encode(flatten(image), filename);
    }

    private static BufferedImage decode(byte[] content, String filename) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw unsupported(filename);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new BadRequestException("Image trop grande : " + filename);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BadRequestException badRequest) {
                throw badRequest;
            }
            // Corrupted or truncated file
            throw unsupported(filename);
        }
    }

    /** JPEG has no transparency: transparent areas become white. */
    private static BufferedImage flatten(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, Color.WHITE, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private static byte[] encode(BufferedImage image, String filename) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, OUTPUT_EXTENSION, output)) {
                throw unsupported(filename);
            }
        } catch (IOException e) {
            throw unsupported(filename);
        }
        return output.toByteArray();
    }

    private static BadRequestException unsupported(String filename) {
        return new BadRequestException("Image illisible ou format non pris en charge (JPEG, PNG, GIF, BMP) : " + filename);
    }
}
