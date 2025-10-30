package Utility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DayWiseFolderUtility {

    public static String getDayWiseFolderPath(String baseDir) {
        String dateFolder = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String dayWiseFolderPath = baseDir + File.separator + dateFolder;
        File folder = new File(dayWiseFolderPath);
        if (!folder.exists()) {
            folder.mkdirs(); // Create the folder if it doesn't exist
        }
        return dayWiseFolderPath;
    }
}
