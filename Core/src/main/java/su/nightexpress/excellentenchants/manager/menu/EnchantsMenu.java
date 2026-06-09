package su.nightexpress.excellentenchants.manager.menu;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;
import su.nightexpress.nightcore.bridge.common.NightKey;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.menu.click.ClickResult;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.bridge.RegistryType;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.nightcore.util.text.night.NightMessage;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static su.nightexpress.excellentenchants.EnchantsPlaceholders.*;
import static su.nightexpress.nightcore.util.text.tag.Tags.*;

public class EnchantsMenu extends NormalMenu<EnchantsPlugin> implements ConfigBased {

    private static final String FILE_NAME = "enchants.yml";

    private static final String CONFLICTS = "%conflicts%";
    private static final String CHARGES   = "%charges%";
    private static final String ID_LINE   = LIGHT_YELLOW.wrap("ID: ") + LIGHT_GRAY.wrap(ENCHANTMENT_ID);

    private final NamespacedKey levelKey;

    private NightItem    enchantIcon;
    private String       enchantName;
    private List<String> enchantLoreMain;
    private List<String> enchantLoreConflicts;
    private List<String> enchantLoreCharges;
    private NightItem    nextPageIcon;
    private NightItem    previousPageIcon;
    private int[]        enchantSlots;

    public EnchantsMenu(@NotNull EnchantsPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X6, BLACK.wrap("Custom Enchantments"));
        this.levelKey = new NamespacedKey(plugin, "list_display_level");

