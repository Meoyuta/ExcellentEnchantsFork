package su.nightexpress.excellentenchants.enchantment.armor;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.PotionEffects;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.DefendEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Randomizer;

import java.nio.file.Path;

public class NourishEnchant extends GameEnchantment implements DefendEnchant {

    public NourishEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(4, 2));
        this.addComponent(EnchantComponent.POTION_EFFECT, PotionEffects.temporal(PotionEffectType.REGENERATION, Modifier.addictive(3).perLevel(1).capacity(10)));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {

    }

    @NotNull
    @Override
    public EnchantPriority getProtectPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onProtect(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        return this.addRandomPotionEffect(victim, level);
    }

    private boolean addRandomPotionEffect(@NotNull LivingEntity victim, int level) {
        PotionEffects potionEffects = this.getComponent(EnchantComponent.POTION_EFFECT);
        int maxSeconds = Math.max(1, (int) Math.ceil(potionEffects.getDuration(level) / 20D));
        int seconds = Randomizer.nextInt(1, maxSeconds + 1);
        int amplifier = Math.max(0, potionEffects.getAmplifier(level) - 1);

        return victim.addPotionEffect(new PotionEffect(potionEffects.getType(), seconds * 20, amplifier, false, this.hasVisualEffects()));
    }
}
