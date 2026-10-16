package com.example.accountmanager.mixin;

import com.example.accountmanager.gui.AccountManagerScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void am$addButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Accounts"),
                b -> this.client.setScreen(new AccountManagerScreen((Screen) (Object) this)))
                .dimensions(5, 5, 80, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void am$renderName(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("User: " + this.client.getSession().getUsername()), 5, 29, 0xAAAAAA);
    }
}
