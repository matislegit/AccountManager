package com.example.accountmanager.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AddAccountScreen extends Screen {
    private final Screen parent;

    public AddAccountScreen(Screen parent) {
        super(Text.literal("Add Account"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 2 - 40;
        addDrawableChild(ButtonWidget.builder(Text.literal("Microsoft"),
                b -> client.setScreen(new MicrosoftLoginScreen(parent))).dimensions(x, y, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Offline Account"),
                b -> client.setScreen(new OfflineAccountScreen(parent))).dimensions(x, y + 24, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Token Login"),
                b -> client.setScreen(new TokenLoginScreen(parent))).dimensions(x, y + 48, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(x, y + 84, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 70, 0xFFFFFF);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
