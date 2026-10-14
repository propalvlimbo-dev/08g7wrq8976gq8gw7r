package dev.scoreboardtweaks.mixin;

import dev.scoreboardtweaks.gui.SidebarInteraction;
import dev.scoreboardtweaks.gui.SidebarRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique private ButtonWidget scoreboardtweaks$settings;
    @Unique private ButtonWidget scoreboardtweaks$move;

    protected ChatScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void scoreboardtweaks$addSettingsButton(CallbackInfo ci) {
        scoreboardtweaks$move = addSelectableChild(ButtonWidget.builder(
            Text.translatable("scoreboardtweaks.move"), button -> SidebarInteraction.openPosition())
            .dimensions(width - 212, 4, 104, 20).build());
        // A visible fallback also lets the user tell whether the mod actually loaded.
        scoreboardtweaks$settings = addSelectableChild(ButtonWidget.builder(
            Text.translatable("scoreboardtweaks.open_settings"), button -> SidebarInteraction.openSettings())
            .dimensions(width - 104, 4, 100, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void scoreboardtweaks$hint(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SidebarRenderer.renderActive(context);
        SidebarInteraction.renderChatOverlay(context, mouseX, mouseY);
        if (scoreboardtweaks$move != null) scoreboardtweaks$move.render(context, mouseX, mouseY, delta);
        if (scoreboardtweaks$settings != null) scoreboardtweaks$settings.render(context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void scoreboardtweaks$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (scoreboardtweaks$move != null && scoreboardtweaks$move.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        } else if (scoreboardtweaks$settings != null && scoreboardtweaks$settings.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        } else if (SidebarInteraction.mousePressed(mouseX, mouseY, button)) {
            setDragging(true);
            cir.setReturnValue(true);
        }
    }

    // ChatScreen inherits these callbacks; add overrides instead of injecting into nonexistent methods.
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (SidebarInteraction.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (SidebarInteraction.mouseReleased(mouseX, mouseY, button)) {
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void scoreboardtweaks$settingsShortcut(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_K && (Screen.hasControlDown() || (modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
            SidebarInteraction.openSettings();
            cir.setReturnValue(true);
        }
    }
}
