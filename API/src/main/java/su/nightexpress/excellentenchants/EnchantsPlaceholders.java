package su.nightexpress.excellentenchants;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Placeholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderList;

public class EnchantsPlaceholders extends Placeholders {

    public static final String WIKI_URL          = "https://nightexpressdev.com/excellentenchants/";
    public static final String WIKI_PLACEHOLDERS = WIKI_URL + "placeholders";
    public static final String WIKI_MODIFIERS    = WIKI_URL + "modifiers";
    public static final String WIKI_CHRAGES      = WIKI_URL + "features/charges";
    public static final String WIKI_ITEM_SETS    = WIKI_URL + "features/item-sets";

    public static final String GENERIC_TYPE        = "%type%";
    public static final String GENERIC_NAME        = "%name%";
    public static final String GENERIC_ITEM        = "%item%";
    public static final String GENERIC_LEVEL       = "%level%";
    public static final String GENERIC_AMOUNT      = "%amount%";
    public static final String GENERIC_CHARGES     = "%charges%";
    public static final String GENERIC_MODIFIER    = "%modifier%";
    public static final String GENERIC_DESCRIPTION = "%description%";
    public static final String GENERIC_ENCHANT     = "%enchant%";
    public static final String GENERIC_RADIUS      = "%radius%";
    public static final String GENERIC_DURATION    = "%duration%";
    public static final String GENERIC_DAMAGE      = "%damage%";
    public static final String GENERIC_MIN         = "%min%";
    public static final String GENERIC_MAX         = "%max%";
    public static final String GENERIC_TIME        = "%time%";
    public static final String GENERIC_KILLER      = "%killer%";

    public static final String TRIGGER_CHANCE   = "%enchantment_trigger_chance%";
    public static final String TIRGGER_INTERVAL = "%enchantment_trigger_interval%";
    public static final String EFFECT_AMPLIFIER = "%enchantment_potion_level%";
    public static final String EFFECT_DURATION  = "%enchantment_potion_duration%";
    public static final String EFFECT_TYPE      = "%enchantment_potion_type%";

    public static final String ENCHANTMENT_ID                            = "%enchantment_id%";
    public static final String ENCHANTMENT_NAME                          = "%enchantment_name%";
    public static final String ENCHANTMENT_DESCRIPTION                   = "%enchantment_description%";
    public static final String ENCHANTMENT_DESCRIPTION_REPLACED          = "%enchantment_description_replaced%";
    public static final String ENCHANTMENT_LEVEL                         = "%enchantment_level%";
    public static final String ENCHANTMENT_LEVEL_MIN                     = "%enchantment_level_min%";
    public static final String ENCHANTMENT_LEVEL_MAX                     = "%enchantment_level_max%";
    public static final String ENCHANTMENT_FIT_ITEM_TYPES                = "%enchantment_fit_item_types%";
    public static final String ENCHANTMENT_CHARGES_MAX_AMOUNT            = "%enchantment_charges_max_amount%";
    public static final String ENCHANTMENT_CHARGES_CONSUME_AMOUNT        = "%enchantment_charges_consume_amount%";
    public static final String ENCHANTMENT_CHARGES_RECHARGE_AMOUNT       = "%enchantment_charges_recharge_amount%";
    public static final String ENCHANTMENT_CHARGES_FUEL_ITEM             = "%enchantment_charges_fuel_item%";

