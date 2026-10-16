package com.example.accountmanager.auth;

import com.example.accountmanager.mixin.MinecraftClientAccessor;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

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

        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        acc.am$setSession(session);
        acc.am$setUserApiService(api);
        acc.am$setProfileKeys(ProfileKeys.create(api, session, mc.runDirectory.toPath()));
    }
}
