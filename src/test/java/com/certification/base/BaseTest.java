package com.certification.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BaseTest {

    private final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<List<String>> consoleLogsThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<List<String>> networkLogsThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<String> remoteTestLabelThreadLocal = new ThreadLocal<>();

    private final List<Playwright> launchedPlaywrights = new CopyOnWriteArrayList<>();
    private final List<Browser> launchedBrowsers = new CopyOnWriteArrayList<>();

    private volatile String browserName = "edge";
    private volatile String remotePlatform = "";

    @Parameters({"browser", "platform"})
    @BeforeClass
    public void setBrowserName(@Optional("edge") String browserName, @Optional("") String platform) {
        this.browserName = browserName;
        this.remotePlatform = platform;
    }

    @BeforeMethod
    public void createContextAndPage(Method testMethod) {
        if (!remotePlatform.isBlank()) {
            if (!RemoteBrowserFactory.hasCredentials()) {
                throw new SkipException("TestMu AI credentials not set (LT_USERNAME/LT_ACCESS_KEY) - skipping: "
                        + browserName + "-" + remotePlatform + "-" + testMethod.getName());
            }
            String testLabel = browserName + "-" + remotePlatform + "-" + testMethod.getName();
            remoteTestLabelThreadLocal.set(testLabel);
            launchRemoteBrowser(testLabel);
        } else if (browserThreadLocal.get() == null) {
            launchLocalBrowser(browserName);
        }

        BrowserContext context = browserThreadLocal.get().newContext(CapabilitiesBuilder.newContextOptions());
        contextThreadLocal.set(context);

        Page page = context.newPage();
        pageThreadLocal.set(page);
        consoleLogsThreadLocal.set(CapabilitiesBuilder.captureConsoleLogs(page));
        networkLogsThreadLocal.set(CapabilitiesBuilder.captureNetworkLogs(page));
    }

    private void launchRemoteBrowser(String testLabel) {
        Playwright playwright = Playwright.create();
        playwrightThreadLocal.set(playwright);

        String wsEndpoint = RemoteBrowserFactory.buildWsEndpoint(browserName, remotePlatform, testLabel);
        Browser browser = playwright.chromium().connect(wsEndpoint);
        browserThreadLocal.set(browser);
    }

    private void launchLocalBrowser(String browserName) {
        Playwright playwright = Playwright.create();
        playwrightThreadLocal.set(playwright);
        launchedPlaywrights.add(playwright);

        BrowserType browserType;
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
        switch (browserName.toLowerCase()) {
            case "firefox":
                browserType = playwright.firefox();
                break;
            case "edge":
                browserType = playwright.chromium();
                launchOptions.setChannel("msedge");
                break;
            case "chrome":
                browserType = playwright.chromium();
                launchOptions.setChannel("chrome");
                break;
            default:
                browserType = playwright.chromium();
        }

        Browser browser = browserType.launch(launchOptions);
        browserThreadLocal.set(browser);
        launchedBrowsers.add(browser);
    }

    protected Page getPage() {
        return pageThreadLocal.get();
    }

    @AfterMethod(alwaysRun = true)
    public void closeContext(ITestResult result) {
        Page page = pageThreadLocal.get();
        BrowserContext context = contextThreadLocal.get();
        if (page == null || context == null) {
            return;
        }

        String testName = browserName + "-" + result.getMethod().getMethodName();

        page.screenshot(new Page.ScreenshotOptions()
                .setPath(CapabilitiesBuilder.screenshotDir().resolve(testName + ".png"))
                .setFullPage(true));

        writeLogFile(CapabilitiesBuilder.consoleLogDir().resolve(testName + ".log"), consoleLogsThreadLocal.get());
        writeLogFile(CapabilitiesBuilder.networkLogDir().resolve(testName + ".log"), networkLogsThreadLocal.get());

        context.close();

        String testLabel = remoteTestLabelThreadLocal.get();
        if (testLabel != null) {
            Browser browser = browserThreadLocal.get();
            Playwright playwright = playwrightThreadLocal.get();
            browser.close();
            playwright.close();
            browserThreadLocal.remove();
            playwrightThreadLocal.remove();
            remoteTestLabelThreadLocal.remove();

            String sessionId = RemoteBrowserFactory.fetchSessionId(testLabel);
            String sessionLine = "TestMu AI session: " + testLabel + " -> " + sessionId;
            System.out.println(sessionLine);
            RemoteBrowserFactory.sessionIds.add(sessionLine);
        }
    }

    private void writeLogFile(Path path, List<String> lines) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log file: " + path, e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        for (Browser browser : launchedBrowsers) {
            browser.close();
        }
        launchedBrowsers.clear();
        for (Playwright playwright : launchedPlaywrights) {
            playwright.close();
        }
        launchedPlaywrights.clear();
    }
}
