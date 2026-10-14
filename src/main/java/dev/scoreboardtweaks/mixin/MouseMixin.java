package dev.scoreboardtweaks.mixin;

import dev.scoreboardtweaks.gui.SidebarInteraction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void scoreboardtweaks$button(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle() && SidebarInteraction.button(button, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("TAIL"))
    private void scoreboardtweaks$cursor(long window, double x, double y, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle()) SidebarInteraction.cursorMoved();
    }
}
