package com.certification.base;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteBrowserFactory {

    private static final String WS_ENDPOINT_BASE = "wss://cdp.lambdatest.com/playwright";
    private static final String SESSIONS_API_BASE = "https://api.lambdatest.com/automation/api/v1/sessions";
    public static final String BUILD_NAME = "Playwright Java Cert";

    public static final List<String> sessionIds = new CopyOnWriteArrayList<>();

    public static boolean hasCredentials() {
        String username = System.getenv("LT_USERNAME");
        String accessKey = System.getenv("LT_ACCESS_KEY");
        return username != null && !username.isBlank() && accessKey != null && !accessKey.isBlank();
    }

    public static String buildWsEndpoint(String browserName, String platform, String testLabel) {
        String username = requireUsername();
        String accessKey = requireAccessKey();

        String capabilitiesJson = "{"
                + "\"browserName\":\"" + browserName + "\","
                + "\"browserVersion\":\"latest\","
                + "\"LT:Options\":{"
                + "\"platform\":\"" + platform + "\","
                + "\"build\":\"" + BUILD_NAME + "\","
                + "\"name\":\"" + testLabel + "\","
                + "\"user\":\"" + username + "\","
                + "\"accessKey\":\"" + accessKey + "\","
                + "\"network\":true,"
                + "\"video\":true,"
                + "\"console\":true"
                + "}"
                + "}";

        return WS_ENDPOINT_BASE + "?capabilities=" + URLEncoder.encode(capabilitiesJson, StandardCharsets.UTF_8);
    }

    public static String fetchSessionId(String testLabel) {
        String username = requireUsername();
        String accessKey = requireAccessKey();

        String auth = Base64.getEncoder().encodeToString((username + ":" + accessKey).getBytes(StandardCharsets.UTF_8));
        String url = SESSIONS_API_BASE + "?build_name=" + URLEncoder.encode(BUILD_NAME, StandardCharsets.UTF_8);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return extractSessionId(response.body(), testLabel);
        } catch (Exception e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private static String extractSessionId(String json, String testLabel) {
        String escapedLabel = Pattern.quote("\"name\":\"" + testLabel + "\"");
        Matcher nameMatcher = Pattern.compile(escapedLabel).matcher(json);
        if (!nameMatcher.find()) {
            return "not found";
        }

        int searchEnd = Math.min(json.length(), nameMatcher.end() + 500);
        String window = json.substring(nameMatcher.end(), searchEnd);

        Matcher sessionIdMatcher = Pattern.compile("\"session_id\":\"([^\"]+)\"").matcher(window);
        return sessionIdMatcher.find() ? sessionIdMatcher.group(1) : "not found";
    }

    private static String requireUsername() {
        String username = System.getenv("LT_USERNAME");
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "LT_USERNAME environment variable must be set to run against the TestMu AI grid.");
        }
        return username;
    }

    private static String requireAccessKey() {
        String accessKey = System.getenv("LT_ACCESS_KEY");
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                    "LT_ACCESS_KEY environment variable must be set to run against the TestMu AI grid.");
        }
        return accessKey;
    }
}
