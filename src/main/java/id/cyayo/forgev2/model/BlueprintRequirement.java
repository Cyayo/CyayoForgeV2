package id.cyayo.forgev2.model;

public class BlueprintRequirement {

    private final String categoryKey;
    private final int amountRequired;

    public BlueprintRequirement(String categoryKey, int amountRequired) {
        this.categoryKey = categoryKey != null ? categoryKey.trim() : "NONE";
        this.amountRequired = Math.max(0, amountRequired);
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public int getAmountRequired() {
        return amountRequired;
    }

    public boolean isValid() {
        return !categoryKey.equalsIgnoreCase("NONE") && !categoryKey.isEmpty() && amountRequired > 0;
    }
}
