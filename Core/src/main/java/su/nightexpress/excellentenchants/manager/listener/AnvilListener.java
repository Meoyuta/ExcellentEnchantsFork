package su.nightexpress.excellentenchants.manager.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.enchantment.EnchantSettings;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.sound.VanillaSound;

import java.util.HashMap;
import java.util.Map;

public class AnvilListener extends AbstractListener<EnchantsPlugin> {

    private static final int MAX_FORCED_ENCHANT_LEVEL = 255;

    private final EnchantSettings settings;
    private final NamespacedKey rechargedKey;

    public AnvilListener(@NotNull EnchantsPlugin plugin, @NotNull EnchantSettings settings) {
        super(plugin);
        this.settings = settings;
        this.rechargedKey = new NamespacedKey(plugin, "item.recharged");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilRename(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        ItemStack result = event.getResult();

        if (first == null) first = new ItemStack(Material.AIR);
        if (second == null) second = new ItemStack(Material.AIR);
        if (result == null) result = new ItemStack(Material.AIR);

        this.updateTooExpensiveLimit(event.getView());

        if (this.handleRecharge(event, first, second)) return;

        this.anvilCombine(event, first, second, result);
    }

    private boolean anvilCombine(@NotNull PrepareAnvilEvent event, @NotNull ItemStack first, @NotNull ItemStack second, @NotNull ItemStack result) {
        ItemStack merged = new ItemStack(result.getType().isAir() ? first : result);
        MergeResult mergeResult = this.mergeConfiguredEnchantments(event, first, second, merged);
        ItemStack finalResult = mergeResult.changed() || !result.getType().isAir() ? merged : result;

        int countResult = EnchantsUtils.countCustomEnchantments(finalResult);
        int countItem;
        if (EnchantsUtils.isEnchantedBook(second)) {
            countItem = EnchantsUtils.countCustomEnchantments(first);
        }
        else if (first.getType() == second.getType()) {
            int countFirst = EnchantsUtils.countCustomEnchantments(first);
            int countSecond = EnchantsUtils.countCustomEnchantments(second);
            countItem = Math.max(countFirst, countSecond);
        }
        else return false;

        int limit = this.settings.getAnvilEnchantsLimit();

        if (countResult > countItem && (countItem >= limit || countResult > limit)) {
            event.setResult(null);
            return false;
        }

        Map<CustomEnchantment, Integer> chargesMap = new HashMap<>();
        EnchantsUtils.getCustomEnchantments(finalResult).forEach((enchantment, level) -> {
            int chargesFirst = enchantment.getCharges(first);
            int chargesSecond = enchantment.getCharges(second);

            chargesMap.put(enchantment, chargesFirst + chargesSecond);
            enchantment.setCharges(merged, level, chargesFirst + chargesSecond);
        });

        if (mergeResult.changed() || !chargesMap.isEmpty()) {
            event.setResult(merged);
            if (mergeResult.changed()) {
                this.updateRepairCost(event.getView(), mergeResult.conflicting(), mergeResult.changedEnchantments());
            }
            return true;
        }

        return false;
    }

    @NotNull
    private MergeResult mergeConfiguredEnchantments(@NotNull PrepareAnvilEvent event,
                                                   @NotNull ItemStack first,
                                                   @NotNull ItemStack second,
                                                   @NotNull ItemStack merged) {
        int extraMaxLevel = this.settings.getAnvilExtraMaxLevel();
        boolean allowConflicts = this.settings.isAnvilConflictingEnchantmentsAllowed();
        if (extraMaxLevel <= 0 && !allowConflicts) return MergeResult.EMPTY;
        if (first.getType().isAir() || second.getType().isAir()) return MergeResult.EMPTY;
        if (!this.isEnchantMergeInput(first, second)) return MergeResult.EMPTY;

        Map<Enchantment, Integer> secondEnchantments = EnchantsUtils.getEnchantments(second);
        if (secondEnchantments.isEmpty()) return MergeResult.EMPTY;

        boolean capLevel = event.getView().getPlayer() instanceof Player player && player.getGameMode() != GameMode.CREATIVE;
        boolean changed = false;
        boolean conflicting = false;
        int changedEnchantments = 0;

        for (Map.Entry<Enchantment, Integer> entry : secondEnchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int incomingLevel = entry.getValue();
            int currentLevel = EnchantsUtils.getLevel(merged, enchantment);

            if (!this.canApplyOnAnvil(first, enchantment, currentLevel)) continue;

            Map<Enchantment, Integer> currentEnchantments = EnchantsUtils.getEnchantments(merged);
            boolean hasConflicts = this.hasConflictingEnchantments(enchantment, currentEnchantments);
            if (hasConflicts && currentLevel <= 0 && !allowConflicts) continue;

            int combinedLevel = this.combineLevel(currentLevel, incomingLevel);
            int allowedLevel = this.getAllowedAnvilLevel(enchantment, capLevel, extraMaxLevel);
            int nextLevel = Math.min(combinedLevel, allowedLevel);
            if (nextLevel <= currentLevel) continue;

            if (!EnchantsUtils.add(merged, enchantment, nextLevel, true)) continue;

            changed = true;
            changedEnchantments++;
            if (hasConflicts) {
                conflicting = true;
            }
        }

        return changed ? new MergeResult(true, conflicting, changedEnchantments) : MergeResult.EMPTY;
    }

    private boolean isEnchantMergeInput(@NotNull ItemStack first, @NotNull ItemStack second) {
        return EnchantsUtils.isEnchantedBook(second) || first.getType() == second.getType();
    }

    private boolean canApplyOnAnvil(@NotNull ItemStack target, @NotNull Enchantment enchantment, int currentLevel) {
        return currentLevel > 0 || EnchantsUtils.isEnchantedBook(target) || enchantment.canEnchantItem(target);
    }

    private boolean hasConflictingEnchantments(@NotNull Enchantment enchantment, @NotNull Map<Enchantment, Integer> enchantments) {
        for (Enchantment other : enchantments.keySet()) {
            if (other.equals(enchantment)) continue;
            if (enchantment.conflictsWith(other) || other.conflictsWith(enchantment)) {
                return true;
            }
        }
        return false;
    }

    private int combineLevel(int currentLevel, int incomingLevel) {
        if (currentLevel <= 0) return incomingLevel;
        return currentLevel == incomingLevel ? currentLevel + 1 : Math.max(currentLevel, incomingLevel);
    }

    private int getAllowedAnvilLevel(@NotNull Enchantment enchantment, boolean capLevel, int extraMaxLevel) {
        if (!capLevel) return MAX_FORCED_ENCHANT_LEVEL;

        int vanillaMax = enchantment.getMaxLevel();
        long allowedLevel = (long) vanillaMax + extraMaxLevel;
        return (int) Math.min(MAX_FORCED_ENCHANT_LEVEL, Math.max(1L, allowedLevel));
    }

    private void updateRepairCost(@NotNull AnvilView anvilView, boolean conflicting, int changedEnchantments) {
        int repairCost = anvilView.getRepairCost();
        if (repairCost <= 0) {
            repairCost = Math.max(1, changedEnchantments);
        }

        if (conflicting) {
            repairCost = (int) Math.ceil((double) repairCost * this.settings.getAnvilConflictPenaltyMultiplier());
        }

        int finalRepairCost = Math.max(1, repairCost);
        this.plugin.runTask(() -> {
            anvilView.setRepairCost(finalRepairCost);
            this.applyTooExpensiveLimit(anvilView);
        });
    }

    private void updateTooExpensiveLimit(@NotNull AnvilView anvilView) {
        this.applyTooExpensiveLimit(anvilView);
        this.plugin.runTask(() -> this.applyTooExpensiveLimit(anvilView));
    }

    private void applyTooExpensiveLimit(@NotNull AnvilView anvilView) {
        if (!this.settings.isAnvilTooExpensiveLimitRemoved()) return;

        anvilView.setMaximumRepairCost(Integer.MAX_VALUE);
    }

    private boolean handleRecharge(@NotNull PrepareAnvilEvent event, @NotNull ItemStack first, @NotNull ItemStack second) {
        if (!Config.isChargesEnabled()) return false;
        if (second.getType().isAir()) return false;

        Map<CustomEnchantment, Integer> chargable = new HashMap<>();
        EnchantsUtils.getCustomEnchantments(first).forEach((data, level) -> {
            if (data.isChargesFuel(second) && !data.isFullOfCharges(first)) {
                chargable.put(data, level);
            }
        });
        if (chargable.isEmpty()) return false;

        int count;
        ItemStack recharged = new ItemStack(first);
        for (count = 0; count < second.getAmount() && !chargable.keySet().stream().allMatch(data -> data.isFullOfCharges(recharged)); ++count) {
            chargable.forEach((enchant, level) -> enchant.fuelCharges(recharged, level));
        }

        PDCUtil.set(recharged, this.rechargedKey, count);
        event.setResult(recharged);

        this.plugin.runTask(() -> {
            event.getView().setRepairCost(chargable.size());
            this.applyTooExpensiveLimit(event.getView());
        });
        return true;
    }



    @EventHandler(priority = EventPriority.NORMAL)
    public void onClickAnvil(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof AnvilInventory anvilInventory)) return;
        if (!(event.getView() instanceof AnvilView anvilView)) return;
        if (event.getRawSlot() != 2) return;

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        int count = PDCUtil.getInt(item, this.rechargedKey).orElse(0);
        if (count == 0) return;

        Player player = (Player) event.getWhoClicked();
        if (player.getLevel() < anvilView.getRepairCost()) return;

        player.setLevel(player.getLevel() - anvilView.getRepairCost());
        PDCUtil.remove(item, this.rechargedKey);
        event.getView().setCursor(item);
        event.setCancelled(false);

        VanillaSound.of(Sound.BLOCK_ENCHANTMENT_TABLE_USE).play(player);

        ItemStack second = anvilInventory.getItem(1);
        if (second != null && !second.getType().isAir()) {
            second.setAmount(second.getAmount() - count);
        }

        anvilInventory.setItem(0, null);
        anvilInventory.setItem(2, null);
    }

    private record MergeResult(boolean changed, boolean conflicting, int changedEnchantments) {

        private static final MergeResult EMPTY = new MergeResult(false, false, 0);
    }
}

