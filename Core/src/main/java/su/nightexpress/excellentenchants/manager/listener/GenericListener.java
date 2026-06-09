package su.nightexpress.excellentenchants.manager.listener;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;
import su.nightexpress.excellentenchants.enchantment.universal.UnyieldingEnchant;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.manager.AbstractListener;

public class GenericListener extends AbstractListener<EnchantsPlugin> {

    private final EnchantManager manager;

    public GenericListener(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager) {
        super(plugin);
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        this.manager.setSpawnReason(event.getEntity(), event.getSpawnReason());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChargesFillOnEnchant(EnchantItemEvent event) {
        if (!Config.isChargesEnabled()) return;

        this.plugin.runTask(() -> {
            Inventory inventory = event.getInventory();

            ItemStack result = inventory.getItem(0);
            if (result == null) return;

            event.getEnchantsToAdd().forEach((enchantment, level) -> {
                EnchantsUtils.restoreCharges(result, enchantment, level);
            });

            inventory.setItem(0, result);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUnyieldingLoot(LootGenerateEvent event) {
        if (event.isPlugin()) return;

        CustomEnchantment enchantment = EnchantRegistry.getById(UnyieldingEnchant.ID);
        if (!(enchantment instanceof UnyieldingEnchant unyielding)) return;
        if (this.manager.getSettings().isEnchantDisabledInWorld(event.getWorld(), unyielding)) return;

        unyielding.populateRuinLoot(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTickedBlockBreak(BlockBreakEvent event) {
        if (this.manager.removeTickedBlock(event.getBlock())) {
            event.setDropItems(false);
            event.setExpToDrop(0);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTickedBlockTNTExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this.manager::removeTickedBlock);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTickedBlockEntityExplode(EntityExplodeEvent event) {
        if (this.manager.isGhastFireball(event.getEntity())) {
            event.blockList().clear();
            event.setYield(0F);
            return;
        }

        event.blockList().forEach(this.manager::removeTickedBlock);

        if (event.getEntity() instanceof LivingEntity entity) {
            this.manager.handleEnchantExplosion(event, entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGhastFireballPrime(ExplosionPrimeEvent event) {
        if (!this.manager.isGhastFireball(event.getEntity())) return;

        event.setFire(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGhastFireballIgnite(BlockIgniteEvent event) {
        Entity entity = event.getIgnitingEntity();
        if (entity == null || !this.manager.isGhastFireball(entity)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGhastFireballDamage(EntityDamageByEntityEvent event) {
        if (!this.isGhastFireballDamage(event)) return;

        event.setDamage(0D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSuppressedEndermanTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Enderman enderman)) return;
        if (!this.manager.isEndermanTeleportSuppressed(enderman)) return;

        event.setCancelled(true);
    }

    private boolean isGhastFireballDamage(@NotNull EntityDamageByEntityEvent event) {
        if (this.manager.isGhastFireball(event.getDamager())) return true;

        Entity directEntity = event.getDamageSource().getDirectEntity();
        return directEntity != null && this.manager.isGhastFireball(directEntity);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        DamageSource source = event.getDamageSource();
        DamageType type = source.getDamageType();

        if (type != DamageType.PLAYER_EXPLOSION && type != DamageType.EXPLOSION) return;
        if (!(source.getCausingEntity() instanceof LivingEntity entity)) return;

        this.manager.handleEnchantExplosionDamage(event, entity);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionKnockback(EntityKnockbackEvent event) {
        if (event.getCause() != EntityKnockbackEvent.KnockbackCause.EXPLOSION) return;
        if (!this.manager.isExplosionKnockbackSuppressed(event.getEntity())) return;

        event.setCancelled(true);
    }
}
