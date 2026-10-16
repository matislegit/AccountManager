package com.example.accountmanager.gui;

import com.example.accountmanager.auth.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;

public class OfflineAccountScreen extends Screen {
    private final Screen parent; // the AccountManagerScreen
    private TextFieldWidget field;
    private String status = "";

    public OfflineAccountScreen(Screen parent) {
        super(Text.literal("Offline Account"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 20, 200, 20, Text.literal("Username"));
        field.setMaxLength(16);
        field.setPlaceholder(Text.literal("Username"));
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> submit())
                .dimensions(width / 2 - 100, height / 2 + 8, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(width / 2 + 2, height / 2 + 8, 98, 20).build());
    }

    private void submit() {
        String name = field.getText().trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) { status = "\u00a7cName must be 3-16 chars (letters, numbers, _)"; return; }
        AccountStore.addOrReplace(new Account("OFFLINE", name,
                Uuids.getOfflinePlayerUuid(name).toString(), "", ""));
        close();
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
