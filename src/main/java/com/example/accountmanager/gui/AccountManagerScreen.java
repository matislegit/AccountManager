package com.example.accountmanager.gui;

import com.example.accountmanager.auth.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class AccountManagerScreen extends Screen {
    private static final int PER_PAGE = 6;
    private final Screen parent;
    private int page = 0;
    private String status = "";

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Account Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Account> all = AccountStore.accounts;
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        String current = client.getSession().getUsername();

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= all.size()) break;
            Account a = all.get(idx);
            int y = 40 + i * 24;
            String label = (a.name.equals(current) ? "\u00a7a> \u00a7r" : "") + a.name + " \u00a77(" + a.type + ")";
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> switchTo(a))
                    .dimensions(width / 2 - 110, y, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cX"), b -> {
                AccountStore.remove(a);
                clearAndInit();
            }).dimensions(width / 2 + 94, y, 20, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); })
                .dimensions(width / 2 - 140, 40, 24, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); })
                .dimensions(width / 2 + 118, 40, 24, 20).build()).active = page < pages - 1;

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Account"),
                b -> client.setScreen(new AddAccountScreen(this)))
                .dimensions(width / 2 - 102, height - 52, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(width / 2 + 2, height - 52, 100, 20).build());
    }

    private void switchTo(Account a) {
        status = "\u00a7eSwitching...";
        new Thread(() -> {
            try {
                Account use = a;
                if (a.type.equals("MICROSOFT")) {
                    Account fresh = MicrosoftAuth.refresh(a);
                    a.accessToken = fresh.accessToken;
                    a.refreshToken = fresh.refreshToken;
                    a.name = fresh.name;
                    AccountStore.save();
                }
                client.execute(() -> {
                    AccountSwitcher.apply(use);
                    status = "\u00a7aNow playing as " + use.name;
                    clearAndInit();
                });
            } catch (Exception e) {
                client.execute(() -> status = "\u00a7c" + e.getMessage());
            }
        }, "AccountSwitch").start();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Current: " + client.getSession().getUsername()), width / 2, 26, 0xAAAAAA);
        if (AccountStore.accounts.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("No saved accounts yet"), width / 2, 80, 0x888888);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height - 70, 0xFFFFFF);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
