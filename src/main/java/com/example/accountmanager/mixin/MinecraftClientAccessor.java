package com.example.accountmanager.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Mutable @Accessor("session")
    void am$setSession(Session session);

    @Mutable @Accessor("userApiService")
    void am$setUserApiService(UserApiService service);

    @Mutable @Accessor("profileKeys")
    void am$setProfileKeys(ProfileKeys keys);
}
