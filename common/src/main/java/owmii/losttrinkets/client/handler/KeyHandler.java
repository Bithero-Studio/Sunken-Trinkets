package owmii.losttrinkets.client.handler;

import me.shedaniel.architectury.event.events.client.ClientRawInputEvent;
import me.shedaniel.architectury.registry.KeyBindings;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.ActionResult;
import owmii.losttrinkets.client.screen.TrinketsScreen;
import owmii.losttrinkets.item.trinkets.MagnetoTrinket;

import static net.minecraft.client.util.InputUtil.Type.KEYSYM;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;

public class KeyHandler {
    public static final String TRINKET_CATEGORY = "key.categories.losttrinkets";
    public static final KeyBinding TRINKET_GUI = new KeyBinding("key.losttrinkets.trinket", KEYSYM, GLFW_KEY_R, TRINKET_CATEGORY);
    public static final KeyBinding MAGNETO = new KeyBinding("key.losttrinkets.magneto", KEYSYM, -1, TRINKET_CATEGORY);

    public static void register() {
        KeyBindings.registerKeyBinding(TRINKET_GUI);
        KeyBindings.registerKeyBinding(MAGNETO);
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (TRINKET_GUI.wasPressed()) {
                client.openScreen(new TrinketsScreen());
            }
            if (MAGNETO.wasPressed()) {
                MagnetoTrinket.trySendCollect(client.player);
            }
            return ActionResult.PASS;
        });
    }
}
