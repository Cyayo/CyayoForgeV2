package id.cyayo.forgev2.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Event yang dipanggil sesaat sebelum item hasil tempaan diserahkan ke pemain.
 * Plugin lain bisa membatalkan event ini atau mengubah item hasil akhirnya.
 */
public class ForgeItemEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    
    private final Player crafter;
    private final ItemStack blueprint;
    private final List<ItemStack> materials;
    private ItemStack resultItem;
    private final double perfectionRate;
    
    private boolean cancelled = false;

    public ForgeItemEvent(Player crafter, ItemStack blueprint, List<ItemStack> materials, ItemStack resultItem, double perfectionRate) {
        this.crafter = crafter;
        this.blueprint = blueprint;
        this.materials = materials;
        this.resultItem = resultItem;
        this.perfectionRate = perfectionRate;
    }

    public Player getCrafter() { return crafter; }
    public ItemStack getBlueprint() { return blueprint; }
    public List<ItemStack> getMaterials() { return materials; }
    public ItemStack getResultItem() { return resultItem; }
    public double getPerfectionRate() { return perfectionRate; }

    /**
     * Memungkinkan plugin lain untuk mengganti item hasil akhirnya secara paksa.
     */
    public void setResultItem(ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
