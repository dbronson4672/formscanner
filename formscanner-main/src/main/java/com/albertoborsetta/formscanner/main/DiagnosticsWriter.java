package com.albertoborsetta.formscanner.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.albertoborsetta.formscanner.api.FormArea;
import com.albertoborsetta.formscanner.api.FormGroup;
import com.albertoborsetta.formscanner.api.FormPoint;
import com.albertoborsetta.formscanner.api.FormQuestion;
import com.albertoborsetta.formscanner.api.FormTemplate;
import com.albertoborsetta.formscanner.api.commons.Constants;
import com.albertoborsetta.formscanner.api.commons.Constants.Corners;
import com.albertoborsetta.formscanner.api.commons.Constants.FieldType;

/**
 * Collects run-time diagnostics for headless scans and persists them to a
 * human-readable report.
 */
class DiagnosticsWriter {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String INDENT = "  ";
    private static final String INDENT2 = INDENT + INDENT;
    private static final String INDENT3 = INDENT2 + INDENT;
    private static final String INDENT4 = INDENT3 + INDENT;

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DECIMAL_FORMAT = new DecimalFormat("0.###", symbols);
    }

    private final FormTemplate template;
    private final StringBuilder buffer = new StringBuilder();

    DiagnosticsWriter(FormTemplate template) {
        this.template = template;
        appendTemplateHeader();
    }

    void record(File imageFile, BufferedImage sourceImage, FormTemplate filledForm) {
        buffer.append("image: ").append(imageFile.getName()).append(NEW_LINE);
        buffer.append(INDENT).append("path: ").append(imageFile.getAbsolutePath()).append(NEW_LINE);

        if (sourceImage != null) {
            buffer.append(INDENT)
                  .append("imageSize: ")
                  .append(sourceImage.getWidth())
                  .append("x")
                  .append(sourceImage.getHeight())
                  .append(NEW_LINE);
        }

        appendLine(INDENT, "detectedDiagonal", formatDouble(filledForm.getDiagonal()));
        appendLine(INDENT, "detectedRotationRad", formatDouble(filledForm.getRotation()));
        appendLine(INDENT, "detectedRotationDeg", formatDouble(Math.toDegrees(filledForm.getRotation())));

        appendCornerSection("detectedCorners", filledForm.getCorners(), INDENT);
        appendCornerComparison(template.getCorners(), filledForm.getCorners());
        appendGroups(filledForm);
        appendDetectedAreas(filledForm);
        buffer.append(NEW_LINE);
    }

    void write(File target) throws IOException {
        if (target == null) {
            return;
        }

        File parent = target.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        Files.write(target.toPath(), buffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendTemplateHeader() {
        buffer.append("template: ").append(template.getName()).append(NEW_LINE);
        appendLine(INDENT, "version", valueOrNA(template.getVersion()));
        appendLine(INDENT, "threshold", valueOrNA(template.getThreshold()));
        appendLine(INDENT, "density", valueOrNA(template.getDensity()));
        appendLine(INDENT, "markerSize", valueOrNA(template.getSize()));
        appendLine(INDENT, "markerShape", template.getShape() != null ? template.getShape().getName() : "n/a");
        appendLine(INDENT, "cornerType", template.getCornerType() != null ? template.getCornerType().getName() : "n/a");
        appendLine(INDENT, "diagonal", formatDouble(template.getDiagonal()));
        appendLine(INDENT, "rotationRad", formatDouble(template.getRotation()));
        appendLine(INDENT, "rotationDeg", formatDouble(Math.toDegrees(template.getRotation())));
        appendCrop(template.getCrop());
        appendCornerSection("templateCorners", template.getCorners(), INDENT);
        appendTemplateAreas();
        appendTemplateGroups();
        buffer.append(NEW_LINE);
    }

    private void appendTemplateAreas() {
        buffer.append(INDENT).append("areas:").append(NEW_LINE);
        List<FormArea> areas = template.getFieldAreas();
        if (areas == null || areas.isEmpty()) {
            buffer.append(INDENT2).append("none").append(NEW_LINE);
            return;
        }

        for (FormArea area : areas) {
            appendAreaBlock(area, INDENT2);
        }
    }

    private void appendTemplateGroups() {
        buffer.append(INDENT).append("groups:").append(NEW_LINE);
        Map<String, FormGroup> groups = template.getGroups();
        if (groups == null || groups.isEmpty()) {
            buffer.append(INDENT2).append("none").append(NEW_LINE);
            return;
        }

        List<String> groupNames = new ArrayList<>(groups.keySet());
        Collections.sort(groupNames);

        for (String groupName : groupNames) {
            buffer.append(INDENT2).append("group: ").append(groupName).append(NEW_LINE);
            FormGroup group = groups.get(groupName);
            if (group == null) {
                continue;
            }
            appendTemplateQuestions(group);
        }
    }

    private void appendTemplateQuestions(FormGroup group) {
        Map<String, FormQuestion> fields = group.getFields();
        if (fields == null || fields.isEmpty()) {
            buffer.append(INDENT3).append("questions: none").append(NEW_LINE);
            return;
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        for (String fieldName : fieldNames) {
            FormQuestion question = fields.get(fieldName);
            if (question == null) {
                continue;
            }
            buffer.append(INDENT3)
                  .append("question: ")
                  .append(fieldName)
                  .append(" [type=")
                  .append(resolveFieldType(question.getType()))
                  .append(", multiple=")
                  .append(question.isMultiple())
                  .append(", rejectMultiple=")
                  .append(question.rejectMultiple())
                  .append("]")
                  .append(NEW_LINE);
            appendTemplateResponses(question, INDENT4);
        }
    }

    private void appendGroups(FormTemplate filledForm) {
        buffer.append(INDENT).append("results:").append(NEW_LINE);
        Map<String, FormGroup> templateGroups = template.getGroups();
        if (templateGroups == null || templateGroups.isEmpty()) {
            buffer.append(INDENT2).append("none").append(NEW_LINE);
            return;
        }

        Map<String, FormGroup> detectedGroups = filledForm.getGroups();
        List<String> groupNames = new ArrayList<>(templateGroups.keySet());
        Collections.sort(groupNames);

        for (String groupName : groupNames) {
            buffer.append(INDENT2).append("group: ").append(groupName).append(NEW_LINE);
            FormGroup templateGroup = templateGroups.get(groupName);
            FormGroup detectedGroup = detectedGroups != null ? detectedGroups.get(groupName) : null;
            appendDetectedQuestions(templateGroup, detectedGroup);
        }
    }

    private void appendDetectedQuestions(FormGroup templateGroup, FormGroup detectedGroup) {
        Map<String, FormQuestion> templateFields = templateGroup != null ? templateGroup.getFields() : null;
        if (templateFields == null || templateFields.isEmpty()) {
            buffer.append(INDENT3).append("questions: none").append(NEW_LINE);
            return;
        }

        Map<String, FormQuestion> detectedFields = detectedGroup != null ? detectedGroup.getFields() : null;
        List<String> fieldNames = new ArrayList<>(templateFields.keySet());
        Collections.sort(fieldNames);

        for (String fieldName : fieldNames) {
            FormQuestion templateQuestion = templateFields.get(fieldName);
            FormQuestion detectedQuestion = detectedFields != null ? detectedFields.get(fieldName) : null;

            buffer.append(INDENT3)
                  .append("question: ")
                  .append(fieldName)
                  .append(" [type=")
                  .append(templateQuestion != null ? resolveFieldType(templateQuestion.getType()) : "n/a")
                  .append("]")
                  .append(NEW_LINE);

            if (detectedQuestion == null || detectedQuestion.getPoints().isEmpty()) {
                buffer.append(INDENT4).append("detectedResponses: none").append(NEW_LINE);
                continue;
            }

            List<String> responses = new ArrayList<>(detectedQuestion.getPoints().keySet());
            Collections.sort(responses);
            buffer.append(INDENT4).append("detectedResponses:").append(NEW_LINE);
            for (String response : responses) {
                FormPoint point = detectedQuestion.getPoint(response);
                Double fillRatio = detectedQuestion.getResponseFillRatio(response);
                buffer.append(INDENT4)
                      .append(INDENT)
                      .append(labelForResponse(response))
                      .append(" -> ")
                      .append(formatPoint(point))
                      .append(" (fill=")
                      .append(fillRatio != null ? formatDouble(fillRatio) : "n/a")
                      .append(')')
                      .append(NEW_LINE);
            }
        }
    }

    private void appendTemplateResponses(FormQuestion question, String baseIndent) {
        Map<String, FormPoint> points = question.getPoints();
        if (points == null || points.isEmpty()) {
            buffer.append(baseIndent).append("responses: none").append(NEW_LINE);
            return;
        }

        List<String> responses = new ArrayList<>(points.keySet());
        Collections.sort(responses);
        buffer.append(baseIndent).append("responses:").append(NEW_LINE);
        for (String response : responses) {
            FormPoint point = points.get(response);
            buffer.append(baseIndent)
                  .append(INDENT)
                  .append(labelForResponse(response))
                  .append(" -> ")
                  .append(formatPoint(point))
                  .append(NEW_LINE);
        }
    }

    private void appendDetectedAreas(FormTemplate filledForm) {
        buffer.append(INDENT).append("detectedAreas:").append(NEW_LINE);
        List<FormArea> areas = filledForm.getFieldAreas();
        if (areas == null || areas.isEmpty()) {
            buffer.append(INDENT2).append("none").append(NEW_LINE);
            return;
        }

        for (FormArea area : areas) {
            appendAreaBlock(area, INDENT2);
        }
    }

    private void appendAreaBlock(FormArea area, String baseIndent) {
        if (area == null) {
            return;
        }

        FieldType type = area.getType();
        buffer.append(baseIndent)
              .append("area: ")
              .append(valueOrNA(area.getName()))
              .append(" [type=")
              .append(type != null ? type.getName() : "n/a")
              .append("]")
              .append(NEW_LINE);

        if (area.getText() != null) {
            buffer.append(baseIndent)
                  .append(INDENT)
                  .append("content: ")
                  .append(area.getText())
                  .append(NEW_LINE);
        }

        appendCornerSection("corners", area.getCorners(), baseIndent + INDENT);
    }

    private void appendCornerSection(String label, Map<Corners, FormPoint> corners, String baseIndent) {
        buffer.append(baseIndent).append(label).append(":");
        if (corners == null || corners.isEmpty()) {
            buffer.append(" none").append(NEW_LINE);
            return;
        }
        buffer.append(NEW_LINE);
        for (Corners corner : Corners.values()) {
            FormPoint point = corners.get(corner);
            buffer.append(baseIndent)
                  .append(INDENT)
                  .append(corner.getName())
                  .append(": ")
                  .append(formatPoint(point))
                  .append(NEW_LINE);
        }
    }

    private void appendCornerComparison(Map<Corners, FormPoint> templateCorners,
                                        Map<Corners, FormPoint> detectedCorners) {
        buffer.append(INDENT).append("cornerOffsets:");
        if (templateCorners == null || detectedCorners == null || templateCorners.isEmpty()
                || detectedCorners.isEmpty()) {
            buffer.append(" n/a").append(NEW_LINE);
            return;
        }

        buffer.append(NEW_LINE);
        for (Corners corner : Corners.values()) {
            FormPoint templatePoint = templateCorners.get(corner);
            FormPoint detectedPoint = detectedCorners.get(corner);
            if (templatePoint == null || detectedPoint == null) {
                continue;
            }
            double dx = detectedPoint.getX() - templatePoint.getX();
            double dy = detectedPoint.getY() - templatePoint.getY();
            buffer.append(INDENT2)
                  .append(corner.getName())
                  .append(": dx=")
                  .append(formatDouble(dx))
                  .append(", dy=")
                  .append(formatDouble(dy))
                  .append(NEW_LINE);
        }
    }

    private void appendCrop(Map<String, Integer> crop) {
        buffer.append(INDENT).append("crop: ");
        if (crop == null || crop.isEmpty()) {
            buffer.append("n/a").append(NEW_LINE);
            return;
        }

        buffer.append("top=")
              .append(valueOrNA(crop.get(Constants.TOP)))
              .append(", right=")
              .append(valueOrNA(crop.get(Constants.RIGHT)))
              .append(", bottom=")
              .append(valueOrNA(crop.get(Constants.BOTTOM)))
              .append(", left=")
              .append(valueOrNA(crop.get(Constants.LEFT)))
              .append(NEW_LINE);
    }

    private void appendLine(String indent, String label, Object value) {
        buffer.append(indent)
              .append(label)
              .append(": ")
              .append(valueOrNA(value))
              .append(NEW_LINE);
    }

    private String resolveFieldType(FieldType type) {
        return type != null ? type.getName() : "n/a";
    }

    private String labelForResponse(String response) {
        if (response == null || response.length() == 0) {
            return "<no response>";
        }
        if (Constants.NO_RESPONSE.equals(response)) {
            return "<no response>";
        }
        return response;
    }

    private String valueOrNA(Object value) {
        return value != null ? String.valueOf(value) : "n/a";
    }

    private String formatPoint(FormPoint point) {
        if (point == null) {
            return "(n/a, n/a)";
        }
        return "(" + formatDouble(point.getX()) + ", " + formatDouble(point.getY()) + ")";
    }

    private String formatDouble(double value) {
        synchronized (DECIMAL_FORMAT) {
            return DECIMAL_FORMAT.format(value);
        }
    }
}
