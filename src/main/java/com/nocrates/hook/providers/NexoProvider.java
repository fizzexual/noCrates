package com.nocrates.hook.providers;

import com.nocrates.hook.CustomItemProvider;
import com.nocrates.hook.Hooks;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Nexo bridge (reflective: com.nexomc.nexo.api.NexoItems). */
public final class NexoProvider implements CustomItemProvider {

    @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean available() {
        return Hooks.nexo();
    }

    @Override
    public ItemStack resolve(String id) {
        try {
            Class<?> api = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object builder = api.getMethod("itemFromId", String.class).invoke(null, id);
            if (builder == null) return null;
            Method build = builder.getClass().getMethod("build");
            return (ItemStack) build.invoke(builder);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
