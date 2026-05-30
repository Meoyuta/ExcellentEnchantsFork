package su.nightexpress.excellentenchants.bridge.spigot;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsKeys;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.bridge.RegistryHack;
import su.nightexpress.excellentenchants.enchantment.DistributionConfig;
import su.nightexpress.excellentenchants.enchantment.EnchantCatalog;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;
import su.nightexpress.nightcore.util.Version;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SpigotEnchantsBootstrap {

    private static final Map<Version, String> NMS_CLASSES = new HashMap<>();

    static {
        try {
            NMS_CLASSES.put(Version.MC_1_21_8, "su.nightexpress.excellentenchants.nms.mc_1_21_8.RegistryHack_1_21_8");
            NMS_CLASSES.put(Version.MC_1_21_10, "su.nightexpress.excellentenchants.nms.mc_1_21_10.RegistryHack_1_21_10");
            NMS_CLASSES.put(Version.MC_1_21_11, "su.nightexpress.excellentenchants.nms.mc_1_21_11.RegistryHack_1_21_11");
            NMS_CLASSES.put(Version.MC_26_1_2, "su.nightexpress.excellentenchants.nms.mc_26_1_2.RegistryHack_26_1_2");
        } catch (Exception ignored) {
        }
    }

    public void bootstrap(@NotNull EnchantsPlugin plugin) {
        RegistryHack registryHack = loadRegistryHack(plugin);

        if (registryHack == null) {
            plugin.error("Unsupported server version!");
            plugin.getPluginManager().disablePlugin(plugin);
            return;
        }

        Path dataDirectory = plugin.getDataFolder().toPath();

        DistributionConfig distributionConfig = DistributionConfig.load(dataDirectory);
        if (distributionConfig.isUseMinecraftNamespace()) {
            EnchantsKeys.setVanillaNamespace();
        }

        registryHack.unfreezeRegistry();

        SpigotItemTagLookup tagLookup = new SpigotItemTagLookup();
        ItemSetRegistry itemSetRegistry = new ItemSetRegistry(dataDirectory, tagLookup);

        // Load ItemTag set objects and register server Tags for them.
        itemSetRegistry.load();
        itemSetRegistry.values().forEach(registryHack::createItemsSet);

        EnchantCatalog.loadAll(dataDirectory, itemSetRegistry, (entry, exception) -> plugin.error("Could not load '%s' enchantment: '%s'".formatted(entry.getId(), exception.getMessage())));

        EnchantCatalog.enabled().forEach(catalog -> registryHack.registerEnchantment(catalog, distributionConfig));

        EnchantRegistry.getRegistered().forEach(registryHack::addExclusives);

        registryHack.freezeRegistry();
    }

    private RegistryHack loadRegistryHack(@NotNull EnchantsPlugin plugin) {
        Version version = Version.getCurrent();
        String className = NMS_CLASSES.get(version);

        if (className == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(EnchantsPlugin.class);
            return (RegistryHack) constructor.newInstance(plugin);
        } catch (ClassNotFoundException e) {
            plugin.error("NMS module not found for version " + version + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            plugin.error("Failed to load NMS module for version " + version + ": " + e.getMessage());
            return null;
        }
    }
}
