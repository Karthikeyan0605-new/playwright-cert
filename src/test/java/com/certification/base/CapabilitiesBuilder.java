package com.certification.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CapabilitiesBuilder {

    private static final Path ARTIFACTS_DIR = Paths.get("target", "test-artifacts");
    private static final Path VIDEO_DIR = ARTIFACTS_DIR.resolve("videos");
    private static final Path SCREENSHOT_DIR = ARTIFACTS_DIR.resolve("screenshots");
    private static final Path NETWORK_LOG_DIR = ARTIFACTS_DIR.resolve("network-logs");
    private static final Path CONSOLE_LOG_DIR = ARTIFACTS_DIR.resolve("console-logs");

    public static Browser.NewContextOptions newContextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setRecordVideoDir(VIDEO_DIR)
                .setRecordVideoSize(1280, 720);
    }

    public static List<String> captureConsoleLogs(Page page) {
        List<String> logs = new CopyOnWriteArrayList<>();
        page.onConsoleMessage(msg -> logs.add("[" + msg.type().toUpperCase() + "] " + msg.text()));
        return logs;
    }

    public static List<String> captureNetworkLogs(Page page) {
        List<String> logs = new CopyOnWriteArrayList<>();
        page.onRequest(request -> logs.add("REQUEST  " + request.method() + " " + request.url()));
        page.onResponse(response -> logs.add("RESPONSE " + response.status() + " " + response.url()));
        return logs;
    }

    public static Path videoDir() {
        return VIDEO_DIR;
    }

    public static Path screenshotDir() {
        return SCREENSHOT_DIR;
    }

    public static Path networkLogDir() {
        return NETWORK_LOG_DIR;
    }

    public static Path consoleLogDir() {
        return CONSOLE_LOG_DIR;
    }
}
