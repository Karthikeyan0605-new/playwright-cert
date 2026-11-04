# Playwright Java Certification Project

A Java + Maven + TestNG framework using [Playwright](https://playwright.dev/java/) for browser automation, covering TestMu AI's Selenium Playground scenarios locally across multiple browsers and remotely on TestMu AI's cloud grid.

## Stack

- Java 21
- Maven
- Playwright for Java 1.64.0
- TestNG 7.10.2

## Project structure

```
src/test/java/com/certification/base/   # BaseTest (browser/context/page lifecycle),
                                         # CapabilitiesBuilder (video/screenshot/console/network capture),
                                         # RemoteBrowserFactory (TestMu AI cloud grid connection),
                                         # SummaryListener (prints + saves the end-of-run summary)
src/test/java/com/certification/tests/  # Scenario test cases
testng.xml                              # The one suite: local cross-browser (Chromium, Firefox, Edge)
                                         # + TestMu AI remote grid (Windows 10/Chromium, macOS Sonoma/Firefox),
                                         # all 5 running in parallel
```

## One-time setup

Install Playwright's browser binaries (one-time, or after upgrading the Playwright version):

```bash
mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

## Running everything in one command

```bash
mvn test
```

This single command, no flags needed:
1. Runs all 3 scenario tests locally across Chromium, Firefox, and Edge, in parallel.
2. Runs the same 3 scenario tests on **TestMu AI's cloud grid** across Windows 10/Chromium and macOS Sonoma/Firefox, in parallel — only if `LT_USERNAME` and `LT_ACCESS_KEY` are set as environment variables. If they're not set, those two remote test groups are reported as **skipped** (not failed) — the build still succeeds and the local results are unaffected.
3. Prints a readable summary at the end: pass/fail/skipped counts per test group, every TestMu AI session ID produced, and how many screenshots/videos/console logs/network logs were captured. The same summary is also saved to `target/run-summary.txt` (overwritten each run), so you can reopen it later without rerunning anything.

### Running against TestMu AI's cloud grid

Set these two environment variables before running `mvn test` (never commit them, never put them in a file):

```bash
export LT_USERNAME="<your TestMu AI username>"
export LT_ACCESS_KEY="<your TestMu AI access key>"
mvn test
```

On Windows PowerShell:

```powershell
cd C:\Users\L129771\Documents\playwright-java-cert
$env:LT_USERNAME = "<your TestMu AI username>"
$env:LT_ACCESS_KEY = "<your TestMu AI access key>"
mvn test
```

Get your access key from your TestMu AI / LambdaTest account's Profile page.

## Artifacts

Every test run (local or remote) captures, under `target/test-artifacts/`:
- `screenshots/` — full-page screenshot after each test
- `videos/` — screen recording of each test
- `console-logs/` — browser console messages
- `network-logs/` — every request/response made during the test

## Notes

- `BaseTest` connects to a remote TestMu AI browser fresh per test method (so each test gets its own TestMu AI session), or launches a local browser once per test class, reusing it across that class's test methods.
- Local browsers run headless by default.
- If `LT_USERNAME`/`LT_ACCESS_KEY` aren't set, remote test methods throw TestNG's `SkipException` rather than failing — this is expected and by design.
