package su.nightexpress.excellentenchants.enchantment.weapon;

import org.bukkit.Particle;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.AttackEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.nio.file.Path;

public class ShadowBindEnchant extends GameEnchantment implements AttackEnchant {

    private Modifier duration;

    public ShadowBindEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(0, 25));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.duration = Modifier.load(config, "ShadowBind.Duration",
            Modifier.addictive(0).perLevel(0.5).capacity(2),
            "Enderman teleport suppression duration (in seconds)."
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_DURATION, level -> NumberUtil.format(this.getDuration(level)));
    }

    public double getDuration(int level) {
        return this.duration.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getAttackPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        if (!(victim instanceof Enderman enderman)) return false;

        long durationTicks = Math.max(1L, Math.round(this.getDuration(level) * 20D));
        this.plugin.getEnchantManager().suppressEndermanTeleport(enderman, durationTicks);

        if (this.hasVisualEffects()) {
            UniParticle.of(Particle.PORTAL).play(victim.getEyeLocation(), 0.35, 0.1, 35);
        }
        return true;
    }
}
