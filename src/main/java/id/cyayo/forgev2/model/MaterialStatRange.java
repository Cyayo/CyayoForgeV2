package id.cyayo.forgev2.model;

import java.util.concurrent.ThreadLocalRandom;

public class MaterialStatRange {

    private final String groupTarget;
    private final String statId;
    private final double min;
    private final double max;

    public MaterialStatRange(String statId, double min, double max) {
        this("ALL", statId, min, max);
    }

    public MaterialStatRange(String groupTarget, String statId, double min, double max) {
        this.groupTarget = groupTarget != null && !groupTarget.trim().isEmpty() ? groupTarget.trim().toUpperCase() : "ALL";
        this.statId = statId != null ? statId.trim().toUpperCase() : "";
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public static MaterialStatRange parse(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String raw = line.trim();

        String groupTarget = "ALL";
        if (raw.startsWith("[") && raw.contains("]")) {
            int closeIdx = raw.indexOf(']');
            groupTarget = raw.substring(1, closeIdx).trim().toUpperCase();
            raw = raw.substring(closeIdx + 1).trim();
        }

        String[] parts = raw.split(":", 2);
        if (parts.length < 2) return null;

        String stat = parts[0].trim().toUpperCase().replace("-", "_");
        String valStr = parts[1].trim();

        double min = 0.0;
        double max = 0.0;

        try {
            if (valStr.contains("-") && !valStr.startsWith("-")) {
                String[] range = valStr.split("-", 2);
                min = Double.parseDouble(range[0].trim());
                max = Double.parseDouble(range[1].trim());
            } else if (valStr.contains("..")) {
                String[] range = valStr.split("\\.\\.", 2);
                min = Double.parseDouble(range[0].trim());
                max = Double.parseDouble(range[1].trim());
            } else {
                double v = Double.parseDouble(valStr);
                min = v;
                max = v;
            }
            return new MaterialStatRange(groupTarget, stat, min, max);
        } catch (Exception e) {
            return null;
        }
    }

    public double roll() {
        if (Math.abs(max - min) < 0.0001) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public String getGroupTarget() {
        return groupTarget;
    }

    public String getStatId() {
        return statId;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean hasRange() {
        return Math.abs(max - min) > 0.0001;
    }
}

