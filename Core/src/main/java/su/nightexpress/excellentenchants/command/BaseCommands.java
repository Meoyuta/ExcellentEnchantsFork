package su.nightexpress.excellentenchants.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.config.Lang;
import su.nightexpress.excellentenchants.config.Perms;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;
import su.nightexpress.excellentenchants.enchantment.universal.UnyieldingEnchant;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.HubNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.bridge.RegistryType;

public class BaseCommands {

    private final EnchantsPlugin plugin;

    public BaseCommands(@NotNull EnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(@NotNull HubNodeBuilder builder) {
        builder.branch(Commands.literal("reload")
            .description(Lang.COMMAND_RELOAD_DESC)
            .permission(Perms.COMMAND_RELOAD)
            .executes((context, arguments) -> {
                this.plugin.doReload(context.getSender());
                return true;
            })
        );

        builder.branch(Commands.literal("book")
            .description(Lang.COMMAND_BOOK_DESC)
            .permission(Perms.COMMAND_BOOK)
            .withArguments(
                CommandArguments.enchantArgument(CommandArguments.ENCHANT),
                CommandArguments.levelArgument(CommandArguments.LEVEL).optional(),
                Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).optional()
            )
            .withFlags(CommandArguments.FLAG_CHARGED)
            .executes(this::giveEnchantBook)
        );

        builder.branch(Commands.literal("randombook")
            .description(Lang.COMMAND_RANDOM_BOOK_DESC)
            .permission(Perms.COMMAND_RANDOM_BOOK)
            .withArguments(Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).optional())
            .withFlags(CommandArguments.FLAG_CUSTOM, CommandArguments.FLAG_CHARGED)
            .executes(this::giveRandomBook)
        );

