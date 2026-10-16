package com.example.accountmanager.gui;

import com.example.accountmanager.auth.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TokenLoginScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;
    private String status = "";

    public TokenLoginScreen(Screen parent) {
        super(Text.literal("Token Login"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 20, 200, 20, Text.literal("Access token"));
        field.setMaxLength(8192);
        field.setPlaceholder(Text.literal("Minecraft access token"));
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("Login"), b -> submit())
                .dimensions(width / 2 - 100, height / 2 + 8, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(width / 2 + 2, height / 2 + 8, 98, 20).build());
    }

    private void submit() {
        String token = field.getText().trim();
        if (token.isEmpty()) return;
        status = "\u00a7eChecking token...";
        new Thread(() -> {
            try {
                Account a = MicrosoftAuth.profile(token);
                client.execute(() -> {
                    AccountStore.addOrReplace(a);
                    AccountSwitcher.apply(a);
                    close();
                });
            } catch (Exception e) {
                client.execute(() -> status = "\u00a7c" + e.getMessage());
            }
        }, "TokenLogin").start();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height / 2 + 38, 0xFFFFFF);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
