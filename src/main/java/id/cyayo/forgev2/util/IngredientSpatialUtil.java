package id.cyayo.forgev2.util;

import id.cyayo.forgev2.model.IngredientTrait;

import java.util.*;

public class IngredientSpatialUtil {

    /**
     * Menghitung daftar target slot ingredient yang terkena efek posisi dari sourceSlot.
     * Hanya slot yang terdaftar di allIngredientSlots yang dianggap valid sebagai target.
     */
    public static Set<Integer> getTargetSlots(int sourceSlot, IngredientTrait.Direction direction, Collection<Integer> allIngredientSlots) {
        Set<Integer> targets = new HashSet<>();
        if (allIngredientSlots == null || allIngredientSlots.isEmpty()) return targets;

        int row = sourceSlot / 9;
        int col = sourceSlot % 9;

        Set<Integer> touchingSlots = new HashSet<>();
        // Below
        int belowSlot = (row + 1) * 9 + col;
        if (allIngredientSlots.contains(belowSlot)) touchingSlots.add(belowSlot);

        // Above
        int aboveSlot = (row - 1) * 9 + col;
        if (row > 0 && allIngredientSlots.contains(aboveSlot)) touchingSlots.add(aboveSlot);

        // Left
        if (col > 0) {
            int leftSlot = sourceSlot - 1;
            if (allIngredientSlots.contains(leftSlot)) touchingSlots.add(leftSlot);
        }

        // Right
        if (col < 8) {
            int rightSlot = sourceSlot + 1;
            if (allIngredientSlots.contains(rightSlot)) touchingSlots.add(rightSlot);
        }

        switch (direction) {
            case BELOW:
                if (allIngredientSlots.contains(belowSlot)) targets.add(belowSlot);
                break;
            case ABOVE:
                if (row > 0 && allIngredientSlots.contains(aboveSlot)) targets.add(aboveSlot);
                break;
            case LEFT:
                if (col > 0 && allIngredientSlots.contains(sourceSlot - 1)) targets.add(sourceSlot - 1);
                break;
            case RIGHT:
                if (col < 8 && allIngredientSlots.contains(sourceSlot + 1)) targets.add(sourceSlot + 1);
                break;
            case TOUCHING:
                targets.addAll(touchingSlots);
                break;
            case NOT_TOUCHING:
                for (int slot : allIngredientSlots) {
                    if (slot != sourceSlot && !touchingSlots.contains(slot)) {
                        targets.add(slot);
                    }
                }
                break;
        }

        return targets;
    }

    /**
     * Menghitung persentase efektivitas total untuk setiap slot ingredient yang aktif.
     * Menggunakan rumus akumulasi linear aditif:
     * Efektivitas Awal = 100%
     * Efektivitas Akhir = 100% + SUM(Modifier dari slot lain yang mengarah ke slot ini).
     */
    public static Map<Integer, Double> calculateSlotEffectiveness(
            List<Integer> allIngredientSlots,
            Map<Integer, IngredientTrait> activeIngredients) {

        Map<Integer, Double> effectivenessMap = new HashMap<>();
        if (allIngredientSlots == null || allIngredientSlots.isEmpty()) return effectivenessMap;

        // 1. Inisialisasi setiap slot aktif dengan efektivitas dasar 100%
        for (int slot : allIngredientSlots) {
            if (activeIngredients.containsKey(slot)) {
                effectivenessMap.put(slot, 100.0);
            }
        }

        // 2. Positional sweep: setiap slot aktif memberikan modifier ke slot targetnya
        for (Map.Entry<Integer, IngredientTrait> sourceEntry : activeIngredients.entrySet()) {
            int sourceSlot = sourceEntry.getKey();
            IngredientTrait trait = sourceEntry.getValue();
            if (trait == null) continue;

            for (IngredientTrait.EffectivenessModifier mod : trait.getEffectivenessModifiers()) {
                Set<Integer> targets = getTargetSlots(sourceSlot, mod.getDirection(), allIngredientSlots);
                for (int targetSlot : targets) {
                    if (effectivenessMap.containsKey(targetSlot)) {
                        double current = effectivenessMap.get(targetSlot);
                        effectivenessMap.put(targetSlot, current + mod.getPercentage());
                    }
                }
            }
        }

        return effectivenessMap;
    }
}
