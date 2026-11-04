package com.certification.base;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SummaryListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        List<String> lines = new ArrayList<>();
        lines.add("=================== SUMMARY ===================");

        Map<String, ISuiteResult> results = suite.getResults();
        for (Map.Entry<String, ISuiteResult> entry : results.entrySet()) {
            ITestContext context = entry.getValue().getTestContext();
            int passed = context.getPassedTests().size();
            int failed = context.getFailedTests().size();
            int skipped = context.getSkippedTests().size();
            lines.add(entry.getKey() + ": passed=" + passed + ", failed=" + failed + ", skipped=" + skipped);
        }

        lines.add("TestMu AI session IDs:");
        if (RemoteBrowserFactory.sessionIds.isEmpty()) {
            lines.add("  (none captured)");
        } else {
            RemoteBrowserFactory.sessionIds.forEach(line -> lines.add("  " + line));
        }

        lines.add("Artifacts (screenshots, videos, console/network logs):");
        lines.add(countFiles("screenshots", CapabilitiesBuilder.screenshotDir()));
        lines.add(countFiles("videos", CapabilitiesBuilder.videoDir()));
        lines.add(countFiles("console-logs", CapabilitiesBuilder.consoleLogDir()));
        lines.add(countFiles("network-logs", CapabilitiesBuilder.networkLogDir()));

        lines.add("==================================================");

        lines.forEach(System.out::println);

        try {
            Path summaryFile = Path.of("target", "run-summary.txt");
            Files.createDirectories(summaryFile.getParent());
            Files.write(summaryFile, lines);
        } catch (IOException e) {
            System.out.println("Failed to write run summary: " + e.getMessage());
        }
    }

    private String countFiles(String label, Path dir) {
        if (!Files.exists(dir)) {
            return "  " + label + ": 0 file(s) in " + dir;
        }
        try (var stream = Files.list(dir)) {
            long count = stream.filter(Files::isRegularFile).count();
            return "  " + label + ": " + count + " file(s) in " + dir;
        } catch (IOException e) {
            return "  " + label + ": unavailable (" + e.getMessage() + ")";
        }
    }
}
