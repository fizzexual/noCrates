package com.nocrates.hook;

import com.nocrates.hook.providers.ItemsAdderProvider;
import com.nocrates.hook.providers.MmoItemsProvider;
import com.nocrates.hook.providers.NexoProvider;
import com.nocrates.hook.providers.OraxenProvider;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of custom-item providers. References like {@code itemsadder:ruby_sword} in
 * ItemSpecs resolve through here; addons may register additional namespaces.
 */
public final class CustomItems {

    private static final Map<String, CustomItemProvider> PROVIDERS = new LinkedHashMap<>();

    private CustomItems() {
    }

    public static void registerDefaults() {
        register(new ItemsAdderProvider());
        register(new OraxenProvider());
        register(new NexoProvider());
        register(new MmoItemsProvider());
    }

    public static void register(CustomItemProvider provider) {
        PROVIDERS.put(provider.namespace().toLowerCase(Locale.ROOT), provider);
    }

    /** "namespace:id" -> ItemStack, or null when the provider is missing or id unknown. */
    public static ItemStack resolve(String reference) {
        if (reference == null) return null;
        int colon = reference.indexOf(':');
        if (colon <= 0) return null;
        CustomItemProvider provider = PROVIDERS.get(reference.substring(0, colon).toLowerCase(Locale.ROOT));
        if (provider == null || !provider.available()) return null;
        try {
            return provider.resolve(reference.substring(colon + 1));
        } catch (Throwable t) {
            return null;
        }
    }
}
