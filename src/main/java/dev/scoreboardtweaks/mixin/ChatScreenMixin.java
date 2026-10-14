package dev.scoreboardtweaks.mixin;

import dev.scoreboardtweaks.gui.SidebarInteraction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void scoreboardtweaks$hint(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SidebarInteraction.renderChatOverlay(context, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void scoreboardtweaks$settingsShortcut(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_K && Screen.hasControlDown()) {
            SidebarInteraction.openSettings();
            cir.setReturnValue(true);
        }
    }
}
