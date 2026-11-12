package id.cyayo.forgev2.model;

import org.bukkit.configuration.ConfigurationSection;

public class ShinyTracker {

    private final String id;
    private final String displayName;
    private final String triggerType;
    private final String appliesTo;
    private final int weight;

    public ShinyTracker(String id, String displayName, String triggerType, String appliesTo, int weight) {
        this.id = id != null ? id.trim().toLowerCase() : "unknown";
        this.displayName = displayName != null ? displayName : id;
        this.triggerType = triggerType != null ? triggerType.trim().toUpperCase() : "CUSTOM";
        this.appliesTo = appliesTo != null ? appliesTo.trim().toUpperCase() : "ALL";
        this.weight = Math.max(1, weight);
    }

    public static ShinyTracker fromSection(String id, ConfigurationSection sec) {
        if (sec == null) return null;
        String name = sec.getString("display-name", id);
        String trigger = sec.getString("trigger", "CUSTOM");
        String applies = sec.getString("applies-to", "ALL");
        int w = sec.getInt("weight", 10);
        return new ShinyTracker(id, name, trigger, applies, w);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public int getWeight() {
        return weight;
    }

    public boolean matchesTarget(boolean isWeapon, String targetType) {
        if (appliesTo.equals("ALL")) return true;
        if (appliesTo.equals("WEAPON") && isWeapon) return true;
        if (appliesTo.equals("ARMOR") && !isWeapon) return true;
        if (targetType != null && appliesTo.equalsIgnoreCase(targetType.trim())) return true;
        return false;
    }
}
