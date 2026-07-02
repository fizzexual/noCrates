package com.nocrates.hook.providers;

import com.nocrates.hook.CustomItemProvider;
import com.nocrates.hook.Hooks;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** MMOItems bridge (reflective; reference format "mmoitems:TYPE:ID"). */
public final class MmoItemsProvider implements CustomItemProvider {

    @Override
    public String namespace() {
        return "mmoitems";
    }

    @Override
    public boolean available() {
        return Hooks.mmoItems();
    }

    @Override
    public ItemStack resolve(String id) {
        String[] parts = id.split(":", 2);
        if (parts.length != 2) return null;
        try {
            Class<?> plugin = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Object instance = plugin.getField("plugin").get(null);
            Class<?> typesClass = Class.forName("net.Indyuce.mmoitems.api.Type");
            Object types = plugin.getMethod("getTypes").invoke(instance);
            Object type = types.getClass().getMethod("get", String.class).invoke(types, parts[0].toUpperCase(java.util.Locale.ROOT));
            if (type == null) return null;
            Method getItem = plugin.getMethod("getItem", typesClass, String.class);
            return (ItemStack) getItem.invoke(instance, type, parts[1].toUpperCase(java.util.Locale.ROOT));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
