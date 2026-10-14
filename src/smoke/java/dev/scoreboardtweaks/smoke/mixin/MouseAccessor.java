package dev.scoreboardtweaks.smoke.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Invoker("onMouseButton") void smoke$button(long window, int button, int action, int modifiers);
    @Invoker("onCursorPos") void smoke$cursor(long window, double x, double y);
}
