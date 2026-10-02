package com.lingua.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingua.media.config.ApplicationProperties;
import com.lingua.media.exception.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor(
        new ApplicationProperties(null, null, new ApplicationProperties.Media("images", "audio", 800, 600, DataSize.ofMegabytes(5)))
    );

    @Test
    void largePictureIsScaledDownToFitTheBoxKeepingItsProportions() throws IOException {
        BufferedImage stored = read(processor.toJpeg(png(1600, 800, BufferedImage.TYPE_INT_RGB), "wide.png"));

        assertThat(stored.getWidth()).isEqualTo(800);
        assertThat(stored.getHeight()).isEqualTo(400);
    }

    @Test
    void tallPictureIsLimitedByItsHeight() throws IOException {
        BufferedImage stored = read(processor.toJpeg(png(600, 1200, BufferedImage.TYPE_INT_RGB), "tall.png"));

        assertThat(stored.getHeight()).isEqualTo(600);
        assertThat(stored.getWidth()).isEqualTo(300);
    }

    @Test
    void smallPictureIsNotEnlarged() throws IOException {
        BufferedImage stored = read(processor.toJpeg(png(120, 80, BufferedImage.TYPE_INT_RGB), "small.png"));

        assertThat(stored.getWidth()).isEqualTo(120);
        assertThat(stored.getHeight()).isEqualTo(80);
    }

    @Test
    void transparentPictureIsStoredAsJpegOnAWhiteBackground() throws IOException {
        // Fully transparent PNG: JPEG cannot store transparency
        byte[] jpeg = processor.toJpeg(png(50, 50, BufferedImage.TYPE_INT_ARGB), "transparent.png");

        BufferedImage stored = read(jpeg);
        Color pixel = new Color(stored.getRGB(25, 25));
        assertThat(pixel.getRed()).isGreaterThan(250);
        assertThat(pixel.getGreen()).isGreaterThan(250);
        assertThat(pixel.getBlue()).isGreaterThan(250);
        // JPEG files start with FF D8
        assertThat(jpeg[0] & 0xFF).isEqualTo(0xFF);
        assertThat(jpeg[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    void fileThatIsNotAPictureIsRefused() {
        byte[] text = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> processor.toJpeg(text, "evil.png")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void truncatedPictureIsRefused() throws IOException {
        byte[] png = png(200, 200, BufferedImage.TYPE_INT_RGB);
        byte[] truncated = java.util.Arrays.copyOf(png, 40);

        assertThatThrownBy(() -> processor.toJpeg(truncated, "broken.png")).isInstanceOf(BadRequestException.class);
    }

    private static byte[] png(int width, int height, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        if (type == BufferedImage.TYPE_INT_RGB) {
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static BufferedImage read(byte[] content) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(content));
    }
}
