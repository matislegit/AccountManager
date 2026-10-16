package com.example.accountmanager.gui;

import com.example.accountmanager.auth.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

public class MicrosoftLoginScreen extends Screen {
    private final Screen parent;
    private volatile boolean cancelled = false;
    private volatile String line1 = "Contacting Microsoft...";
    private volatile String line2 = "";
    private volatile MicrosoftAuth.DeviceCode code;
    private ButtonWidget openBtn, copyBtn;

    public MicrosoftLoginScreen(Screen parent) {
        super(Text.literal("Microsoft Login"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Open Link"), b -> {
            if (code != null) Util.getOperatingSystem().open(URI.create(code.uri()));
        }).dimensions(width / 2 - 100, height / 2 + 10, 98, 20).build());
        copyBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Copy Code"), b -> {
            if (code != null) client.keyboard.setClipboard(code.userCode());
        }).dimensions(width / 2 + 2, height / 2 + 10, 98, 20).build());
        openBtn.active = copyBtn.active = code != null;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(width / 2 - 100, height / 2 + 34, 200, 20).build());

        if (code == null && !cancelled) startLogin();
    }

    private void startLogin() {
        new Thread(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = MicrosoftAuth.requestDeviceCode();
                code = dc;
                line1 = "Go to " + dc.uri() + " and enter:";
                line2 = "\u00a7e\u00a7l" + dc.userCode();
                client.execute(() -> { openBtn.active = true; copyBtn.active = true; });

                Account a = MicrosoftAuth.waitForLogin(dc, () -> cancelled);
                client.execute(() -> {
                    AccountStore.addOrReplace(a);
                    AccountSwitcher.apply(a);
                    close();
                });
            } catch (Exception e) {
                if (!cancelled) { line1 = "\u00a7c" + e.getMessage(); line2 = ""; }
            }
        }, "MicrosoftLogin").start();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 60, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line1), width / 2, height / 2 - 30, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line2), width / 2, height / 2 - 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        cancelled = true;
        client.setScreen(parent);
    }
}
