package com.example.accountmanager.auth;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.Optional;
import java.util.UUID;

public final class AccountSwitcher {
    private AccountSwitcher() {}

    /** Applies the account to the running client. Call on the render thread. */
    public static void apply(Account a) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean online = !a.type.equals("OFFLINE");
        Session session = new Session(a.name, UUID.fromString(a.uuid),
                online ? a.accessToken : "0", Optional.empty(), Optional.empty());

        UserApiService api = UserApiService.OFFLINE;
        if (online) {
            try {
                api = new YggdrasilAuthenticationService(Proxy.NO_PROXY).createUserApiService(a.accessToken);
            } catch (Exception e) { e.printStackTrace(); }
        }

        setByType(mc, Session.class, session);
        setByType(mc, UserApiService.class, api);
        try {
            setByType(mc, ProfileKeys.class, ProfileKeys.create(api, session, mc.runDirectory.toPath()));
        } catch (Throwable t) { t.printStackTrace(); }
    }

    /** Finds the field by its type, so it doesn't depend on mapping names. */
    private static void setByType(MinecraftClient mc, Class<?> type, Object value) {
        try {
            for (Field f : MinecraftClient.class.getDeclaredFields()) {
                if (f.getType() == type && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    f.set(mc, value);
                    return;
                }
            }
            System.err.println("[AccountManager] No field of type " + type.getName());
        } catch (Throwable t) { t.printStackTrace(); }
    }
}
