package com.example.accountmanager.auth;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.*;
import java.util.*;

public final class AccountStore {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("accountmanager.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<Account> accounts = new ArrayList<>();

    static { load(); }
    private AccountStore() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                List<Account> l = GSON.fromJson(Files.readString(FILE), new TypeToken<List<Account>>() {}.getType());
                accounts.clear();
                if (l != null) accounts.addAll(l);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(accounts)); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addOrReplace(Account a) {
        accounts.removeIf(x -> x.type.equals(a.type) && x.uuid.equals(a.uuid));
        accounts.add(a);
        save();
    }

    public static void remove(Account a) { accounts.remove(a); save(); }
}
