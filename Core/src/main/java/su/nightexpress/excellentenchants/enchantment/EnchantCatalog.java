package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.EnchantsKeys;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantDefinition;
import su.nightexpress.excellentenchants.api.EnchantDistribution;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.bridge.EnchantCatalogEntry;
import su.nightexpress.excellentenchants.enchantment.armor.*;
import su.nightexpress.excellentenchants.enchantment.bow.*;
import su.nightexpress.excellentenchants.enchantment.fishing.*;
import su.nightexpress.excellentenchants.enchantment.tool.*;
import su.nightexpress.excellentenchants.enchantment.universal.*;
import su.nightexpress.excellentenchants.enchantment.weapon.*;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Version;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum EnchantCatalog implements EnchantCatalogEntry {

    COLD_STEEL(ColdSteelEnchant::new),
    DARKNESS_CLOAK(DarknessCloakEnchant::new),
    DRAGON_HEART(DragonHeartEnchant::new),
    DESTORY(DestoryEnchant::new),
    ELEMENTAL_PROTECTION(ElementalProtectionEnchant::new),
    FIRE_SHIELD(FireShieldEnchant::new),
    FLAME_WALKER(FlameWalkerEnchant::new),
    HARDENED(HardenedEnchant::new),
    ICE_SHIELD(IceShieldEnchant::new),
    LIGHTWEIGHT(LightweightEnchant::new),
    JUMPING(JumpingEnchant::new),
    KAMIKADZE(KamikadzeEnchant::new),
    NIGHT_VISION(NightVisionEnchant::new),
    REBOUND(ReboundEnchant::new),
    REGROWTH(RegrowthEnchant::new),
    SATURATION(SaturationEnchant::new),
    SPEED(SpeedyEnchant::new),
    STOPPING_FORCE(StoppingForceEnchant::new),
    WATER_BREATHING(WaterBreathingEnchant::new),
    BOMBER(BomberEnchant::new),
    ENDER_BOW(EnderBowEnchant::new),
    GHAST(GhastEnchant::new),
    CONFUSING_ARROWS(ConfusingArrowsEnchant::new),
    DARKNESS_ARROWS(DarknessArrowsEnchant::new),
    DRAGONFIRE_ARROWS(DragonfireArrowsEnchant::new),
    ELECTRIFIED_ARROWS(ElectrifiedArrowsEnchant::new),
    EXPLOSIVE_ARROWS(ExplosiveArrowsEnchant::new),
    FLARE(FlareEnchant::new),
    HOVER(HoverEnchant::new),
    LINGERING(LingeringEnchant::new),
    POISONED_ARROWS(PoisonedArrowsEnchant::new),
    SNIPER(SniperEnchant::new),
    VAMPIRIC_ARROWS(VampiricArrowsEnchant::new),
    WITHERED_ARROWS(WitheredArrowsEnchant::new),
    AUTO_REEL(AutoReelEnchant::new, false, true),
    CURSE_OF_DROWNED(CurseOfDrownedEnchant::new, true),
    DOUBLE_CATCH(DoubleCatchEnchant::new),
    RIVER_MASTER(RiverMasterEnchant::new),
    SEASONED_ANGLER(SeasonedAnglerEnchant::new),
    SURVIVALIST(SurvivalistEnchant::new),
    BLAST_MINING(BlastMiningEnchant::new),
    GLASSBREAKER(GlassbreakerEnchant::new),
    HASTE(HasteEnchant::new),
    LUCKY_MINER(LuckyMinerEnchant::new),
    REPLANTER(ReplanterEnchant::new),
    SILK_CHEST(SilkChestEnchant::new, false, true),
    SILK_SPAWNER(SilkSpawnerEnchant::new),
    SMELTER(SmelterEnchant::new),
    TELEKINESIS(TelekinesisEnchant::new),
    TREEFELLER(TreefellerEnchant::new),
    TUNNEL(TunnelEnchant::new),
    VEINMINER(VeinminerEnchant::new),
    CURSE_OF_BREAKING(CurseOfBreakingEnchant::new, true),
    CURSE_OF_FRAGILITY(CurseOfFragilityEnchant::new, true),
    CURSE_OF_MEDIOCRITY(CurseOfMediocrityEnchant::new, true),
    CURSE_OF_MISFORTUNE(CurseOfMisfortuneEnchant::new, true),
    RESTORE(RestoreEnchant::new),
    SOULBOUND(SoulboundEnchant::new),
    BANE_OF_NETHERSPAWN(BaneOfNetherspawnEnchant::new),
    BLINDNESS(BlindnessEnchant::new),
    CONFUSION(ConfusionEnchant::new),
    CURE(CureEnchant::new),
    CURSE_OF_DEATH(CurseOfDeathEnchant::new, true),
    CUTTER(CutterEnchant::new),
    DECAPITATOR(DecapitatorEnchant::new),
    DOUBLE_STRIKE(DoubleStrikeEnchant::new),
    EXHAUST(ExhaustEnchant::new),
    ICE_ASPECT(IceAspectEnchant::new),
    INFERNUS(InfernusEnchant::new),
    NIMBLE(NimbleEnchant::new),
    PARALYZE(ParalyzeEnchant::new),
    RAGE(RageEnchant::new),
    ROCKET(RocketEnchant::new),
    ROUGHNESS(RoughnessEnchant::new),
    SWIPER(SwiperEnchant::new),
    TEMPER(TemperEnchant::new),
    THRIFTY(ThriftyEnchant::new),
    THUNDER(ThunderEnchant::new),
    VAMPIRE(VampireEnchant::new),
    VENOM(VenomEnchant::new),
    VILLAGE_DEFENDER(VillageDefenderEnchant::new),
    WISDOM(WisdomEnchant::new),
    WITHER(WitherEnchant::new)
    ;

    private static final String DEFAULT_ENCHANTS_PATH = "defaults/enchants/";

    public static void loadAll(@NotNull Path dataDir, @NotNull ItemSetRegistry itemSetRegistry, @NotNull BiConsumer<EnchantCatalog, IllegalStateException> onError) {
        Path enchantsDir = Path.of(dataDir.toString(), EnchantsFiles.DIR_ENCHANTS);
        Path disabledDir = Path.of(enchantsDir.toString(), EnchantsFiles.DIR_DISABLED);

        if (!Files.exists(disabledDir)) {
            try {
                Files.createDirectories(disabledDir);
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        Set<String> disabledEnchants = FileUtil.findYamlFiles(disabledDir.toString()).stream().map(FileUtil::getNameWithoutExtension).collect(Collectors.toSet());

        for (EnchantCatalog value : values()) {
            if (disabledEnchants.contains(value.getId())) {
                value.disabled = true;
            }
            else {
                try {
                    value.load(enchantsDir, itemSetRegistry);
                }
                catch (IllegalStateException exception) {
                    value.disabled = true;
                    onError.accept(value, exception);
                }
            }
        }
    }

    @NotNull
    public static Stream<EnchantCatalog> stream() {
        return Stream.of(values());
    }

    @NotNull
    public static Stream<EnchantCatalog> enabled() {
        return stream().filter(EnchantCatalog::isEnabled);
    }

    /*public static boolean isPresentAndEnabled(@NotNull String enchantId) {
        return Enums.parse(enchantId, EnchantCatalog.class).filter(EnchantCatalog::isEnabled).isPresent();
    }*/

    private final String  id;
    private final boolean curse;
    private final boolean paperOnly;

    private final EnchantFactory<?> factory;

    private EnchantDefinition   definition;
    private EnchantDistribution distribution;
    private boolean             disabled;

    EnchantCatalog(@NotNull EnchantFactory<?> factory) {
        this(factory, false);
    }

    EnchantCatalog(@NotNull EnchantFactory<?> factory, boolean curse) {
        this(factory, curse, false);
    }

    EnchantCatalog(@NotNull EnchantFactory<?> factory, boolean curse, boolean paperOnly) {
        this.id = LowerCase.INTERNAL.apply(this.name());
        this.curse = curse;
        this.paperOnly = paperOnly;
        this.factory = factory;
    }

    public void load(@NotNull Path enchantsDir, @NotNull ItemSetRegistry itemSetRegistry) throws IllegalStateException {
        if (this.paperOnly && Version.isSpigot()) throw new IllegalStateException("The enchantment is available for PaperMC only");

        Path file = Path.of(enchantsDir.toString(), FileConfig.withExtension(this.id));
        boolean exists = Files.exists(file);

        if (!exists) {
            exists = this.extractDefaultConfig(file);
        }
        if (!exists) {
            throw new IllegalStateException("No default config present for the '%s' enchantment.".formatted(this.id));
        }

        FileConfig config = FileConfig.load(file);

        this.definition = EnchantDefinition.read(config, "Definition", itemSetRegistry);
        this.distribution = EnchantDistribution.read(config, "Distribution");

        config.saveChanges();
    }

    private boolean extractDefaultConfig(@NotNull Path file) {
        String resourcePath = DEFAULT_ENCHANTS_PATH + FileConfig.withExtension(this.id);

        try (InputStream inputStream = EnchantCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) return false;

            Files.createDirectories(file.getParent());
            Files.copy(inputStream, file);
            return true;
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not extract default enchantment config: " + exception.getMessage(), exception);
        }
    }

    @NotNull
    public CustomEnchantment createEnchantment(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        return this.factory.create(plugin, manager, file, context);
    }

    @Override
    @NotNull
    public String getId() {
        return this.id;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return EnchantsKeys.create(this.id);
    }

    @Override
    @NotNull
    public EnchantDefinition getDefinition() {
        if (this.definition == null) throw new IllegalStateException("Definition is not yet initialized");

        return this.definition;
    }

    @Override
    @NotNull
    public EnchantDistribution getDistribution() {
        if (this.distribution == null) throw new IllegalStateException("Distribution is not yet initialized");

        return this.distribution;
    }

    @Override
    public boolean isCurse() {
        return this.curse;
    }

    public boolean isPaperOnly() {
        return this.paperOnly;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public boolean isEnabled() {
        return !this.disabled;
    }
}
