package Utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class ExcelUtils {
    public XSSFWorkbook workbook;
    public XSSFSheet sheet;
    public File files;

    /*
    Constructor to intlize the things in excel
     */
    public ExcelUtils(String file) throws IOException {

        files = new File(file);
        FileInputStream input = new FileInputStream(files);
        workbook = new XSSFWorkbook(input);
        sheet = workbook.getSheetAt(0);
        input.close();


    }

    public int getnorows() throws IOException {
        int rows = sheet.getLastRowNum();
        return rows + 1;
    }


    public int getlastcell() throws IOException {
        int y = sheet.getRow(1).getLastCellNum();
        return y;

    }


    public String readExcelData(int index, int index1) throws IOException {
        int row = getnorows() + 1;
        int col = getlastcell();

        Cell b = sheet.getRow(index).getCell(index1);

        switch (b.getCellType()) {

            case STRING:
                return b.toString();

            case NUMERIC:
                DecimalFormat df = new DecimalFormat("0");
                return df.format(b.getNumericCellValue());

            default:
                return null;


        }
    }

}
