package com.albertoborsetta.formscanner.main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.albertoborsetta.formscanner.api.FormGroup;
import com.albertoborsetta.formscanner.api.FormPoint;
import com.albertoborsetta.formscanner.api.FormQuestion;
import com.albertoborsetta.formscanner.api.FormTemplate;
import com.albertoborsetta.formscanner.api.commons.Constants;
import com.albertoborsetta.formscanner.api.commons.Constants.ShapeType;

/**
 * Produces annotated versions of scanned forms showing detected answers.
 */
class AnnotatedImageWriter {

    private static final Logger logger = LogManager.getLogger(AnnotatedImageWriter.class);

    private final FormTemplate template;

    AnnotatedImageWriter(FormTemplate template) {
        this.template = template;
    }

    void write(File imageFile, BufferedImage sourceImage, FormTemplate filledForm) {
        if (imageFile == null || sourceImage == null || filledForm == null) {
            return;
        }

        BufferedImage annotated = duplicate(sourceImage);
        Graphics2D g = annotated.createGraphics();
        try {
            g.drawImage(sourceImage, 0, 0, null);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int markerSize = determineMarkerSize();
            ShapeType shape = template.getShape();

            drawDetectedCorners(g, filledForm);

            Map<String, FormGroup> groups = filledForm.getGroups();
            if (groups != null && !groups.isEmpty()) {
                List<String> groupNames = new ArrayList<>(groups.keySet());
                Collections.sort(groupNames);
                for (String groupName : groupNames) {
                    FormGroup group = groups.get(groupName);
                    if (group == null) {
                        continue;
                    }
                    Map<String, FormQuestion> questions = group.getFields();
                    if (questions == null || questions.isEmpty()) {
                        continue;
                    }
                    List<String> questionNames = new ArrayList<>(questions.keySet());
                    Collections.sort(questionNames);
                    for (String questionName : questionNames) {
                        FormQuestion question = questions.get(questionName);
                        if (question == null) {
                            continue;
                        }
                        drawResponses(g, question, markerSize, shape);
                    }
                }
            }
        } finally {
            g.dispose();
        }

        File target = deriveTarget(imageFile);
        try {
            String format = determineFormat(target);
            ImageIO.write(annotated, format, target);
        } catch (IOException e) {
            logger.debug("Error", e);
        }
    }

    private void drawDetectedCorners(Graphics2D g, FormTemplate filledForm) {
        Map<com.albertoborsetta.formscanner.api.commons.Constants.Corners, FormPoint> corners = filledForm.getCorners();
        if (corners == null || corners.isEmpty()) {
            return;
        }

        Color previous = g.getColor();
        g.setColor(new Color(0, 180, 0));
        g.setStroke(new BasicStroke(3f));

        com.albertoborsetta.formscanner.api.commons.Constants.Corners[] values = com.albertoborsetta.formscanner.api.commons.Constants.Corners.values();
        for (int i = 0; i < values.length; i++) {
            FormPoint start = corners.get(values[i]);
            FormPoint end = corners.get(values[(i + 1) % values.length]);
            if (start == null || end == null) {
                continue;
            }
            g.drawLine((int) Math.round(start.getX()), (int) Math.round(start.getY()),
                    (int) Math.round(end.getX()), (int) Math.round(end.getY()));
        }

        g.setColor(previous);
    }

    private void drawResponses(Graphics2D g, FormQuestion question, int markerSize, ShapeType shape) {
        Map<String, FormPoint> responses = question.getPoints();
        if (responses == null || responses.isEmpty()) {
            return;
        }

        g.setStroke(new BasicStroke(2f));
        Color fillColor = new Color(255, 0, 0, 200);
        Color borderColor = new Color(255, 0, 0);

        for (FormPoint point : responses.values()) {
            if (point == null || point == Constants.EMPTY_POINT) {
                continue;
            }
            int diameter = markerSize > 0 ? markerSize : 20;
            int radius = diameter / 2;
            int x = (int) Math.round(point.getX());
            int y = (int) Math.round(point.getY());

            if (shape == ShapeType.SQUARE) {
                g.setColor(fillColor);
                g.fillRect(x - radius, y - radius, diameter, diameter);
                g.setColor(borderColor);
                g.drawRect(x - radius, y - radius, diameter, diameter);
            } else {
                g.setColor(fillColor);
                g.fillOval(x - radius, y - radius, diameter, diameter);
                g.setColor(borderColor);
                g.drawOval(x - radius, y - radius, diameter, diameter);
            }
        }
    }

    private BufferedImage duplicate(BufferedImage image) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), type);
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }

    private int determineMarkerSize() {
        Integer size = template.getSize();
        if (size == null || size <= 0) {
            return 20;
        }
        return size;
    }

    private File deriveTarget(File imageFile) {
        String name = imageFile.getName();
        String extension = FilenameUtils.getExtension(name);
        String base = FilenameUtils.removeExtension(name);
        String diagName;
        if (extension.isEmpty()) {
            diagName = base + ".diag";
        } else {
            diagName = base + ".diag." + extension;
        }
        return new File(imageFile.getParentFile(), diagName);
    }

    private String determineFormat(File target) {
        String extension = FilenameUtils.getExtension(target.getName());
        if (extension == null || extension.isEmpty()) {
            return "png";
        }
        return extension.toLowerCase(Locale.ROOT);
    }
}
