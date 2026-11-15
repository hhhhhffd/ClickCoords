package com.clickcoords.mixin;

import com.clickcoords.ChatCoordinates;
import com.clickcoords.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void clickcoords$waypoint(Style style, boolean shift, CallbackInfoReturnable<Boolean> result) {
        if (!shift) return;
        String coordinates = ChatCoordinates.recognizedCopy(style.getClickEvent());
        if (coordinates != null && Waypoints.toggle(Minecraft.getInstance(), coordinates)) {
            result.setReturnValue(true);
        }
    }
}
