package su.nightexpress.excellentenchants.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.excellentenchants.EnchantsPlaceholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class Lang implements LangContainer {

    public static final TextLocale COMMAND_ARGUMENT_NAME_PLAYER      = LangEntry.builder("Command.Argument.Name.Player").text("player");
    public static final TextLocale COMMAND_ARGUMENT_NAME_AMOUNT      = LangEntry.builder("Command.Argument.Name.Amount").text("amount");
    public static final TextLocale COMMAND_ARGUMENT_NAME_LEVEL       = LangEntry.builder("Command.Argument.Name.Level").text("level");
    public static final TextLocale COMMAND_ARGUMENT_NAME_SLOT        = LangEntry.builder("Command.Argument.Name.Slot").text("slot");
    public static final TextLocale COMMAND_ARGUMENT_NAME_ENCHANTMENT = LangEntry.builder("Command.Argument.Name.Enchantment").text("enchantment");
    public static final TextLocale COMMAND_ARGUMENT_NAME_QUERY       = LangEntry.builder("Command.Argument.Name.Query").text("query");
    public static final TextLocale COMMAND_ARGUMENT_NAME_STATE       = LangEntry.builder("Command.Argument.Name.State").text("state");

    public static final TextLocale COMMAND_RELOAD_DESC      = LangEntry.builder("Command.Reload.Desc").text("Reload the plugin.");
    public static final TextLocale COMMAND_LIST_DESC        = LangEntry.builder("Command.List.Desc").text("Open enchantment information browser.");
    public static final TextLocale COMMAND_ENCHANT_DESC     = LangEntry.builder("Command.Enchant.Desc").text("Enchant item in specific slot.");
    public static final TextLocale COMMAND_DISENCHANT_DESC  = LangEntry.builder("Command.Disenchant.Desc").text("Disenchant item in specific slot.");
    public static final TextLocale COMMAND_BOOK_DESC        = LangEntry.builder("Command.Book.Desc").text("Give a book with specific enchantment.");
    public static final TextLocale COMMAND_RANDOM_BOOK_DESC = LangEntry.builder("Command.RandomBook.Desc").text("Give a book with random enchantment.");
    public static final TextLocale COMMAND_GIVE_FUEL_DESC   = LangEntry.builder("Command.GiveFuel.Desc").text("Give enchantment fuel item.");
    public static final TextLocale COMMAND_QUERY_DESC       = LangEntry.builder("Command.Query.Desc").text("Query enchantment runtime status.");
    public static final TextLocale COMMAND_UNYIELDING_DESC  = LangEntry.builder("Command.Unyielding.Desc").text("Set player's unyielding active flag.");

    public static final MessageLocale COMMAND_LIST_DONE_OTHERS = LangEntry.builder("Command.List.DoneOthers").chatMessage(
        GRAY.wrap("Opened enchantments GUI for " + SOFT_YELLOW.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale COMMAND_QUERY_UNKNOWN = LangEntry.builder("Command.Query.Unknown").chatMessage(
        GRAY.wrap("Unknown query target: " + SOFT_RED.wrap(GENERIC_TYPE) + ".")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_UNAVAILABLE = LangEntry.builder("Command.Query.Unyielding.Unavailable").chatMessage(
        SOFT_RED.wrap(GENERIC_ENCHANT + " enchantment is not loaded.")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_INACTIVE = LangEntry.builder("Command.Query.Unyielding.Inactive").chatMessage(
        GRAY.wrap(GENERIC_ENCHANT + " is inactive. Trigger a totem enchanted with it first, or enable the flag with the admin command.")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_WAITING = LangEntry.builder("Command.Query.Unyielding.Waiting").chatMessage(
        GRAY.wrap(GENERIC_ENCHANT + " is active, but no damage types have been recorded yet.")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_HEADER = LangEntry.builder("Command.Query.Unyielding.Header").chatMessage(
        GRAY.wrap("──────── " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " Status") + " ────────")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_STATUS = LangEntry.builder("Command.Query.Unyielding.Status").chatMessage(
        GRAY.wrap(GENERIC_ENCHANT + " " + SOFT_YELLOW.wrap(GENERIC_TYPE) + ": " + SOFT_YELLOW.wrap(GENERIC_AMOUNT) +
            " hits, next same-type damage multiplier " + SOFT_YELLOW.wrap(GENERIC_MODIFIER) +
            ", reduction " + SOFT_YELLOW.wrap(GENERIC_DAMAGE + "%") + ".")
    );

    public static final MessageLocale COMMAND_QUERY_UNYIELDING_FOOTER = LangEntry.builder("Command.Query.Unyielding.Footer").chatMessage(
        GRAY.wrap("────────────────────")
    );

    public static final MessageLocale UNYIELDING_IMMUNITY_REACHED = LangEntry.builder("Unyielding.ImmunityReached").chatMessage(
        GRAY.wrap(GENERIC_ENCHANT + " has made you immune to " + SOFT_YELLOW.wrap(GENERIC_TYPE) + " damage.")
    );

    public static final MessageLocale COMMAND_UNYIELDING_ENABLED = LangEntry.builder("Command.Unyielding.Enabled").chatMessage(
        GRAY.wrap("Enabled the " + GENERIC_ENCHANT + " flag for " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + ".")
    );

    public static final MessageLocale COMMAND_UNYIELDING_DISABLED = LangEntry.builder("Command.Unyielding.Disabled").chatMessage(
        GRAY.wrap("Disabled the " + GENERIC_ENCHANT + " flag for " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " and cleared its tracked status.")
    );


    public static final MessageLocale COMMAND_ENCHANT_DONE_SELF = LangEntry.builder("Command.Enchant.Done.Self").chatMessage(
        GRAY.wrap(SOFT_YELLOW.wrap(GENERIC_ITEM) + " enchanted with " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + "!")
    );

    public static final MessageLocale COMMAND_ENCHANT_DONE_OTHERS = LangEntry.builder("Command.Enchant.Done.Others").chatMessage(
        GRAY.wrap(SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + "'s " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " enchanted with " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + "!")
    );

    public static final MessageLocale COMMAND_DISENCHANT_DONE_SELF = LangEntry.builder("Command.Disenchant.Done.Self").chatMessage(
        GRAY.wrap(SOFT_YELLOW.wrap(GENERIC_ITEM) + " disenchanted from " + SOFT_YELLOW.wrap(GENERIC_ENCHANT) + "!")
    );

    public static final MessageLocale COMMAND_DISENCHANT_DONE_OTHERS = LangEntry.builder("Command.Disenchant.Done.Others").chatMessage(
        GRAY.wrap(SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + "'s " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " disenchanted from " + SOFT_YELLOW.wrap(GENERIC_ENCHANT) + "!")
    );

    public static final MessageLocale COMMAND_ENCHANT_ERROR_NO_ITEM = LangEntry.builder("Command.Enchant.Error.NoItem").chatMessage(
        SOFT_RED.wrap("There is no item to enchant!")
    );

    public static final MessageLocale ENCHANTED_BOOK_GAVE = LangEntry.builder("Command.Book.Done").chatMessage(
        GRAY.wrap("You gave " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + " book to " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + ".")
    );

    public static final MessageLocale CHARGES_FUEL_GAVE = LangEntry.builder("Charges.Fuel.Gave").chatMessage(
        GRAY.wrap("You gave " + SOFT_YELLOW.wrap("x" + GENERIC_AMOUNT + " " + GENERIC_NAME) + " fuel to " + SOFT_YELLOW.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale CHARGES_FUEL_BAD_ENCHANTMENT = LangEntry.builder("Charges.Fuel.BadEnchantment").chatMessage(
        GRAY.wrap("Enchantment " + SOFT_RED.wrap(GENERIC_NAME) + " can't be charged.")
    );


    public static final MessageLocale COMMAND_SYNTAX_INVALID_SLOT = LangEntry.builder("Command.Syntax.InvalidSlot").chatMessage(
        GRAY.wrap(SOFT_RED.wrap(GENERIC_INPUT) + " is not a valid slot!")
    );
}
