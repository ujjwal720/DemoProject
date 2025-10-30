package Utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportsUtility {



    private static final String DIR_PATH = System.getProperty("user.dir") +
            File.separator + "Test_Reports";



    public static ExtentReports reportsInit(){

        String dayWiseFolderPath = DayWiseFolderUtility.getDayWiseFolderPath(DIR_PATH);
        String timestamp = new SimpleDateFormat("HHmmssSSS").format(new Date());
        String REPORT_PATH = dayWiseFolderPath + File.separator + "Report_" + timestamp + ".html";
        ExtentSparkReporter sparkReporter;
        ExtentReports reports;
        sparkReporter = new ExtentSparkReporter(REPORT_PATH);
        /*
        Meta data is used for passing the information
         */
        sparkReporter.config().setReportName("Ujjwal Shrivastava");
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("Test Demo");
        reports = new ExtentReports();
        reports.setSystemInfo("QA", "1.25");
        reports.attachReporter(sparkReporter);
        return reports;
    }

    public static ExtentTest createTest(ExtentReports reports, String testname){
        return reports.createTest(testname);
    }

}
