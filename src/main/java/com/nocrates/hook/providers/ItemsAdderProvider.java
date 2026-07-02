package com.nocrates.hook.providers;

import com.nocrates.hook.CustomItemProvider;
import com.nocrates.hook.Hooks;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** ItemsAdder bridge (reflective: dev.lone.itemsadder.api.CustomStack). */
public final class ItemsAdderProvider implements CustomItemProvider {

    @Override
    public String namespace() {
        return "itemsadder";
    }

    @Override
    public boolean available() {
        return Hooks.itemsAdder();
    }

    @Override
    public ItemStack resolve(String id) {
        try {
            Class<?> cs = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = cs.getMethod("getInstance", String.class);
            Object stack = getInstance.invoke(null, id);
            if (stack == null) return null;
            return (ItemStack) cs.getMethod("getItemStack").invoke(stack);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
