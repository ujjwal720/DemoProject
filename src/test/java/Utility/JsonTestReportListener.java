package Utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonTestReportListener implements ITestListener {

    // Represent each test result in required format
    private static class TestData {
        List<String> logs;
        String status;
        String testName;
        String screenshot;
        String description;
        long executionTimeMs;
        ErrorData error; // null unless failure
    }

    // Represent error structure
    private static class ErrorData {
        String type;
        String message;
        String stackTrace;
    }

    private final List<TestData> tests = new ArrayList<>();
    private long suiteStartTime;
    private long suiteEndTime;

    @Override
    public void onStart(ITestContext context) {
        suiteStartTime = System.currentTimeMillis();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        tests.add(createTestData(result, "PASSED"));
        LogCollector.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        tests.add(createTestData(result, "FAILED"));
        LogCollector.clear();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        tests.add(createTestData(result, "SKIPPED"));
        LogCollector.clear();
    }


    @Override
    public void onFinish(ITestContext context) {
        suiteEndTime = System.currentTimeMillis();

        // --- Summary block ---
        Map<String, Object> summary = new HashMap<>();
        summary.put("failed", tests.stream().filter(t -> "FAILED".equals(t.status)).count());
        summary.put("passed", tests.stream().filter(t -> "PASSED".equals(t.status)).count());
        summary.put("skipped", tests.stream().filter(t -> "SKIPPED".equals(t.status)).count());
        summary.put("totalTests", tests.size());
        summary.put("totalExecutionTimeMs", suiteEndTime - suiteStartTime);

        // --- Main report structure ---
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tests", tests);
        report.put("browser", System.getProperty("browser", "Chrome 128.0"));
        report.put("summary", summary);
        report.put("platform", System.getProperty("os.name", "Windows 11"));
        report.put("environment", "QA");

        // --- Format timestamps in PostgreSQL format (UTC) ---
        SimpleDateFormat pgTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX");
        pgTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));
        String executionTimestamp = pgTimestamp.format(new Date());
        report.put("executionDate", executionTimestamp);

        // --- Write JSON locally (optional) ---
        String folderTimestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String folderPath = "Test_Reports/" + folderTimestamp;
        new File(folderPath).mkdirs();
        try (FileWriter writer = new FileWriter(folderPath + "/test-results.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(report, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- Push JSON to Supabase ---
        try {
            // --- Load project properties ---
            String propertiesFilePath = System.getProperty("user.dir") + "/project.properties";
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propertiesFilePath)) {
                props.load(fis);
            }

            String projectId = props.getProperty("project.id");
            String userId = props.getProperty("user.id");
            String runid= props.getProperty("project.name")+"_"+folderTimestamp;

            if (projectId == null || userId == null) {
                throw new RuntimeException("project.id or user.id not found in project.properties!");
            }

            // --- Supabase URL and anon key ---
            String supabaseUrl = "https://lghzmijzfpvrcvogxpew.supabase.co/rest/v1/automation_results";
            String anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxnaHptaWp6ZnB2cmN2b2d4cGV3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTUwODYzNDQsImV4cCI6MjA3MDY2MjM0NH0.ySHdnHqIsq3ot0Cg7gyQvES6qZrN1TZSyZg4XoKaneE";

            // --- Convert report map to JSON ---
            Gson gson = new GsonBuilder().create();
            String reportJson = gson.toJson(report);

            // --- Build JSON payload ---
            String payload = "{"
                    + "\"id\":\"" + UUID.randomUUID() + "\","
                    + "\"run_id\":\"" + runid + "\","
                    + "\"json_result\":" + reportJson + ","
                    + "\"timestamp\":\"" + executionTimestamp + "\","
                    + "\"user_id\":\"" + userId + "\","
                    + "\"project_id\":\"" + projectId + "\","
                    + "\"created_at\":\"" + executionTimestamp + "\""
                    + "}";

            // --- Open HTTP connection ---
            java.net.URL url = new java.net.URL(supabaseUrl);
            System.out.println("Supabase URL: " + url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", anonKey);
            conn.setRequestProperty("Authorization", "Bearer " + anonKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // --- Send payload ---
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // --- Read response code ---
            int responseCode = conn.getResponseCode();
            System.out.println("Supabase insert response code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private TestData createTestData(ITestResult result, String status) {
        TestData data = new TestData();
        data.logs = LogCollector.getLogs();
        data.status = status;
        data.testName = result.getMethod().getMethodName();
        data.description = result.getMethod().getDescription();
        data.executionTimeMs = result.getEndMillis() - result.getStartMillis();

        Object screenshotAttr = result.getAttribute("screenshot");
        data.screenshot = screenshotAttr != null ? screenshotAttr.toString() : null;

        if ("FAILED".equals(status)) {
            Throwable t = result.getThrowable();
            if (t != null) {
                ErrorData err = new ErrorData();
                err.type = t.getClass().getSimpleName();
                err.message = t.getMessage();
                err.stackTrace = getStackTraceAsString(t);
                data.error = err;
            }
        }
        return data;
    }

    private String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append(el.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    // Unused methods
    @Override public void onTestStart(ITestResult result) {}
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onTestFailedWithTimeout(ITestResult result) {}
}