    @NotNull
    public static PlaceholderList<Integer> forEnchant(@NotNull CustomEnchantment enchantment) {
        return PlaceholderList.create(list -> list
            .add(ENCHANTMENT_ID, level -> enchantment.getId())
            .add(ENCHANTMENT_NAME, level -> enchantment.getDisplayName())
            .add(ENCHANTMENT_DESCRIPTION, level -> String.join("\n", enchantment.getDescription()))
            .add(ENCHANTMENT_DESCRIPTION_REPLACED, level -> String.join("\n", enchantment.getDescription(level)))
            .add(ENCHANTMENT_LEVEL, NumberUtil::toRoman)
            .add(ENCHANTMENT_LEVEL_MIN, level -> String.valueOf(1))
            .add(ENCHANTMENT_LEVEL_MAX, level -> String.valueOf(enchantment.getDefinition().getMaxLevel()))
            .add(ENCHANTMENT_FIT_ITEM_TYPES, level -> enchantment.getSupportedItems().getDisplayName())
            .add(ENCHANTMENT_CHARGES_MAX_AMOUNT, level -> getChargesMaxAmount(enchantment, level))
            .add(ENCHANTMENT_CHARGES_CONSUME_AMOUNT, level -> getChargesConsumeAmount(enchantment))
            .add(ENCHANTMENT_CHARGES_RECHARGE_AMOUNT, level -> getChargesRechargeAmount(enchantment))
            .add(ENCHANTMENT_CHARGES_FUEL_ITEM, level -> getChargesFuelItem(enchantment))
            .add(TRIGGER_CHANCE, level -> getTriggerChance(enchantment, level))
            .add(TIRGGER_INTERVAL, () -> getTriggerInterval(enchantment))
            .add(EFFECT_AMPLIFIER, level -> getEffectAmplifier(enchantment, level))
            .add(EFFECT_DURATION, level -> getEffectDuration(enchantment, level))
            .add(EFFECT_TYPE, () -> getEffectType(enchantment))
        );
    }

    @NotNull
    private static String getChargesMaxAmount(@NotNull CustomEnchantment enchantment, int level) {
        if (!enchantment.hasComponent(EnchantComponent.CHARGES)) return ENCHANTMENT_CHARGES_MAX_AMOUNT;

        return NumberUtil.format(enchantment.getCharges().getMaxAmount(level));
    }

    @NotNull
    private static String getChargesConsumeAmount(@NotNull CustomEnchantment enchantment) {
        if (!enchantment.hasComponent(EnchantComponent.CHARGES)) return ENCHANTMENT_CHARGES_CONSUME_AMOUNT;

        return NumberUtil.format(enchantment.getCharges().getConsumeAmount());
    }

    @NotNull
    private static String getChargesRechargeAmount(@NotNull CustomEnchantment enchantment) {
        if (!enchantment.hasComponent(EnchantComponent.CHARGES)) return ENCHANTMENT_CHARGES_RECHARGE_AMOUNT;

        return NumberUtil.format(enchantment.getCharges().getRechargeAmount());
    }

    @NotNull
    private static String getChargesFuelItem(@NotNull CustomEnchantment enchantment) {
        if (!enchantment.hasComponent(EnchantComponent.CHARGES)) return ENCHANTMENT_CHARGES_FUEL_ITEM;

        return ItemUtil.getItemNameSerialized(enchantment.getFuel());
    }

    @NotNull
    private static String getTriggerChance(@NotNull CustomEnchantment enchantment, int level) {
        if (!enchantment.hasComponent(EnchantComponent.PROBABILITY)) return TRIGGER_CHANCE;

        return NumberUtil.format(enchantment.getComponent(EnchantComponent.PROBABILITY).getTriggerChance(level));
    }

    @NotNull
    private static String getTriggerInterval(@NotNull CustomEnchantment enchantment) {
        if (!enchantment.hasComponent(EnchantComponent.PERIODIC)) return TIRGGER_INTERVAL;

        return NumberUtil.format(enchantment.getComponent(EnchantComponent.PERIODIC).getInterval());
    }

    @NotNull
    private static String getEffectAmplifier(@NotNull CustomEnchantment enchantment, int level) {
        if (!enchantment.hasComponent(EnchantComponent.POTION_EFFECT)) return EFFECT_AMPLIFIER;

        return NumberUtil.toRoman(enchantment.getComponent(EnchantComponent.POTION_EFFECT).getAmplifier(level));
    }

    @NotNull
    private static String getEffectDuration(@NotNull CustomEnchantment enchantment, int level) {
        if (!enchantment.hasComponent(EnchantComponent.POTION_EFFECT)) return EFFECT_DURATION;

        return NumberUtil.format(enchantment.getComponent(EnchantComponent.POTION_EFFECT).getDuration(level) / 20D);
    }

    @NotNull
    private static String getEffectType(@NotNull CustomEnchantment enchantment) {
        if (!enchantment.hasComponent(EnchantComponent.POTION_EFFECT)) return EFFECT_TYPE;

        return LangUtil.getSerializedName(enchantment.getComponent(EnchantComponent.POTION_EFFECT).getType());
    }
}
