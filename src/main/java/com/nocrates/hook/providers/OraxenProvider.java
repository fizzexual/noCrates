package com.nocrates.hook.providers;

import com.nocrates.hook.CustomItemProvider;
import com.nocrates.hook.Hooks;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Oraxen bridge (reflective: io.th0rgal.oraxen.api.OraxenItems). */
public final class OraxenProvider implements CustomItemProvider {

    @Override
    public String namespace() {
        return "oraxen";
    }

    @Override
    public boolean available() {
        return Hooks.oraxen();
    }

    @Override
    public ItemStack resolve(String id) {
        try {
            Class<?> api = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Object builder = api.getMethod("getItemById", String.class).invoke(null, id);
            if (builder == null) return null;
            Method build = builder.getClass().getMethod("build");
            return (ItemStack) build.invoke(builder);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
