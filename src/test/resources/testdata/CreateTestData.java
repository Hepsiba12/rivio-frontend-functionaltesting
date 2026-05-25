import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class CreateTestData {

    private static final String HEADER_BG = "FFBDD7EE";
    // Resolve relative to the current project so any teammate can regenerate.
    private static final String OUTPUT_DIR =
            System.getProperty("user.dir") + "/src/test/resources/testdata/";

    public static void main(String[] args) throws IOException {
        createLoginData();
        createEmployeeData();
        createLeaveData();
        createAttendanceData();
        createRecruitmentData();
        System.out.println("All 5 Excel files created successfully.");
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 0xFF, (byte) 0xBD, (byte) 0xD7, (byte) 0xEE}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle dataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private static void writeRow(Sheet sheet, int rowIdx, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i] == null ? "" : values[i]);
            cell.setCellStyle(style);
        }
    }

    private static void setColumnWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private static void createLoginData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle dStyle = dataStyle(wb);

        // Sheet 1: ValidLogin
        Sheet valid = wb.createSheet("ValidLogin");
        writeRow(valid, 0, hStyle, "email", "password", "role");
        writeRow(valid, 1, dStyle, "admin@rivio.com", "password", "SUPERADMIN");
        writeRow(valid, 2, dStyle, "hr@rivio.com", "password", "HR");
        writeRow(valid, 3, dStyle, "manager@rivio.com", "password", "MANAGER");
        writeRow(valid, 4, dStyle, "payroll@gmail.com", "password", "PAYROLL_MANAGER");
        writeRow(valid, 5, dStyle, "employee@rivio.com", "password", "EMPLOYEE");
        setColumnWidths(valid, 28, 16, 20);

        // Sheet 2: InvalidLogin
        Sheet invalid = wb.createSheet("InvalidLogin");
        writeRow(invalid, 0, hStyle, "email", "password", "expectedErrorMessage");
        writeRow(invalid, 1, dStyle, "admin@rivio.com", "wrongpass", "Invalid credentials");
        writeRow(invalid, 2, dStyle, "notexist@test.com", "password", "Invalid credentials");
        writeRow(invalid, 3, dStyle, "", "password", "");
        writeRow(invalid, 4, dStyle, "admin@rivio.com", "", "");
        writeRow(invalid, 5, dStyle, "notanemail", "password", "");
        setColumnWidths(invalid, 28, 16, 28);

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + "LoginData.xlsx")) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("Created: LoginData.xlsx");
    }

    private static void createEmployeeData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle dStyle = dataStyle(wb);

        // ── Sheet 1: EmployeeData (legacy directory dataset) ─────────────────
        Sheet sheet = wb.createSheet("EmployeeData");
        writeRow(sheet, 0, hStyle,
                "firstName", "lastName", "email", "phone", "dob",
                "gender", "department", "designation", "employmentType",
                "joinDate", "location", "salary");

        writeRow(sheet, 1, dStyle,
                "Alice", "Smith", "alice.smith@rivio.com", "9876543210", "1995-03-15",
                "Female", "Engineering", "Software Engineer", "FULL_TIME",
                "2025-01-01", "Mumbai", "80000");
        writeRow(sheet, 2, dStyle,
                "Bob", "Jones", "bob.jones@rivio.com", "9123456789", "1990-07-22",
                "Male", "HR", "HR Executive", "FULL_TIME",
                "2025-02-01", "Bangalore", "60000");
        writeRow(sheet, 3, dStyle,
                "Carol", "Davis", "carol.davis@rivio.com", "9988776655", "1993-11-05",
                "Female", "Finance", "Accountant", "FULL_TIME",
                "2025-03-01", "Chennai", "55000");

        setColumnWidths(sheet, 14, 14, 28, 16, 14, 10, 16, 22, 18, 14, 14, 10);

        // ── Sheet 2: AddEmployee (Admin > Add Employee onboarding dataset) ───
        // Columns map to the Onboard New Employee p-dialog (Rivio API §4.1).
        // expectedResult = PASS  → row must successfully submit
        // expectedResult = FAIL  → row must be rejected (validation / dup / blank)
        Sheet add = wb.createSheet("AddEmployee");
        writeRow(add, 0, hStyle,
                "testCaseId", "email", "password", "role",
                "firstName", "lastName", "employeeCode",
                "department", "designation", "location",
                "employmentType", "joiningDate", "expectedResult", "description");

        // Positive rows
        writeRow(add, 1, dStyle,
                "RV_ADD_EMP_01", "newuser01@rivio.com", "Password123!", "Employee",
                "Mitesh", "Paliwal", "EMP1001",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-01", "PASS",
                "Valid full-time engineering onboarding");
        writeRow(add, 2, dStyle,
                "RV_ADD_EMP_02", "newuser02@rivio.com", "Password123!", "Employee",
                "Aisha", "Verma", "EMP1002",
                "HR", "HR Executive", "Mumbai",
                "FULL_TIME", "2026-06-05", "PASS",
                "Valid HR onboarding in Mumbai");
        writeRow(add, 3, dStyle,
                "RV_ADD_EMP_03", "newuser03@rivio.com", "Password123!", "Manager",
                "Rahul", "Iyer", "EMP1003",
                "Engineering", "Engineering Manager", "Bengaluru HQ",
                "FULL_TIME", "2026-06-10", "PASS",
                "Manager-role onboarding");
        writeRow(add, 4, dStyle,
                "RV_ADD_EMP_04", "newuser04@rivio.com", "Password123!", "Employee",
                "Priya", "Nair", "EMP1004",
                "Finance", "Accountant", "Chennai",
                "PART_TIME", "2026-06-15", "PASS",
                "Part-time finance onboarding");

        // Negative rows (should be rejected by the modal validators)
        writeRow(add, 5, dStyle,
                "RV_ADD_EMP_05", "", "Password123!", "Employee",
                "NoEmail", "User", "EMP1005",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-20", "FAIL",
                "Empty email — submit must be disabled / form invalid");
        writeRow(add, 6, dStyle,
                "RV_ADD_EMP_06", "bad-email-format", "Password123!", "Employee",
                "Bad", "Email", "EMP1006",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-20", "FAIL",
                "Invalid email format");
        writeRow(add, 7, dStyle,
                "RV_ADD_EMP_07", "newuser07@rivio.com", "123", "Employee",
                "Weak", "Pass", "EMP1007",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-22", "FAIL",
                "Weak password (less than 8 chars)");
        writeRow(add, 8, dStyle,
                "RV_ADD_EMP_08", "newuser08@rivio.com", "Password123!", "Employee",
                "", "", "EMP1008",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-25", "FAIL",
                "Empty first & last name");
        writeRow(add, 9, dStyle,
                "RV_ADD_EMP_09", "newuser09@rivio.com", "Password123!", "Employee",
                "Dup", "Code", "EMP1001",
                "Engineering", "Backend Developer", "Bengaluru HQ",
                "FULL_TIME", "2026-06-28", "FAIL",
                "Duplicate employee code (collides with EMP_01)");

        setColumnWidths(add, 16, 28, 16, 14, 14, 14, 14, 18, 24, 18, 14, 14, 14, 42);

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + "EmployeeData.xlsx")) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("Created: EmployeeData.xlsx (sheets: EmployeeData, AddEmployee)");
    }

    private static void createLeaveData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle dStyle = dataStyle(wb);

        Sheet sheet = wb.createSheet("LeaveData");
        writeRow(sheet, 0, hStyle, "status", "fromDate", "toDate", "expectedCount");
        writeRow(sheet, 1, dStyle, "PENDING",  "2025-01-01", "2025-12-31", "");
        writeRow(sheet, 2, dStyle, "APPROVED", "2025-01-01", "2025-12-31", "");
        writeRow(sheet, 3, dStyle, "REJECTED", "2025-01-01", "2025-12-31", "");
        writeRow(sheet, 4, dStyle, "",          "2025-06-01", "2025-06-30", "");
        setColumnWidths(sheet, 16, 14, 14, 18);

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + "LeaveData.xlsx")) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("Created: LeaveData.xlsx");
    }

    private static void createAttendanceData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle dStyle = dataStyle(wb);

        Sheet sheet = wb.createSheet("AttendanceData");
        writeRow(sheet, 0, hStyle, "employeeName", "fromDate", "toDate", "expectedRecords");
        writeRow(sheet, 1, dStyle, "John", "2025-01-01", "2025-01-31", "");
        writeRow(sheet, 2, dStyle, "",     "2025-05-01", "2025-05-31", "");
        setColumnWidths(sheet, 18, 14, 14, 20);

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + "AttendanceData.xlsx")) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("Created: AttendanceData.xlsx");
    }

    private static void createRecruitmentData() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle dStyle = dataStyle(wb);

        Sheet sheet = wb.createSheet("RecruitmentData");
        writeRow(sheet, 0, hStyle, "searchKeyword", "expectedMinCount");
        writeRow(sheet, 1, dStyle, "Engineer", "1");
        writeRow(sheet, 2, dStyle, "Designer", "0");
        writeRow(sheet, 3, dStyle, "Manager",  "1");
        setColumnWidths(sheet, 20, 22);

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + "RecruitmentData.xlsx")) {
            wb.write(fos);
        }
        wb.close();
        System.out.println("Created: RecruitmentData.xlsx");
    }
}
