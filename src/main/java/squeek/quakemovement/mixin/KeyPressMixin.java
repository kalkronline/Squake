package squeek.quakemovement.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.quakemovement.Config;
import squeek.quakemovement.KeyBindInitializer;

@Mixin(MinecraftClient.class)
public class KeyPressMixin
{
	@Inject(method = "handleInputEvents", at=@At("HEAD"))
	public void handleInputEvents(CallbackInfo info) {
		if (KeyBindInitializer.enableKey.wasPressed()) {
			Config.ENABLED = !Config.ENABLED;
			MinecraftClient.getInstance().inGameHud.addChatMessage(MessageType.CHAT, new TranslatableText("key.squake.toggle" + (Config.ENABLED ? "on" : "off")), Util.NIL_UUID);
		}
	}
}
