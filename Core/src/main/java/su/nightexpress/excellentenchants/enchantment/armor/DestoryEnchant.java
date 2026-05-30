package su.nightexpress.excellentenchants.enchantment.armor;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.type.AttackEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;

public class DestoryEnchant extends GameEnchantment implements AttackEnchant {

    private Modifier damageBonus;

    public DestoryEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.damageBonus = Modifier.load(config, "Destory.Damage_Bonus",
            Modifier.addictive(0).perLevel(7.5).capacity(37.5),
            "Base extra damage (in %) before the health ratio multiplier.",
            "Final bonus is calculated as: damage bonus * (max health / current health)."
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT, level -> NumberUtil.format(this.getDamageBonus(level)));
    }

    public double getDamageBonus(int level) {
        return this.damageBonus.getValue(level);
    }

    @NotNull
    @Override
    public EnchantPriority getAttackPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack chestplate, int level) {
        if (!(damager instanceof Player)) return false;

        double maxHealth = EntityUtil.getAttributeValue(damager, Attribute.MAX_HEALTH);
        if (maxHealth <= 0D) return false;

        double health = damager.getHealth();
        if (health <= 0D) return false;

        double bonusPercent = this.getDamageBonus(level) * (maxHealth / health);
        if (bonusPercent <= 0D) return false;

        double multiplier = 1D + (bonusPercent / 100D);
        event.setDamage(event.getDamage() * multiplier);
        return true;
    }
}
