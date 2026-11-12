package id.cyayo.forgev2.gui;

import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public interface CustomForgeGUI extends InventoryHolder {
    /**
     * Mengembalikan item dekorasi/placeholder bawaan untuk slot tertentu,
     * atau null jika slot tersebut awalnya kosong.
     */
    ItemStack getPlaceholder(int slot);
}
