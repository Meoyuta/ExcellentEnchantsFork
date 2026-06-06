package su.nightexpress.excellentenchants.enchantment.universal;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.type.ResurrectEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UnyieldingEnchant extends GameEnchantment implements ResurrectEnchant {

    public static final String ID = "unyielding";

    private final Map<UUID, DamageMemory> damageMemoryMap;

    private int    minimumStreak;
    private double reductionBase;
    private double immunityThreshold;
    private int    ancientCityMinBooks;
    private int    ancientCityMaxBooks;

    public UnyieldingEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.damageMemoryMap = new HashMap<>();

        this.addPlaceholder("%unyielding_minimum_streak%", level -> NumberUtil.format(this.minimumStreak));
        this.addPlaceholder("%unyielding_reduction_base%", level -> NumberUtil.format(this.reductionBase));
        this.addPlaceholder("%unyielding_immunity_threshold%", level -> NumberUtil.format(this.immunityThreshold));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.minimumStreak = Math.max(1, ConfigValue.create("Unyielding.Minimum_Streak",
            3,
            "How many consecutive hits of the same damage type must be recorded before damage reduction is applied."
        ).read(config));

        this.reductionBase = Math.clamp(ConfigValue.create("Unyielding.Reduction_Base",
            0.95D,
            "Damage multiplier base for every recorded consecutive hit of the same damage type.",
            "Example: 5 recorded fire hits means the 6th fire hit becomes damage * (0.95 ^ 5)."
        ).read(config), 0D, 1D);

        this.immunityThreshold = Math.max(0D, ConfigValue.create("Unyielding.Immunity_Threshold",
            0.001D,
            "If reduced damage falls below this value, the damage is set to zero."
        ).read(config));

        this.ancientCityMinBooks = Math.max(0, ConfigValue.create("Unyielding.Ancient_City_Loot.Min_Books",
            1,
            "Minimum unyielding enchanted books added to Ancient City loot."
        ).read(config));

        this.ancientCityMaxBooks = Math.max(this.ancientCityMinBooks, ConfigValue.create("Unyielding.Ancient_City_Loot.Max_Books",
            2,
            "Maximum unyielding enchanted books added to Ancient City loot."
        ).read(config));
    }

    @Override
    @NotNull
    public EnchantPriority getResurrectPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onResurrect(@NotNull EntityResurrectEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;

        this.damageMemoryMap.put(player.getUniqueId(), new DamageMemory());
        return true;
    }

    public void handleDamage(@NotNull EntityDamageEvent event, @NotNull Player player) {
        DamageMemory memory = this.damageMemoryMap.get(player.getUniqueId());
        if (memory == null) return;
        if (event.getDamage() <= 0D) return;

        NamespacedKey damageKey = event.getDamageSource().getDamageType().getKey();
        String damageId = damageKey.asString();

        int previousStreak = memory.damageId != null && memory.damageId.equals(damageId) ? memory.streak : 0;

        if (previousStreak >= this.minimumStreak) {
            double reducedDamage = event.getDamage() * Math.pow(this.reductionBase, previousStreak);
            event.setDamage(reducedDamage < this.immunityThreshold ? 0D : Math.max(0D, reducedDamage));
        }

        if (previousStreak == 0) {
            memory.damageId = damageId;
            memory.streak = 1;
        }
        else {
            memory.streak++;
        }
    }

    public void clear(@NotNull Player player) {
        this.damageMemoryMap.remove(player.getUniqueId());
    }

    public void populateAncientCityLoot(@NotNull LootGenerateEvent event) {
        if (!this.isAncientCityLoot(event)) return;
        if (this.ancientCityMaxBooks <= 0) return;

        int amount = ThreadLocalRandom.current().nextInt(this.ancientCityMinBooks, this.ancientCityMaxBooks + 1);
        for (int index = 0; index < amount; index++) {
            event.getLoot().add(this.createBook());
        }
    }

    private boolean isAncientCityLoot(@NotNull LootGenerateEvent event) {
        NamespacedKey lootKey = event.getLootTable().getKey();

        return lootKey.equals(LootTables.ANCIENT_CITY.getKey()) || lootKey.equals(LootTables.ANCIENT_CITY_ICE_BOX.getKey());
    }

    @NotNull
    private ItemStack createBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantsUtils.add(book, this.getBukkitEnchantment(), EnchantsUtils.randomLevel(this.getBukkitEnchantment()), true);
        return book;
    }

    private static class DamageMemory {

        private String damageId;
        private int    streak;
    }
}