        this.load(FileConfig.loadOrExtract(plugin, EnchantsFiles.DIR_MENU, FILE_NAME));
    }

    @Override
    public boolean open(@NotNull Player player) {
        return this.open(player, viewer -> {
            viewer.setPage(1);
            viewer.setRebuildMenu(true);
        });
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {

    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {
        this.renderPage(viewer, inventory);
    }

    @Override
    public void onClick(@NotNull MenuViewer viewer, @NotNull ClickResult result, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (result.isInventory()) return;

        int slot = result.getSlot();
        Inventory inventory = viewer.getInventory();
        if (slot == this.getNextPageSlot(inventory)) {
            this.goToPage(viewer, viewer.getPage() + 1);
            return;
        }

        if (slot == this.getPreviousPageSlot(inventory)) {
            this.goToPage(viewer, viewer.getPage() - 1);
            return;
        }

        if (!event.isLeftClick()) return;

        CustomEnchantment enchantment = this.getEnchantAtSlot(viewer, slot);
        if (enchantment == null) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;

        int levelHas = PDCUtil.getInt(currentItem, this.levelKey).orElse(1);
        if (++levelHas > enchantment.getDefinition().getMaxLevel()) {
            levelHas = 1;
        }

        ItemStack item = this.buildEnchantIcon(enchantment, levelHas).getItemStack();
        PDCUtil.set(item, this.levelKey, levelHas);
        event.setCurrentItem(item);
        viewer.getInventory().setItem(slot, item);
    }

    private void goToPage(@NotNull MenuViewer viewer, int page) {
        if (page < 1 || page > viewer.getPages() || page == viewer.getPage()) return;

        viewer.setPage(page);
        viewer.setRebuildMenu(false);
        this.flush(viewer);
    }

    private void renderPage(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {
        this.clearDynamicSlots(inventory);

        List<CustomEnchantment> enchantments = this.getVisibleEnchants();
        int[] pageSlots = this.getPageSlots(inventory);
        int perPage = pageSlots.length;
        int pages = perPage <= 0 ? 1 : Math.max(1, (int) Math.ceil((double) enchantments.size() / (double) perPage));
        int page = Math.clamp(viewer.getPage(), 1, pages);

        viewer.setPages(pages);
        viewer.setPage(page);

        if (perPage > 0) {
            int start = (page - 1) * perPage;
            for (int slotIndex = 0; slotIndex < perPage; slotIndex++) {
                int enchantIndex = start + slotIndex;
                if (enchantIndex >= enchantments.size()) break;

                int slot = pageSlots[slotIndex];
                if (!this.isInventorySlot(inventory, slot)) continue;

                ItemStack itemStack = this.buildEnchantIcon(enchantments.get(enchantIndex), 1).getItemStack();
                inventory.setItem(slot, itemStack);
            }
        }

        int previousPageSlot = this.getPreviousPageSlot(inventory);
        if (this.isInventorySlot(inventory, previousPageSlot)) {
            inventory.setItem(previousPageSlot, this.previousPageIcon.copy().getItemStack());
        }
        int nextPageSlot = this.getNextPageSlot(inventory);
        if (this.isInventorySlot(inventory, nextPageSlot)) {
            inventory.setItem(nextPageSlot, this.nextPageIcon.copy().getItemStack());
        }
    }

    private void clearDynamicSlots(@NotNull Inventory inventory) {
        int size = inventory.getSize();

        for (int slot : this.enchantSlots) {
            if (slot < 0 || slot >= size) continue;

            inventory.clear(slot);
        }

        int previousPageSlot = this.getPreviousPageSlot(inventory);
        if (this.isInventorySlot(inventory, previousPageSlot)) {
            inventory.clear(previousPageSlot);
        }
        int nextPageSlot = this.getNextPageSlot(inventory);
        if (this.isInventorySlot(inventory, nextPageSlot)) {
            inventory.clear(nextPageSlot);
        }
    }

    @NotNull
    private List<CustomEnchantment> getVisibleEnchants() {
        return EnchantRegistry.getRegistered().stream()
            .filter(Predicate.not(CustomEnchantment::isHiddenFromList))
            .sorted(Comparator.comparing((CustomEnchantment data) -> NightMessage.stripTags(data.getDisplayName()))
                .thenComparing(CustomEnchantment::getId))
            .toList();
    }

    private CustomEnchantment getEnchantAtSlot(@NotNull MenuViewer viewer, int slot) {
        int[] pageSlots = this.getPageSlots(viewer.getInventory());
        int slotIndex = this.getEnchantSlotIndex(pageSlots, slot);
        if (slotIndex < 0) return null;

        int enchantIndex = (viewer.getPage() - 1) * pageSlots.length + slotIndex;
        List<CustomEnchantment> enchantments = this.getVisibleEnchants();

        return enchantIndex >= 0 && enchantIndex < enchantments.size() ? enchantments.get(enchantIndex) : null;
    }

    private int getEnchantSlotIndex(int[] slots, int slot) {
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == slot) return index;
        }
        return -1;
    }

    private int[] getPageSlots(@NotNull Inventory inventory) {
        int previousPageSlot = this.getPreviousPageSlot(inventory);
        int nextPageSlot = this.getNextPageSlot(inventory);

        return Arrays.stream(this.enchantSlots)
            .filter(slot -> this.isInventorySlot(inventory, slot))
            .filter(slot -> slot != previousPageSlot && slot != nextPageSlot)
            .distinct()
            .toArray();
    }

    private int getPreviousPageSlot(@NotNull Inventory inventory) {
        int size = inventory.getSize();
        return size <= 0 ? -1 : Math.max(0, size - 9);
    }

    private int getNextPageSlot(@NotNull Inventory inventory) {
        return inventory.getSize() - 1;
    }

    private boolean isInventorySlot(@NotNull Inventory inventory, int slot) {
        return slot >= 0 && slot < inventory.getSize();
    }

    @NotNull
    private NightItem buildEnchantIcon(@NotNull CustomEnchantment enchant, int level) {
        List<String> conflicts = new ArrayList<>();
        if (enchant.getDefinition().hasConflicts()) {
            for (String line : this.enchantLoreConflicts) {
                if (line.contains(GENERIC_NAME)) {
                    enchant.getDefinition().getExclusiveSet().stream()
                        .map(NightKey::key)
                        .map(NightKey::toBukkit)
                        .map(key -> BukkitThing.getByKey(RegistryType.ENCHANTMENT, key)).filter(Objects::nonNull).map(LangUtil::getSerializedName)
                        .forEach(conf -> conflicts.add(line.replace(GENERIC_NAME, conf)));
                    continue;
                }
                conflicts.add(line);
            }
        }

        List<String> charges = Replacer.create()
            .replace(GENERIC_AMOUNT, () -> NumberUtil.format(enchant.getCharges().getMaxAmount(level)))
            .replace(GENERIC_ITEM, () -> ItemUtil.getNameSerialized(enchant.getFuel()))
            .apply(enchant.isChargeable() ? this.enchantLoreCharges : Collections.emptyList());

        return this.enchantIcon.copy().hideAllComponents()
            .setDisplayName(this.enchantName)
            .setLore(this.enchantLoreMain)
            .replacement(replacer -> replacer
                .replace(CHARGES, charges)
                .replace(CONFLICTS, conflicts)
                .replace(enchant.replacePlaceholders(level))
            );
    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        this.enchantIcon = ConfigValue.create("Enchantment.Icon", new NightItem(Material.ENCHANTED_BOOK)).read(config);
        this.nextPageIcon = ConfigValue.create("Navigation.Next_Icon", NightItem.fromType(Material.ARROW)
            .setDisplayName(LIGHT_YELLOW.wrap("下一页 / Next Page"))
            .setLore(Lists.newList(LIGHT_GRAY.wrap("打开下一页。 / Open the next page.")))).read(config);
        this.previousPageIcon = ConfigValue.create("Navigation.Previous_Icon", NightItem.fromType(Material.ARROW)
            .setDisplayName(LIGHT_YELLOW.wrap("上一页 / Previous Page"))
            .setLore(Lists.newList(LIGHT_GRAY.wrap("打开上一页。 / Open the previous page.")))).read(config);

        this.enchantName = ConfigValue.create("Enchantment.Name",
            LIGHT_YELLOW.wrap(BOLD.wrap(ENCHANTMENT_NAME + " " + ENCHANTMENT_LEVEL))
        ).read(config);

        this.enchantLoreMain = ConfigValue.create("Enchantment.Lore.Main",
            Lists.newList(
                ENCHANTMENT_DESCRIPTION_REPLACED,
                DARK_GRAY.wrap("(click to switch levels)"),
                EMPTY_IF_ABOVE,
                LIGHT_YELLOW.wrap(BOLD.wrap("Info:")),
                ID_LINE,
                LIGHT_YELLOW.wrap("▪ " + LIGHT_GRAY.wrap("Applies to: ") + ENCHANTMENT_FIT_ITEM_TYPES),
                LIGHT_YELLOW.wrap("▪ " + LIGHT_GRAY.wrap("Levels: ") + ENCHANTMENT_LEVEL_MIN + LIGHT_GRAY.wrap(" - ") + ENCHANTMENT_LEVEL_MAX),
                EMPTY_IF_BELOW,
                CHARGES,
                EMPTY_IF_BELOW,
                CONFLICTS
            )).read(config);
        this.enchantLoreMain = withEnchantIdLine(this.enchantLoreMain);

        this.enchantLoreConflicts = ConfigValue.create("Enchantment.Lore.Conflicts",
            Lists.newList(
                LIGHT_RED.wrap(BOLD.wrap("Conflicts:")),
                LIGHT_RED.wrap("✘ ") + LIGHT_GRAY.wrap(GENERIC_NAME)
            )).read(config);

        this.enchantLoreCharges = ConfigValue.create("Enchantment.Lore.Charges",
            Lists.newList(
                LIGHT_YELLOW.wrap("▪ " + LIGHT_GRAY.wrap("Charges: ") + GENERIC_AMOUNT + "⚡" + LIGHT_GRAY.wrap(" (" + WHITE.wrap(GENERIC_ITEM) + ")"))
            )).read(config);

        this.enchantSlots = Arrays.stream(ConfigValue.create("Enchantment.Slots", IntStream.range(0, 45).toArray()).read(config))
            .distinct()
            .toArray();
    }

    @NotNull
    private static List<String> withEnchantIdLine(@NotNull List<String> lore) {
        if (lore.stream().anyMatch(line -> line.contains(ENCHANTMENT_ID))) return lore;

        List<String> updated = new ArrayList<>(lore);
        int insertIndex = updated.size();
        for (int index = 0; index < updated.size(); index++) {
            String line = updated.get(index);
            if (line.contains(ENCHANTMENT_FIT_ITEM_TYPES) || line.contains(ENCHANTMENT_LEVEL_MIN)) {
                insertIndex = index;
                break;
            }
        }
        updated.add(insertIndex, ID_LINE);
        return updated;
    }
}
