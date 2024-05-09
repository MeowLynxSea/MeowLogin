package cn.meowdream.meowlogin.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class FancyCaptchaGenerator {

    public String[] generateCaptcha() {
        // Generate random captcha text
        String captchaText = generateRandomCaptchaText();

        // Generate image
        BufferedImage image = createCaptchaImage(captchaText);

        // Convert image to base64
        String base64Image = encodeImageToBase64(image);

        return new String[]{base64Image, captchaText};
    }

    private static String generateRandomCaptchaText() {
        String characters = "0123456789@#%&";
        Random random = new Random();
        int length = random.nextInt(2) + 4; // Random length between 4 and 6
        StringBuilder captchaText = new StringBuilder();
        for (int i = 0; i < length; i++) {
            captchaText.append(characters.charAt(random.nextInt(characters.length())));
        }
        return captchaText.toString();
    }

    private static BufferedImage createCaptchaImage(String text) {
        int width = 128;
        int height = 128;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Set background gradient
        Color startColor = getRandomLightColor();
        Color endColor = getRandomLightColor();
        int startX = new Random().nextInt(width);
        int startY = new Random().nextInt(height);
        int endX = new Random().nextInt(width);
        int endY = new Random().nextInt(height);
        GradientPaint gradient = new GradientPaint(startX, startY, startColor, endX, endY, endColor);
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, width, height);

        // Draw random curves
        graphics.setColor(getRandomLightColor());
        for (int i = 0; i < 10; i++) {
            int x1 = new Random().nextInt(width);
            int y1 = new Random().nextInt(height);
            int x2 = new Random().nextInt(width);
            int y2 = new Random().nextInt(height);
            int ctrlx = new Random().nextInt(width);
            int ctrly = new Random().nextInt(height);
            graphics.draw(new QuadCurve2D.Float(x1, y1, ctrlx, ctrly, x2, y2));
        }

        // Draw random circles (outline only)
        graphics.setStroke(new BasicStroke(2)); // Set stroke width
        for (int i = 0; i < 5; i++) {
            int circleX = new Random().nextInt(width);
            int circleY = new Random().nextInt(height);
            int circleSize = new Random().nextInt(20) + 10; // Random size between 10 and 30
            graphics.setColor(getRandomLightColor());
            graphics.drawOval(circleX, circleY, circleSize, circleSize);
        }

        // Draw random triangles (outline only)
        for (int i = 0; i < 3; i++) {
            int[] xPoints = {new Random().nextInt(width), new Random().nextInt(width), new Random().nextInt(width)};
            int[] yPoints = {new Random().nextInt(height), new Random().nextInt(height), new Random().nextInt(height)};
            graphics.setColor(getRandomLightColor());
            graphics.drawPolygon(xPoints, yPoints, 3);
        }

        // Draw text with random rotation
        graphics.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (width - textWidth) / 2;
        int y = height / 2 + fm.getAscent() / 2;
        for (int i = 0; i < text.length(); i++) {
            graphics.setColor(getRandomLightColor());
            int rotation = new Random().nextInt(40) - 20; // Random rotation between -20 and 20 degrees
            graphics.rotate(Math.toRadians(rotation), x, y); // Rotate around the center of the character
            graphics.drawString(String.valueOf(text.charAt(i)), x, y);
            x += fm.charWidth(text.charAt(i));
            graphics.rotate(Math.toRadians(-rotation), x, y); // Reset rotation
        }

        graphics.dispose();
        return image;
    }

    private static Color getRandomDarkColor() {
        Random random = new Random();
        int red = 128 + random.nextInt(128);
        int green = 128 + random.nextInt(128);
        int blue = 128 + random.nextInt(128);
        return new Color(red, green, blue);
    }

    private static Color getRandomLightColor() {
        Random random = new Random();
        int red = random.nextInt(128);
        int green = random.nextInt(128);
        int blue = random.nextInt(128);
        return new Color(red, green, blue);
    }

    private static String encodeImageToBase64(BufferedImage image) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] imageBytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
