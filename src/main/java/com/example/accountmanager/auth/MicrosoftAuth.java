package com.example.accountmanager.auth;

import com.google.gson.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/** Microsoft device-code login -> Xbox Live -> XSTS -> Minecraft services. */
public final class MicrosoftAuth {
    // Public client id used by the legacy Minecraft launcher flow. Swap in your own Azure app id if you prefer.
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public record DeviceCode(String userCode, String deviceCode, String uri, int interval, int expiresIn) {}

    private MicrosoftAuth() {}

    public static DeviceCode requestDeviceCode() throws Exception {
        JsonObject o = form("https://login.live.com/oauth20_connect.srf",
                Map.of("client_id", CLIENT_ID, "scope", SCOPE, "response_type", "device_code"));
        if (!o.has("user_code")) throw new Exception("Could not start login: " + o);
        return new DeviceCode(o.get("user_code").getAsString(), o.get("device_code").getAsString(),
                o.get("verification_uri").getAsString(), o.get("interval").getAsInt(), o.get("expires_in").getAsInt());
    }

    public static Account waitForLogin(DeviceCode dc, BooleanSupplier cancelled) throws Exception {
        long deadline = System.currentTimeMillis() + dc.expiresIn() * 1000L;
        int interval = Math.max(dc.interval(), 1);
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            if (cancelled.getAsBoolean()) throw new Exception("Cancelled");
            JsonObject o = form("https://login.live.com/oauth20_token.srf", Map.of(
                    "client_id", CLIENT_ID, "device_code", dc.deviceCode(),
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
            if (o.has("access_token")) {
                return finish(o.get("access_token").getAsString(), o.get("refresh_token").getAsString());
            }
            String err = o.has("error") ? o.get("error").getAsString() : "unknown";
            if (err.equals("slow_down")) interval += 5;
            else if (!err.equals("authorization_pending")) throw new Exception("Login failed: " + err);
        }
        throw new Exception("Code expired");
    }

    /** Gets a fresh Minecraft token for a saved Microsoft account. */
    public static Account refresh(Account a) throws Exception {
        JsonObject o = form("https://login.live.com/oauth20_token.srf", Map.of(
                "client_id", CLIENT_ID, "refresh_token", a.refreshToken,
                "grant_type", "refresh_token", "scope", SCOPE));
        if (!o.has("access_token")) throw new Exception("Session expired, please re-add this account");
        return finish(o.get("access_token").getAsString(),
                o.has("refresh_token") ? o.get("refresh_token").getAsString() : a.refreshToken);
    }

    private static Account finish(String liveToken, String refreshToken) throws Exception {
        // Xbox Live
        JsonObject xblBody = new JsonObject();
        JsonObject props = new JsonObject();
        props.addProperty("AuthMethod", "RPS");
        props.addProperty("SiteName", "user.auth.xboxlive.com");
        props.addProperty("RpsTicket", "t=" + liveToken);
        xblBody.add("Properties", props);
        xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblBody.addProperty("TokenType", "JWT");
        JsonObject xbl = json("https://user.auth.xboxlive.com/user/authenticate", xblBody, null);
        String xblToken = xbl.get("Token").getAsString();
        String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0)
                .getAsJsonObject().get("uhs").getAsString();

        // XSTS
        JsonObject xstsBody = new JsonObject();
        JsonObject p2 = new JsonObject();
        p2.addProperty("SandboxId", "RETAIL");
        JsonArray tokens = new JsonArray();
        tokens.add(xblToken);
        p2.add("UserTokens", tokens);
        xstsBody.add("Properties", p2);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        JsonObject xsts = json("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody, null);
        if (!xsts.has("Token")) throw new Exception("Xbox error (child account / no Xbox profile?): " + xsts);

        // Minecraft
        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xsts.get("Token").getAsString());
        JsonObject mc = json("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody, null);
        if (!mc.has("access_token")) throw new Exception("Minecraft auth failed: " + mc);
        String mcToken = mc.get("access_token").getAsString();

        Account a = profile(mcToken);
        a.type = "MICROSOFT";
        a.refreshToken = refreshToken;
        return a;
    }

    /** Looks up the profile for a Minecraft access token (also used to validate Token Login). */
    public static Account profile(String mcToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcToken).GET().build();
        JsonObject o = JsonParser.parseString(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
        if (!o.has("id")) throw new Exception("Invalid token or no Minecraft profile");
        String id = o.get("id").getAsString().replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
        return new Account("TOKEN", o.get("name").getAsString(), id, mcToken, "");
    }

    private static JsonObject form(String url, Map<String, String> params) throws Exception {
        String body = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return JsonParser.parseString(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
    }

    private static JsonObject json(String url, JsonObject body, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return JsonParser.parseString(HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
    }
}
