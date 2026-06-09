package su.nightexpress.excellentenchants.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.ProjectileEnchant;
import su.nightexpress.excellentenchants.api.item.ItemSetDefaults;
import su.nightexpress.excellentenchants.enchantment.*;
import su.nightexpress.excellentenchants.enchantment.universal.UnyieldingEnchant;
import su.nightexpress.excellentenchants.manager.block.TickedBlock;
import su.nightexpress.excellentenchants.manager.damage.Explosion;
import su.nightexpress.excellentenchants.manager.listener.AnvilListener;
import su.nightexpress.excellentenchants.manager.listener.EnchantListener;
import su.nightexpress.excellentenchants.manager.listener.GenericListener;
import su.nightexpress.excellentenchants.manager.listener.SlotListener;
import su.nightexpress.excellentenchants.manager.menu.EnchantsMenu;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.bridge.RegistryType;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class EnchantManager extends AbstractManager<EnchantsPlugin> {

    private final Map<AbstractArrow, Set<UniParticle>> arrowEffects;
    private final Map<Location, TickedBlock>           tickedBlocks;
    private final Map<UUID, Explosion>                 explosions;
    private final Set<UUID>                            suppressedExplosionKnockback;
    private final Map<UUID, Long>                      suppressedEndermanTeleports;

    private final EnchantSettings settings;

    private final NamespacedKey entitySpawnKey;
    private final NamespacedKey blockEnchantKey;
    private final NamespacedKey ghastFireballKey;

    private EnchantsMenu enchantsMenu;

    public EnchantManager(@NotNull EnchantsPlugin plugin) {
        super(plugin);
        this.arrowEffects = new ConcurrentHashMap<>();
        this.tickedBlocks = new HashMap<>();
        this.explosions = new HashMap<>();
        this.suppressedExplosionKnockback = ConcurrentHashMap.newKeySet();
        this.suppressedEndermanTeleports = new ConcurrentHashMap<>();
        this.settings = new EnchantSettings();

        this.entitySpawnKey = new NamespacedKey(plugin, "entity.spawn_reason");
        this.blockEnchantKey = new NamespacedKey(plugin, "block.enchant");
        this.ghastFireballKey = new NamespacedKey(plugin, "ghast_fireball");
    }

    protected void onLoad() {
        this.settings.load(this.plugin.getConfig());
        this.loadEnchants();

        this.enchantsMenu = new EnchantsMenu(this.plugin);

        this.addListener(new GenericListener(this.plugin, this));
        this.addListener(new AnvilListener(this.plugin, this.settings));
        this.addListener(new EnchantListener(this.plugin, this));

        if (Version.isPaper()) {
            this.addListener(new SlotListener(this.plugin, this));
        }

        this.addAsyncTask(this::tickArrowEffects, this.settings.getArrowEffectsTickInterval());

        if (!EnchantRegistry.PASSIVE.isEmpty()) {
            this.addTask(this::tickPassiveEnchants, this.settings.getPassiveEnchantsTickInterval());
        }

        this.addTask(this::tickBlocks, 1L);
    }

    @Override
    protected void onShutdown() {
        this.saveUnyieldingMemory();
        this.restoreBlocks();

        if (this.enchantsMenu != null) this.enchantsMenu.clear();

        this.arrowEffects.clear();
        this.tickedBlocks.clear();
        this.explosions.clear();
        this.suppressedExplosionKnockback.clear();
        this.suppressedEndermanTeleports.clear();
    }

    private void saveUnyieldingMemory() {
        CustomEnchantment enchantment = EnchantRegistry.getById(UnyieldingEnchant.ID);
        if (enchantment instanceof UnyieldingEnchant unyielding) {
            unyielding.saveMemory();
        }
    }

    private void loadEnchants() {
        EnchantCatalog.enabled().forEach(this::loadEnchant);
        ItemSetDefaults.clearAll(); // Clear default item sets from memory.

        this.plugin.info("Loaded " + EnchantRegistry.getRegistered().size() + " enchantments.");
    }

    private boolean loadEnchant(@NotNull EnchantCatalog catalog) {
        String id = catalog.getId();
        CustomEnchantment registered = EnchantRegistry.getById(id);
        if (registered != null) {
            registered.load();
            return true;
        }

        Path file = Path.of(this.plugin.getDataFolder() + EnchantsFiles.DIR_ENCHANTS, FileConfig.withExtension(id));
        if (!Files.exists(file)) {
            this.plugin.error("No config file present for the '%s' enchantment.".formatted(id));
            return false;
        }

        Enchantment bukkitEnchant = BukkitThing.getByKey(RegistryType.ENCHANTMENT, catalog.getKey());
        if (bukkitEnchant == null) {
            this.plugin.error("No registered bukkit enchant found for '%s'.".formatted(id));
            return false;
        }

        EnchantContext context = new EnchantContext(id, bukkitEnchant, catalog.getDefinition(), catalog.getDistribution(), catalog.isCurse());
        CustomEnchantment enchantment = catalog.createEnchantment(this.plugin, this, file, context);

        enchantment.load();
        EnchantRegistry.registerEnchant(enchantment);
        return true;
    }

    public void updateCache(@NotNull LivingEntity entity, @NotNull EquipmentSlot slot, @Nullable ItemStack itemStack) {
        EnchantRegistry.getHolders().forEach(holder -> {
            if (!holder.isCacheable()) return;

            if (itemStack == null || itemStack.getType().isAir() || !EnchantsUtils.hasEnchantsAndNotABook(itemStack) || !EnchantsUtils.isValidSlotForEnchantEffects(itemStack, slot)) {
                holder.removeCache(entity, slot);
                return;
            }

            Map<CustomEnchantment, Integer> allEnchants = EnchantsUtils.getCustomEnchantments(itemStack);
            holder.updateCache(entity, slot, itemStack, allEnchants);
        });
    }

    public void clearCache(@NotNull LivingEntity entity) {
        EnchantRegistry.getHolders().forEach(holder -> {
            if (!holder.isCacheable()) return;

            holder.clearCache(entity);
        });
    }

    public void reCache(@NotNull LivingEntity entity) {
        this.clearCache(entity);

        EntityUtil.getEquippedItems(entity).forEach((slot, itemStack) -> {
            this.updateCache(entity, slot, itemStack);
        });
    }

    @NotNull
    public EnchantSettings getSettings() {
        return this.settings;
    }

    public void openEnchantsMenu(@NotNull Player player) {
        this.enchantsMenu.open(player);
    }

    public void addArrowEffect(@NotNull AbstractArrow arrow, @NotNull UniParticle particle) {
        this.arrowEffects.computeIfAbsent(arrow, k -> new HashSet<>()).add(particle);
    }

    public void removeArrowEffects(@NotNull AbstractArrow arrow) {
        this.arrowEffects.remove(arrow);
    }

    private void tickArrowEffects() {
        this.arrowEffects.keySet().removeIf(arrow -> !arrow.isValid() || arrow.isDead());
        this.arrowEffects.forEach((arrow, effects) -> {
            effects.forEach(particle -> particle.play(arrow.getLocation(), 0f, 0f, 10));
        });
    }

    private void tickBlocks() {
        this.tickedBlocks.values().removeIf(tickedBlock -> {
            tickedBlock.tick();
            return tickedBlock.isDead();
        });
    }

    private void restoreBlocks() {
        this.tickedBlocks.values().forEach(TickedBlock::restore);
    }

    private void tickPassiveEnchants() {
        this.getPassiveEnchantEntities().forEach(entity -> {
            this.handleInSlots(entity, EntityUtil.EQUIPMENT_SLOTS, EnchantRegistry.PASSIVE, (item, enchant, level) -> enchant.onTrigger(entity, item, level));
        });
    }

    @NotNull
    private Set<LivingEntity> getPassiveEnchantEntities() {
        Set<LivingEntity> entities = new HashSet<>(Players.getOnline());

        if (this.settings.isPassiveEnchantsAllowedForMobs()) {
            this.plugin.getServer().getWorlds().forEach(world -> {
                entities.addAll(world.getLivingEntities());
            });
        }

        entities.removeIf(Entity::isDead);

        return entities;
    }

    public void addTickedBlock(@NotNull Block block, @NotNull Material origin, @NotNull Material transform, int lifeTime) {
        Location location = block.getLocation();
        TickedBlock tickedBlock = new TickedBlock(location, origin, lifeTime);
        this.tickedBlocks.put(location, tickedBlock);

        block.setType(transform);
    }

    public boolean removeTickedBlock(@NotNull Block block) {
        return this.removeTickedBlock(block.getLocation());
    }

    public boolean removeTickedBlock(@NotNull Location location) {
        TickedBlock tickedBlock = this.tickedBlocks.remove(location);
        if (tickedBlock == null) return false;

        tickedBlock.restore();
        return true;
    }

    public void setBlockEnchant(@NotNull ItemStack itemStack, @NotNull BlockEnchant enchant) {
        PDCUtil.set(itemStack, this.blockEnchantKey, enchant.getId());
    }

    @Nullable
    public BlockEnchant getBlockEnchant(@NotNull ItemStack itemStack) {
        String enchantId = PDCUtil.getString(itemStack, this.blockEnchantKey).orElse(null);
        if (enchantId == null) return null;

        return EnchantRegistry.BLOCK.getEnchant(enchantId);
    }

    public void setSpawnReason(@NotNull Entity entity, @NotNull CreatureSpawnEvent.SpawnReason reason) {
        PDCUtil.set(entity, this.entitySpawnKey, reason.name());
    }

    @Nullable
    public CreatureSpawnEvent.SpawnReason getSpawnReason(@NotNull Entity entity) {
        String name = PDCUtil.getString(entity, this.entitySpawnKey).orElse(null);
        return name == null ? null : Enums.get(name, CreatureSpawnEvent.SpawnReason.class);
    }

    public void markGhastFireball(@NotNull Fireball fireball) {
        fireball.getPersistentDataContainer().set(this.ghastFireballKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean isGhastFireball(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(this.ghastFireballKey, PersistentDataType.BYTE);
    }

    public void suppressEndermanTeleport(@NotNull Enderman enderman, long durationTicks) {
        if (durationTicks <= 0L) return;

        long expiresAt = System.currentTimeMillis() + (durationTicks * 50L);
        this.suppressedEndermanTeleports.merge(enderman.getUniqueId(), expiresAt, Math::max);
    }

    public boolean isEndermanTeleportSuppressed(@NotNull Enderman enderman) {
        UUID entityId = enderman.getUniqueId();
        Long expiresAt = this.suppressedEndermanTeleports.get(entityId);
        if (expiresAt == null) return false;

        if (expiresAt <= System.currentTimeMillis()) {
            this.suppressedEndermanTeleports.remove(entityId);
            return false;
        }
        return true;
    }

    public boolean createExplosion(@NotNull LivingEntity entity, @NotNull Location location, float power, boolean fire, boolean destroy, @NotNull Consumer<Explosion> consumer) {
        Explosion explosion = new Explosion(entity);
        consumer.accept(explosion);

        this.explosions.put(entity.getUniqueId(), explosion);
        if (!explosion.hasKnockback()) {
            this.suppressExplosionKnockback(location, power);
        }

        return entity.getWorld().createExplosion(location, power, fire, destroy, entity);
    }

    public void handleEnchantExplosion(@NotNull EntityExplodeEvent event, @NotNull LivingEntity entity) {
        Explosion explosion = this.explosions.get(entity.getUniqueId());
        if (explosion == null) return;

        explosion.handleExplosion(event);

        this.plugin.runTask(() -> this.explosions.remove(entity.getUniqueId()));
    }

    public void handleEnchantExplosionDamage(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity entity) {
        Explosion explosion = this.explosions.get(entity.getUniqueId());
        if (explosion == null) return;

        if (!explosion.hasKnockback()) {
            this.suppressExplosionKnockback(event.getEntity());
        }
        explosion.handleDamage(event);
    }

    private void suppressExplosionKnockback(@NotNull Location location, float power) {
        double radius = Math.max(0D, power * 2D);
        Set<UUID> marked = new HashSet<>();

        location.getWorld().getNearbyEntities(location, radius, radius, radius, entity -> entity instanceof LivingEntity).forEach(entity -> {
            UUID entityId = entity.getUniqueId();
            marked.add(entityId);
            this.suppressedExplosionKnockback.add(entityId);
        });

        this.plugin.runTask(() -> this.suppressedExplosionKnockback.removeAll(marked));
    }

    private void suppressExplosionKnockback(@NotNull Entity entity) {
        UUID entityId = entity.getUniqueId();
        this.suppressedExplosionKnockback.add(entityId);
        this.plugin.runTask(() -> this.suppressedExplosionKnockback.remove(entityId));
    }

    public boolean isExplosionKnockbackSuppressed(@NotNull Entity entity) {
        return this.suppressedExplosionKnockback.contains(entity.getUniqueId());
    }

    public <T extends CustomEnchantment> void handleInventoryEnchants(@NotNull Player player, @NotNull EnchantHolder<T> holder, @NotNull EnchantUsage<T> usage) {
        this.handleFully(player, EnchantsUtils.getAll(player, holder), holder::getPriority, usage);
    }

    public <T extends CustomEnchantment> void handleItemEnchants(@NotNull LivingEntity entity,
                                                                 @NotNull ItemStack itemStack,
                                                                 @NotNull EnchantHolder<T> holder,
                                                                 @NotNull EnchantUsage<T> usage) {
        Map<ItemStack, Map<T, Integer>> enchants = new HashMap<>();
        enchants.put(itemStack, EnchantsUtils.getCustomEnchantments(itemStack, holder));

        this.handleFully(entity, enchants, holder::getPriority, usage);
    }

    public <P extends AbstractArrow, T extends ProjectileEnchant<P>> void handleArrowEnchants(@NotNull P projectile,
                                                                                              @NotNull EnchantHolder<T> holder,
                                                                                              @NotNull EnchantUsage<T> usage) {
        ItemStack bow = projectile.getWeapon();
        if (bow == null || !EnchantsUtils.hasEnchantsAndNotABook(bow)) return;

        Map<ItemStack, Map<T, Integer>> enchants = new HashMap<>();
        enchants.put(bow, EnchantsUtils.getArrowEnchants(projectile, holder));

        this.handleDirect(enchants, holder::getPriority, usage);
    }

    public <T extends CustomEnchantment> void handleInSlot(@NotNull LivingEntity entity,
                                                           @NotNull EquipmentSlot slot,
                                                           @NotNull EnchantHolder<T> holder,
                                                           @NotNull EnchantUsage<T> usage) {
        this.handleInSlots(entity, new EquipmentSlot[]{slot}, holder, usage);
    }

    public <T extends CustomEnchantment> void handleInSlots(@NotNull LivingEntity entity,
                                                            @NotNull EquipmentSlot[] slots,
                                                            @NotNull EnchantHolder<T> holder,
                                                            @NotNull EnchantUsage<T> usage) {

        Map<ItemStack, Map<T, Integer>> enchantMap = new HashMap<>();
        boolean noCache = entity.getType() != EntityType.PLAYER || !holder.isCacheable() || Version.isSpigot();

        for (EquipmentSlot slot : slots) {
            if (noCache || slot == EquipmentSlot.HAND) { // Main hand is not cached
                ItemStack itemStack = EntityUtil.getItemInSlot(entity, slot);
                if (itemStack == null || itemStack.getType().isAir()) continue; // Ignore empty slots.
                if (!EnchantsUtils.hasEnchantsAndNotABook(itemStack)) continue; // Ignore books and items without enchants.
                if (!EnchantsUtils.isValidSlotForEnchantEffects(itemStack, slot)) continue; // Ignore armor items when holding in hands.

                enchantMap.put(itemStack, EnchantsUtils.getCustomEnchantments(itemStack, holder));
            }
            else {
                EnchantedItem<T> enchantedItem = holder.getCached(entity, slot);
                if (enchantedItem == null) continue;

                enchantMap.put(enchantedItem.getItemStack(), enchantedItem.getEnchants());
            }
        }

        this.handleFully(entity, enchantMap, holder::getPriority, usage);
    }

    public <T extends CustomEnchantment> void handleFully(@NotNull LivingEntity entity,
                                                          @NotNull Map<ItemStack, Map<T, Integer>> enchantMap,
                                                          @NotNull Function<T, EnchantPriority> priority,
                                                          @NotNull EnchantUsage<T> usage) {

        this.handleDirect(enchantMap, priority, (itemStack, enchant, level) -> {
            if (this.settings.isEnchantDisabledInWorld(entity.getWorld(), enchant)) return false;
            if (enchant.isOutOfCharges(itemStack)) return false;
            if (enchant.hasComponent(EnchantComponent.PERIODIC) && !enchant.isTriggerTime(entity)) return false;
            if (enchant.hasComponent(EnchantComponent.PROBABILITY) && !enchant.testTriggerChance(level)) return false;
            if (!usage.useEnchant(itemStack, enchant, level)) return false;

            enchant.consumeCharges(itemStack, level); // TODO Re-add equipment for mobs to apply changes
            return true;
        });
    }

    public <T extends CustomEnchantment> void handleDirect(@NotNull Map<ItemStack, Map<T, Integer>> enchantMap,
                                                           @NotNull Function<T, EnchantPriority> priority,
                                                           @NotNull EnchantUsage<T> usage) {
        enchantMap.forEach((itemStack, enchants) -> {
            enchants.entrySet().stream().sorted(Comparator.comparingInt(entry -> priority.apply(entry.getKey()).ordinal())).forEach(entry -> {
                T enchant = entry.getKey();
                int level = entry.getValue();

                usage.useEnchant(itemStack, enchant, level);
            });
        });
    }
}

