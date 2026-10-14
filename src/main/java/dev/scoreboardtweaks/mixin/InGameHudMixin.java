package dev.scoreboardtweaks.mixin;

import dev.scoreboardtweaks.gui.SidebarRenderer;
import dev.scoreboardtweaks.gui.PositionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void scoreboardtweaks$beginFrame(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        SidebarRenderer.beginFrame();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At("HEAD"), cancellable = true)
    private void scoreboardtweaks$renderSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        var screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof ChatScreen) && !(screen instanceof PositionScreen)) SidebarRenderer.render(context, objective);
        ci.cancel();
    }
}
