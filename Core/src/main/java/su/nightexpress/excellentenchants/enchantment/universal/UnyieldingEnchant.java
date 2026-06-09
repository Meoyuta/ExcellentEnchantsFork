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
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.type.ResurrectEnchant;
import su.nightexpress.excellentenchants.config.Lang;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UnyieldingEnchant extends GameEnchantment implements ResurrectEnchant {

    public static final String ID = "unyielding";

    private static final String DATA_FILE_NAME              = "unyielding.yml";
    private static final String DATA_PLAYERS                = "Players";
    private static final String DATA_ACTIVE                 = "Active";
    private static final String DATA_PROGRESS               = "Progress";
    private static final String DATA_DAMAGE_ID              = "Damage_Id";
    private static final String DATA_STREAK                 = "Streak";
    private static final String DATA_IMMUNITY_NOTIFICATIONS = "Immunity_Notifications";
    private static final String VANILLA_NAMESPACE           = "minecraft";
    private static final String CHEST_LOOT_PREFIX           = "chests/";
    private static final String ARCHAEOLOGY_LOOT_PREFIX     = "archaeology/";

    private final Map<UUID, DamageMemory> damageMemoryMap;
    private final Path                    dataFile;

    private int    minimumStreak;
    private double reductionBase;
    private double immunityThreshold;
    private double ancientCityLootChance;
    private int    ancientCityMinBooks;
    private int    ancientCityMaxBooks;
    private double otherRuinsLootChance;
    private int    otherRuinsMinBooks;
    private int    otherRuinsMaxBooks;

    public UnyieldingEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.damageMemoryMap = new HashMap<>();
        this.dataFile = Path.of(plugin.getDataFolder().getAbsolutePath(), EnchantsFiles.DIR_DATA, DATA_FILE_NAME);

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

        this.ancientCityLootChance = readChance(config, "Unyielding.Ancient_City_Loot.Chance",
            0.15D,
            "Chance for an unyielding enchanted book to be added to Ancient City loot.",
            "Use 0.15 for 15% chance."
        );

        this.ancientCityMinBooks = Math.max(0, ConfigValue.create("Unyielding.Ancient_City_Loot.Min_Books",
            1,
            "Minimum unyielding enchanted books added to Ancient City loot."
        ).read(config));

        this.ancientCityMaxBooks = Math.max(this.ancientCityMinBooks, ConfigValue.create("Unyielding.Ancient_City_Loot.Max_Books",
            2,
            "Maximum unyielding enchanted books added to Ancient City loot."
        ).read(config));

        this.otherRuinsLootChance = readChance(config, "Unyielding.Other_Ruins_Loot.Chance",
            0.01D,
            "Chance for an unyielding enchanted book to be added to other vanilla structure loot.",
            "Use 0.01 for 1% chance."
        );

        this.otherRuinsMinBooks = Math.max(0, ConfigValue.create("Unyielding.Other_Ruins_Loot.Min_Books",
            1,
            "Minimum unyielding enchanted books added to other vanilla structure loot."
        ).read(config));

        this.otherRuinsMaxBooks = Math.max(this.otherRuinsMinBooks, ConfigValue.create("Unyielding.Other_Ruins_Loot.Max_Books",
            1,
            "Maximum unyielding enchanted books added to other vanilla structure loot."
        ).read(config));

        this.loadMemory();
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
        this.saveMemory();
        return true;
    }

    public void handleDamage(@NotNull EntityDamageEvent event, @NotNull Player player) {
        DamageMemory memory = this.damageMemoryMap.get(player.getUniqueId());
        if (memory == null) return;
        if (event.getDamage() <= 0D) return;

        NamespacedKey damageKey = event.getDamageSource().getDamageType().getKey();
        String damageId = damageKey.asString();
        DamageProgress progress = memory.progressMap.computeIfAbsent(damageId, id -> new DamageProgress());

        int previousStreak = progress.streak;

        if (previousStreak >= this.minimumStreak) {
            double reducedDamage = event.getDamage() * Math.pow(this.reductionBase, previousStreak);
            boolean immune = reducedDamage < this.immunityThreshold;
            if (immune) {
                event.setCancelled(true);
            }
            else {
                event.setDamage(Math.max(0D, reducedDamage));
            }

            if (immune && memory.immunityNotifications.add(damageId)) {
                String enchantName = this.getDisplayName();
                Lang.UNYIELDING_IMMUNITY_REACHED.message().send(player, replacer -> replacer
                    .replace("百折不挠", enchantName)
                    .replace("Unyielding", enchantName)
                    .replace(EnchantsPlaceholders.GENERIC_ENCHANT, enchantName)
                    .replace(EnchantsPlaceholders.GENERIC_TYPE, formatDamageId(damageId))
                    .replace(EnchantsPlaceholders.GENERIC_NAME, damageId)
                );
            }
        }

        progress.streak++;
        this.saveMemory();
    }

    public void clear(@NotNull Player player) {
        if (this.damageMemoryMap.remove(player.getUniqueId()) != null) {
            this.saveMemory();
        }
    }

    public void setActive(@NotNull Player player, boolean active) {
        if (active) {
            this.damageMemoryMap.computeIfAbsent(player.getUniqueId(), id -> new DamageMemory());
        }
        else {
            this.damageMemoryMap.remove(player.getUniqueId());
        }
        this.saveMemory();
    }

    public void saveMemory() {
        FileConfig config = this.loadDataConfig();
        config.remove(DATA_PLAYERS);

        this.damageMemoryMap.entrySet().stream()
            .sorted((first, second) -> first.getKey().compareTo(second.getKey()))
            .forEach(entry -> this.writeMemory(config, entry.getKey(), entry.getValue()));

        config.save();
    }

    private void loadMemory() {
        FileConfig config = this.loadDataConfig();
        this.damageMemoryMap.clear();

        for (String uuidRaw : config.getSection(DATA_PLAYERS)) {
            UUID uuid = parseUUID(uuidRaw);
            if (uuid == null) continue;

            String playerPath = DATA_PLAYERS + "." + uuidRaw;
            DamageMemory memory = new DamageMemory();

            memory.immunityNotifications.addAll(config.getStringList(playerPath + "." + DATA_IMMUNITY_NOTIFICATIONS));

            for (String progressKey : config.getSection(playerPath + "." + DATA_PROGRESS)) {
                String progressPath = playerPath + "." + DATA_PROGRESS + "." + progressKey;
                String damageId = config.getString(progressPath + "." + DATA_DAMAGE_ID, decodeDamageId(progressKey));
                int streak = config.getInt(progressPath + "." + DATA_STREAK, 0);
                if (damageId == null || damageId.isBlank() || streak <= 0) continue;

                DamageProgress progress = new DamageProgress();
                progress.streak = streak;
                memory.progressMap.put(damageId, progress);
            }

            if (config.getBoolean(playerPath + "." + DATA_ACTIVE, false) || !memory.progressMap.isEmpty()) {
                this.damageMemoryMap.put(uuid, memory);
            }
        }
    }

    private void writeMemory(@NotNull FileConfig config, @NotNull UUID playerId, @NotNull DamageMemory memory) {
        String playerPath = DATA_PLAYERS + "." + playerId;
        config.set(playerPath + "." + DATA_ACTIVE, true);
        config.set(playerPath + "." + DATA_IMMUNITY_NOTIFICATIONS, memory.immunityNotifications.stream().sorted().toList());

        memory.progressMap.entrySet().stream()
            .sorted((first, second) -> first.getKey().compareTo(second.getKey()))
            .forEach(entry -> {
                DamageProgress progress = entry.getValue();
                if (progress.streak <= 0) return;

                String progressPath = playerPath + "." + DATA_PROGRESS + "." + encodeDamageId(entry.getKey());
                config.set(progressPath + "." + DATA_DAMAGE_ID, entry.getKey());
                config.set(progressPath + "." + DATA_STREAK, progress.streak);
            });
    }

    @NotNull
    private FileConfig loadDataConfig() {
        this.createDataDirectory();

        return FileConfig.load(this.dataFile);
    }

    private void createDataDirectory() {
        Path parent = this.dataFile.getParent();
        if (parent == null) return;

        try {
            Files.createDirectories(parent);
        }
        catch (IOException exception) {
            this.plugin.error("Could not create unyielding data directory: " + exception.getMessage());
        }
    }

    private static UUID parseUUID(@NotNull String raw) {
        try {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @NotNull
    private static String encodeDamageId(@NotNull String damageId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(damageId.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    private static String decodeDamageId(@NotNull String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException exception) {
            return encoded;
        }
    }

    @NotNull
    public Optional<List<DamageStatus>> getDamageStatuses(@NotNull Player player) {
        DamageMemory memory = this.damageMemoryMap.get(player.getUniqueId());
        if (memory == null) return Optional.empty();

        return Optional.of(memory.progressMap.entrySet().stream()
            .sorted((first, second) -> first.getKey().compareTo(second.getKey()))
            .map(entry -> {
                DamageProgress progress = entry.getValue();
                return new DamageStatus(
                    entry.getKey(),
                    progress.streak,
                    this.minimumStreak,
                    this.getNextMultiplier(progress.streak),
                    this.reductionBase,
                    this.immunityThreshold
                );
            })
            .toList());
    }

    private double getNextMultiplier(int streak) {
        return streak >= this.minimumStreak ? Math.pow(this.reductionBase, streak) : 1D;
    }

    @NotNull
    public static String formatDamageId(@NotNull String damageId) {
        int index = damageId.indexOf(':');
        String cleanId = index >= 0 ? damageId.substring(index + 1) : damageId;

        return cleanId.replace('_', ' ');
    }

    public void populateRuinLoot(@NotNull LootGenerateEvent event) {
        LootType lootType = this.getLootType(event);
        if (lootType == null) return;

        double chance = lootType == LootType.ANCIENT_CITY ? this.ancientCityLootChance : this.otherRuinsLootChance;
        int minBooks = lootType == LootType.ANCIENT_CITY ? this.ancientCityMinBooks : this.otherRuinsMinBooks;
        int maxBooks = lootType == LootType.ANCIENT_CITY ? this.ancientCityMaxBooks : this.otherRuinsMaxBooks;
        if (maxBooks <= 0) return;
        if (chance <= 0D) return;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        int amount = ThreadLocalRandom.current().nextInt(minBooks, maxBooks + 1);
        for (int index = 0; index < amount; index++) {
            event.getLoot().add(this.createBook());
        }
    }

    private LootType getLootType(@NotNull LootGenerateEvent event) {
        NamespacedKey lootKey = event.getLootTable().getKey();
        if (this.isAncientCityLoot(lootKey)) return LootType.ANCIENT_CITY;
        if (this.isOtherRuinLoot(lootKey)) return LootType.OTHER_RUINS;

        return null;
    }

    private boolean isAncientCityLoot(@NotNull NamespacedKey lootKey) {
        return lootKey.equals(LootTables.ANCIENT_CITY.getKey()) || lootKey.equals(LootTables.ANCIENT_CITY_ICE_BOX.getKey());
    }

    private boolean isOtherRuinLoot(@NotNull NamespacedKey lootKey) {
        if (!lootKey.getNamespace().equals(VANILLA_NAMESPACE)) return false;

        String key = lootKey.getKey();
        return key.startsWith(CHEST_LOOT_PREFIX) || key.startsWith(ARCHAEOLOGY_LOOT_PREFIX);
    }

    @NotNull
    private ItemStack createBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantsUtils.add(book, this.getBukkitEnchantment(), EnchantsUtils.randomLevel(this.getBukkitEnchantment()), true);
        return book;
    }

    private static double readChance(@NotNull FileConfig config, @NotNull String path, double defaultValue, @NotNull String... comments) {
        return Math.clamp(ConfigValue.create(path, defaultValue, comments).read(config), 0D, 1D);
    }

    private enum LootType {
        ANCIENT_CITY,
        OTHER_RUINS
    }

    private static class DamageMemory {

        private final Map<String, DamageProgress> progressMap = new HashMap<>();
        private final Set<String> immunityNotifications = new HashSet<>();
    }

    private static class DamageProgress {

        private int streak;
    }

    public record DamageStatus(@NotNull String damageId,
                               int streak,
                               int minimumStreak,
                               double nextMultiplier,
                               double reductionBase,
                               double immunityThreshold) {

        public boolean isReducing() {
            return this.streak >= this.minimumStreak;
        }

        public double getReductionPercent() {
            return (1D - this.nextMultiplier) * 100D;
        }
    }
}
