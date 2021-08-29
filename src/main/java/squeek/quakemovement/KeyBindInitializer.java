package squeek.quakemovement;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeyBindInitializer implements ClientModInitializer {
    public static KeyBinding enableKey;

    @Override
    public void onInitializeClient() {
        enableKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.squake.enable",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.squake.binds"
        ));
    }
}