        builder.branch(Commands.literal("enchant")
            .description(Lang.COMMAND_ENCHANT_DESC)
            .permission(Perms.COMMAND_ENCHANT)
            .withArguments(
                CommandArguments.enchantArgument(CommandArguments.ENCHANT),
                CommandArguments.levelArgument(CommandArguments.LEVEL),
                Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).optional(),
                CommandArguments.slotArgument(CommandArguments.SLOT).optional()
            )
            .withFlags(CommandArguments.FLAG_CHARGED)
            .executes(this::enchantItem)
        );

        builder.branch(Commands.literal("disenchant")
            .description(Lang.COMMAND_DISENCHANT_DESC)
            .permission(Perms.COMMAND_DISENCHANT)
            .withArguments(
                CommandArguments.enchantArgument(CommandArguments.ENCHANT),
                Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).optional(),
                CommandArguments.slotArgument(CommandArguments.SLOT).optional()
            )
            .executes(this::disenchantItem)
        );

        builder.branch(Commands.literal("list")
            .playerOnly()
            .description(Lang.COMMAND_LIST_DESC)
            .permission(Perms.COMMAND_LIST)
            .withArguments(Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).permission(Perms.COMMAND_LIST_OTHERS).optional())
            .executes(this::openList)
        );

        builder.branch(Commands.literal("query")
            .playerOnly()
            .description(Lang.COMMAND_QUERY_DESC)
            .permission(Perms.COMMAND_QUERY)
            .withArguments(Arguments.string(CommandArguments.QUERY)
                .localized(Lang.COMMAND_ARGUMENT_NAME_QUERY)
                .suggestions((reader, context) -> Lists.newList(UnyieldingEnchant.ID))
            )
            .executes(this::queryEnchant)
        );

        builder.branch(Commands.literal("unyielding")
            .description(Lang.COMMAND_UNYIELDING_DESC)
            .permission(Perms.COMMAND_UNYIELDING)
            .withArguments(
                Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER),
                Arguments.bool(CommandArguments.STATE).localized(Lang.COMMAND_ARGUMENT_NAME_STATE)
            )
            .executes(this::setUnyieldingState)
        );

        if (Config.isChargesEnabled()) {
            builder.branch(Commands.literal("givefuel")
                .playerOnly()
                .description(Lang.COMMAND_GIVE_FUEL_DESC)
                .permission(Perms.COMMAND_GIVE_FUEL)
                .withArguments(
                    CommandArguments.customEnchantArgument(CommandArguments.ENCHANT)
                        .suggestions((reader, context) -> EnchantRegistry.getRegistered().stream().filter(CustomEnchantment::isChargeable).map(CustomEnchantment::getId).toList()),
                    Arguments.integer(CommandArguments.AMOUNT, 1).localized(Lang.COMMAND_ARGUMENT_NAME_AMOUNT).suggestions((rader, context) -> Lists.newList("1", "8", "16", "32", "64")).optional(),
                    Arguments.player(CommandArguments.PLAYER).localized(Lang.COMMAND_ARGUMENT_NAME_PLAYER).optional()
                )
                .executes(this::giveFuel)
            );
        }
    }

    private int getLevel(@NotNull Enchantment enchantment, @NotNull ParsedArguments arguments) {
        int level = arguments.getInt(CommandArguments.LEVEL, -1);
        if (level <= 0) {
            level = EnchantsUtils.randomLevel(enchantment);
        }
        return level;
    }

    private boolean giveEnchantBook(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();

        boolean charged = context.hasFlag(CommandArguments.FLAG_CHARGED);
        Enchantment enchantment = arguments.getEnchantment(CommandArguments.ENCHANT);
        int level = getLevel(enchantment, arguments);

        return this.giveBook(context.getSender(), player, enchantment, level, charged);
    }

    private boolean giveRandomBook(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();

        boolean custom = context.hasFlag(CommandArguments.FLAG_CUSTOM);
        boolean charged = context.hasFlag(CommandArguments.FLAG_CHARGED);
        Enchantment enchantment = Randomizer.pick(custom ? EnchantRegistry.getRegisteredBukkit() : BukkitThing.getAll(RegistryType.ENCHANTMENT));
        int level = EnchantsUtils.randomLevel(enchantment);

        return this.giveBook(context.getSender(), player, enchantment, level, charged);
    }

    private boolean giveBook(@NotNull CommandSender sender, @NotNull Player player, @NotNull Enchantment enchantment, int level, boolean charged) {
        ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
        if (charged) {
            EnchantsUtils.restoreCharges(itemStack, enchantment, level);
        }

        EnchantsUtils.add(itemStack, enchantment, level, true);
        Players.addItem(player, itemStack);

        Lang.ENCHANTED_BOOK_GAVE.message().send(sender, replacer -> replacer
            .replace(EnchantsPlaceholders.GENERIC_ENCHANT, LangUtil.getSerializedName(enchantment))
            .replace(EnchantsPlaceholders.GENERIC_LEVEL, NumberUtil.toRoman(level))
            .replace(EnchantsPlaceholders.forPlayer(player))
        );

        return true;
    }

    private boolean enchantItem(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();
        EquipmentSlot slot = arguments.getOr(CommandArguments.SLOT, EquipmentSlot.class, EquipmentSlot.HAND);

        ItemStack itemStack = EntityUtil.getItemInSlot(player, slot);
        if (itemStack == null || itemStack.getType().isAir()) {
            context.send(Lang.COMMAND_ENCHANT_ERROR_NO_ITEM);
            return false;
        }

        boolean charged = context.hasFlag(CommandArguments.FLAG_CHARGED);
        Enchantment enchantment = arguments.getEnchantment(CommandArguments.ENCHANT);
        int level = getLevel(enchantment, arguments);

        EnchantsUtils.add(itemStack, enchantment, level, true);

        if (charged) {
            EnchantsUtils.restoreCharges(itemStack, enchantment, level);
        }

        context.send(context.getSender() == player ? Lang.COMMAND_ENCHANT_DONE_SELF : Lang.COMMAND_ENCHANT_DONE_OTHERS, replacer -> replacer
            .replace(EnchantsPlaceholders.forPlayer(player))
            .replace(EnchantsPlaceholders.GENERIC_ITEM, ItemUtil.getNameSerialized(itemStack))
            .replace(EnchantsPlaceholders.GENERIC_ENCHANT, LangUtil.getSerializedName(enchantment))
            .replace(EnchantsPlaceholders.GENERIC_LEVEL, NumberUtil.toRoman(level))
        );

        return true;
    }

    private boolean disenchantItem(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();
        EquipmentSlot slot = arguments.getOr(CommandArguments.SLOT, EquipmentSlot.class, EquipmentSlot.HAND);

        ItemStack itemStack = EntityUtil.getItemInSlot(player, slot);
        if (itemStack == null || itemStack.getType().isAir()) {
            context.send(Lang.COMMAND_ENCHANT_ERROR_NO_ITEM);
            return false;
        }

        Enchantment enchantment = arguments.getEnchantment(CommandArguments.ENCHANT);
        EnchantsUtils.remove(itemStack, enchantment);

        context.send(context.getSender() == player ? Lang.COMMAND_DISENCHANT_DONE_SELF : Lang.COMMAND_DISENCHANT_DONE_OTHERS, replacer -> replacer
            .replace(EnchantsPlaceholders.forPlayer(player))
            .replace(EnchantsPlaceholders.GENERIC_ITEM, ItemUtil.getNameSerialized(itemStack))
            .replace(EnchantsPlaceholders.GENERIC_ENCHANT, LangUtil.getSerializedName(enchantment))
        );

        return true;
    }

    private boolean giveFuel(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();
        CustomEnchantment enchantment = arguments.get(CommandArguments.ENCHANT, CustomEnchantment.class);
        int amount = arguments.getInt(CommandArguments.AMOUNT, 1);

        if (!enchantment.isChargeable()) {
            context.send(Lang.CHARGES_FUEL_BAD_ENCHANTMENT, replacer -> replacer.replace(EnchantsPlaceholders.GENERIC_NAME, enchantment.getDisplayName()));
            return false;
        }

        ItemStack fuel = enchantment.getFuel();
        fuel.setAmount(amount);

        Players.addItem(player, fuel);

        context.send(Lang.CHARGES_FUEL_GAVE, replacer -> replacer
            .replace(EnchantsPlaceholders.GENERIC_AMOUNT, NumberUtil.format(amount))
            .replace(EnchantsPlaceholders.GENERIC_NAME, ItemUtil.getNameSerialized(fuel))
            .replace(EnchantsPlaceholders.forPlayer(player))
        );

        return true;
    }

    private boolean openList(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer() && !arguments.contains(CommandArguments.PLAYER)) {
            context.printUsage();
            return false;
        }

        Player player = arguments.contains(CommandArguments.PLAYER) ? arguments.getPlayer(CommandArguments.PLAYER) : context.getPlayerOrThrow();
        this.plugin.getEnchantManager().openEnchantsMenu(player);

        if (player != context.getSender()) {
            context.send(Lang.COMMAND_LIST_DONE_OTHERS, replacer -> replacer.replace(EnchantsPlaceholders.forPlayer(player)));
        }
        return true;
    }

    private boolean queryEnchant(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String query = arguments.getString(CommandArguments.QUERY);

        if (!query.equalsIgnoreCase(UnyieldingEnchant.ID)) {
            context.send(Lang.COMMAND_QUERY_UNKNOWN, replacer -> replacer
                .replace(EnchantsPlaceholders.GENERIC_TYPE, query)
                .replace(EnchantsPlaceholders.GENERIC_NAME, query)
            );
            return false;
        }

        CustomEnchantment enchantment = EnchantRegistry.getById(UnyieldingEnchant.ID);
        if (!(enchantment instanceof UnyieldingEnchant unyieldingEnchant)) {
            context.send(Lang.COMMAND_QUERY_UNYIELDING_HEADER);
            context.send(Lang.COMMAND_QUERY_UNYIELDING_UNAVAILABLE);
            context.send(Lang.COMMAND_QUERY_UNYIELDING_FOOTER);
            return true;
        }

        context.send(Lang.COMMAND_QUERY_UNYIELDING_HEADER);

        var statusesOptional = unyieldingEnchant.getDamageStatuses(player);
        if (statusesOptional.isEmpty()) {
            context.send(Lang.COMMAND_QUERY_UNYIELDING_INACTIVE);
            context.send(Lang.COMMAND_QUERY_UNYIELDING_FOOTER);
            return true;
        }

        var statuses = statusesOptional.get();
        if (statuses.isEmpty()) {
            context.send(Lang.COMMAND_QUERY_UNYIELDING_WAITING);
            context.send(Lang.COMMAND_QUERY_UNYIELDING_FOOTER);
            return true;
        }

        statuses.forEach(status -> {
            String damageId = status.damageId();
            context.send(Lang.COMMAND_QUERY_UNYIELDING_STATUS, replacer -> replacer
                .replace(EnchantsPlaceholders.GENERIC_TYPE, UnyieldingEnchant.formatDamageId(damageId))
                .replace(EnchantsPlaceholders.GENERIC_NAME, damageId)
                .replace(EnchantsPlaceholders.GENERIC_AMOUNT, NumberUtil.format(status.streak()))
                .replace(EnchantsPlaceholders.GENERIC_MODIFIER, NumberUtil.format(status.nextMultiplier()))
                .replace(EnchantsPlaceholders.GENERIC_DAMAGE, NumberUtil.format(status.getReductionPercent()))
            );
        });
        context.send(Lang.COMMAND_QUERY_UNYIELDING_FOOTER);
        return true;
    }

    private boolean setUnyieldingState(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        Player player = arguments.getPlayer(CommandArguments.PLAYER);
        boolean state = arguments.getBoolean(CommandArguments.STATE);

        CustomEnchantment enchantment = EnchantRegistry.getById(UnyieldingEnchant.ID);
        if (!(enchantment instanceof UnyieldingEnchant unyieldingEnchant)) {
            context.send(Lang.COMMAND_QUERY_UNYIELDING_UNAVAILABLE);
            return false;
        }

        unyieldingEnchant.setActive(player, state);
        context.send(state ? Lang.COMMAND_UNYIELDING_ENABLED : Lang.COMMAND_UNYIELDING_DISABLED, replacer -> replacer
            .replace(EnchantsPlaceholders.forPlayer(player))
        );
        return true;
    }
}
