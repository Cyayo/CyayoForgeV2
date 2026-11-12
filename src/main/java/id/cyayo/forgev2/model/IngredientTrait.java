package id.cyayo.forgev2.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class IngredientTrait {

    public enum Direction {
        BELOW,
        ABOVE,
        LEFT,
        RIGHT,
        TOUCHING,
        NOT_TOUCHING;

        public static Direction fromString(String str) {
            if (str == null) return null;
            String clean = str.trim().toUpperCase().replace("-", "_").replace(" ", "_");
            if (clean.equals("UNDER") || clean.equals("DOWN") || clean.equals("BOTTOM")) return BELOW;
            if (clean.equals("UP") || clean.equals("TOP")) return ABOVE;
            if (clean.equals("ADJACENT") || clean.equals("NEIGHBOR") || clean.equals("AROUND")) return TOUCHING;
            if (clean.equals("NOT_ADJACENT")) return NOT_TOUCHING;
            try {
                return Direction.valueOf(clean);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class EffectivenessModifier {
        private final Direction direction;
        private final double percentage;

        public EffectivenessModifier(Direction direction, double percentage) {
            this.direction = direction;
            this.percentage = percentage;
        }

        public Direction getDirection() {
            return direction;
        }

        public double getPercentage() {
            return percentage;
        }
    }

    public static class TraitAbility {
        private final String abilityId;
        private final String castMode;
        private final Map<String, Double> modifiers;

        public TraitAbility(String abilityId, String castMode, Map<String, Double> modifiers) {
            this.abilityId = abilityId;
            this.castMode = castMode != null ? castMode.toUpperCase().replace(" ", "_") : "ON_HIT";
            this.modifiers = modifiers != null ? modifiers : new HashMap<>();
        }

        public String getAbilityId() {
            return abilityId;
        }

        public String getCastMode() {
            return castMode;
        }

        public Map<String, Double> getModifiers() {
            return modifiers;
        }
    }

    private final String itemId;
    private final List<MaterialStatRange> weaponTraits = new ArrayList<>();
    private final List<MaterialStatRange> armorTraits = new ArrayList<>();
    private final List<EffectivenessModifier> effectivenessModifiers = new ArrayList<>();
    private TraitAbility weaponAbility;
    private TraitAbility armorAbility;

    public IngredientTrait(String itemId) {
        this.itemId = itemId != null ? itemId.trim().toUpperCase() : "";
    }

    public static IngredientTrait fromConfig(String itemId, ConfigurationSection section) {
        if (section == null) return null;
        IngredientTrait trait = new IngredientTrait(itemId);

        // 1. Parse traits (WEAPON & ARMOR)
        ConfigurationSection traitsSec = section.getConfigurationSection("traits");
        if (traitsSec != null) {
            List<String> wList = traitsSec.getStringList("WEAPON");
            for (String str : wList) {
                MaterialStatRange range = MaterialStatRange.parse(str);
                if (range != null) trait.weaponTraits.add(range);
            }
            List<String> aList = traitsSec.getStringList("ARMOR");
            for (String str : aList) {
                MaterialStatRange range = MaterialStatRange.parse(str);
                if (range != null) trait.armorTraits.add(range);
            }
        }

        // 2. Parse effectiveness (mendukung Section Map maupun List)
        if (section.isConfigurationSection("effectiveness")) {
            ConfigurationSection effSec = section.getConfigurationSection("effectiveness");
            if (effSec != null) {
                for (String dirKey : effSec.getKeys(false)) {
                    Direction dir = Direction.fromString(dirKey);
                    if (dir == null) continue;
                    String valStr = effSec.getString(dirKey, "").replace("%", "").replace("+", "").trim();
                    try {
                        double pct = Double.parseDouble(valStr);
                        trait.effectivenessModifiers.add(new EffectivenessModifier(dir, pct));
                    } catch (Exception ignored) {}
                }
            }
        } else {
            List<?> effList = section.getList("effectiveness");
            if (effList != null) {
                for (Object obj : effList) {
                    if (obj instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            String dirStr = String.valueOf(entry.getKey());
                            Direction dir = Direction.fromString(dirStr);
                            if (dir == null) continue;
                            String valStr = String.valueOf(entry.getValue()).replace("%", "").replace("+", "").trim();
                            try {
                                double pct = Double.parseDouble(valStr);
                                trait.effectivenessModifiers.add(new EffectivenessModifier(dir, pct));
                            } catch (Exception ignored) {}
                        }
                    } else if (obj instanceof String str) {
                        // Mendukung format "below: +50%"
                        String[] parts = str.split(":", 2);
                        if (parts.length == 2) {
                            Direction dir = Direction.fromString(parts[0]);
                            if (dir != null) {
                                String valStr = parts[1].replace("%", "").replace("+", "").trim();
                                try {
                                    double pct = Double.parseDouble(valStr);
                                    trait.effectivenessModifiers.add(new EffectivenessModifier(dir, pct));
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        }

        // 3. Parse ability (WEAPON & ARMOR)
        ConfigurationSection abilitySec = section.getConfigurationSection("ability");
        if (abilitySec != null) {
            ConfigurationSection wAbSec = abilitySec.getConfigurationSection("WEAPON");
            if (wAbSec != null) {
                trait.weaponAbility = parseAbility(wAbSec);
            }
            ConfigurationSection aAbSec = abilitySec.getConfigurationSection("ARMOR");
            if (aAbSec != null) {
                trait.armorAbility = parseAbility(aAbSec);
            }
        }

        return trait;
    }

    private static TraitAbility parseAbility(ConfigurationSection sec) {
        String id = sec.getString("ability-id");
        if (id == null || id.isEmpty()) return null;
        String castMode = sec.getString("cast-mode", "ON_HIT");
        Map<String, Double> mods = new HashMap<>();
        ConfigurationSection modSec = sec.getConfigurationSection("modifiers");
        if (modSec != null) {
            for (String k : modSec.getKeys(false)) {
                mods.put(k.toLowerCase(), modSec.getDouble(k));
            }
        }
        return new TraitAbility(id, castMode, mods);
    }

    public String getItemId() {
        return itemId;
    }

    public List<MaterialStatRange> getWeaponTraits() {
        return weaponTraits;
    }

    public List<MaterialStatRange> getArmorTraits() {
        return armorTraits;
    }

    public List<MaterialStatRange> getTraits(boolean isWeapon) {
        return isWeapon ? weaponTraits : armorTraits;
    }

    public List<EffectivenessModifier> getEffectivenessModifiers() {
        return effectivenessModifiers;
    }

    public TraitAbility getWeaponAbility() {
        return weaponAbility;
    }

    public TraitAbility getArmorAbility() {
        return armorAbility;
    }

    public TraitAbility getAbility(boolean isWeapon) {
        return isWeapon ? weaponAbility : armorAbility;
    }
}
