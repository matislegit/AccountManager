package com.example.accountmanager.auth;

public class Account {
    public String type;          // MICROSOFT, OFFLINE, TOKEN
    public String name;
    public String uuid;          // dashed UUID
    public String accessToken;   // empty for offline
    public String refreshToken;  // Microsoft only (Live refresh token)

    public Account() {}
    public Account(String type, String name, String uuid, String accessToken, String refreshToken) {
        this.type = type; this.name = name; this.uuid = uuid;
        this.accessToken = accessToken; this.refreshToken = refreshToken;
    }
}
