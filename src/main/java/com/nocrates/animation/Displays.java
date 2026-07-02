package com.nocrates.animation;

import com.nocrates.text.Text;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** Temporary display entities used by opening animations (tagged, auto-cleanable). */
public final class Displays {

    public static final NamespacedKey PDC_TAG = NamespacedKey.fromString("nocrates:animation");

    private Displays() {
    }

    public static ItemDisplay item(Location at, ItemStack item, float scale) {
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class);
        display.setItemStack(item);
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 1, 0)));
        display.getPersistentDataContainer().set(PDC_TAG, PersistentDataType.STRING, "1");
        return display;
    }

    public static TextDisplay text(Location at, String miniMessage) {
        TextDisplay display = at.getWorld().spawn(at, TextDisplay.class);
        display.text(Text.mm(miniMessage));
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.setShadowed(false);
        display.setDefaultBackground(false);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.getPersistentDataContainer().set(PDC_TAG, PersistentDataType.STRING, "1");
        return display;
    }

    /** Spins an item display by updating its yaw transformation. */
    public static void spin(ItemDisplay display, float degreesPerCall) {
        Transformation t = display.getTransformation();
        AxisAngle4f rotation = new AxisAngle4f().set(t.getLeftRotation());
        float angle = rotation.angle + (float) Math.toRadians(degreesPerCall);
        display.setTransformation(new Transformation(
                t.getTranslation(),
                new org.joml.Quaternionf(new AxisAngle4f(angle, 0, 1, 0)),
                t.getScale(),
                t.getRightRotation()));
    }
}
