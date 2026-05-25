package com.cts.rivio.utils;

import com.cts.rivio.constants.AppConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * AddEmployeeDataBuilder — bootstraps the "AddEmployee" sheet inside
 * EmployeeData.xlsx so the DataProvider-driven Admin > Add Employee
 * test can run on a fresh checkout without anyone manually
 * re-running CreateTestData.java.
 *
 * Idempotent: if the sheet already exists the call is a no-op.
 */
public class AddEmployeeDataBuilder {

    private AddEmployeeDataBuilder() {}

    public static final String SHEET_NAME = AppConstants.SHEET_ADD_EMPLOYEE;

    private static final String[] HEADERS = {
            "testCaseId", "email", "password", "role",
            "firstName", "lastName", "employeeCode",
            "department", "designation", "location",
            "employmentType", "joiningDate", "expectedResult", "description"
    };

    // 5 positive (different dept/designation/location/employmentType/role combos)
    // + 9 negative — one per unique validator / required-field combination.
    //
    // {ts} is substituted at run-time with a millis-tail so emails and employee
    // codes stay unique across repeated suite executions.
    private static final String[][] ROWS = {
            // ── Positive flavors ───────────────────────────────────────────────
            {"RV_ADD_EMP_01", "eng.dev.{ts}@rivio.com", "Password123!", "Employee",
             "Mitesh", "Paliwal", "EMP{ts}1",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-01", "PASS",
             "Flavor 1 — Engineering / Backend / Bengaluru / Full-time"},
            {"RV_ADD_EMP_02", "hr.exec.{ts}@rivio.com", "Password123!", "Employee",
             "Aisha", "Verma", "EMP{ts}2",
             "HR", "HR Executive", "Mumbai",
             "Full Time", "2026-06-05", "PASS",
             "Flavor 2 — HR / Executive / Mumbai / Full-time"},
            {"RV_ADD_EMP_03", "eng.mgr.{ts}@rivio.com", "Password123!", "Manager",
             "Rahul", "Iyer", "EMP{ts}3",
             "Engineering", "Engineering Manager", "Bengaluru HQ",
             "Full Time", "2026-06-10", "PASS",
             "Flavor 3 — Manager role / Engineering"},
            {"RV_ADD_EMP_04", "fin.pt.{ts}@rivio.com", "Password123!", "Employee",
             "Priya", "Nair", "EMP{ts}4",
             "Finance", "Payroll Specialist", "Chennai HQ",
             "Part Time", "2026-06-15", "PASS",
             "Flavor 4 — Finance / Payroll Specialist / Chennai HQ / Part-time"},
            {"RV_ADD_EMP_05", "sales.con.{ts}@rivio.com", "Password123!", "Employee",
             "Karan", "Mehta", "EMP{ts}5",
             "Sales", "Sales Executive", "Delhi",
             "Contract", "2026-06-18", "PASS",
             "Flavor 5 — Sales / Contract / Delhi"},

            // ── Negative flavors — one per required-field or validator ─────────
            {"RV_ADD_EMP_06", "", "Password123!", "Employee",
             "NoEmail", "User", "EMP{ts}6",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-20", "FAIL",
             "Negative — empty email (Validators.required)"},
            {"RV_ADD_EMP_07", "bad-email-format", "Password123!", "Employee",
             "Bad", "Email", "EMP{ts}7",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-22", "FAIL",
             "Negative — malformed email (Validators.email)"},
            {"RV_ADD_EMP_08", "noname.{ts}@rivio.com", "Password123!", "Employee",
             "", "", "EMP{ts}8",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-25", "FAIL",
             "Negative — empty firstName + lastName (both Validators.required)"},
            {"RV_ADD_EMP_09", "no.pwd.{ts}@rivio.com", "", "Employee",
             "NoPwd", "User", "EMP{ts}9",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-26", "FAIL",
             "Negative — empty password (Validators.required)"},
            {"RV_ADD_EMP_10", "short.pwd.{ts}@rivio.com", "Ab12!", "Employee",
             "Short", "Pwd", "EMP{ts}A",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-27", "FAIL",
             "Negative — password 5 chars, minLength(6) violation"},
            {"RV_ADD_EMP_11", "no.code.{ts}@rivio.com", "Password123!", "Employee",
             "NoCode", "User", "",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-28", "FAIL",
             "Negative — empty employeeCode (Validators.required)"},
            {"RV_ADD_EMP_12", "no.role.{ts}@rivio.com", "Password123!", "",
             "NoRole", "User", "EMP{ts}B",
             "Engineering", "Backend Developer", "Bengaluru HQ",
             "Full Time", "2026-06-29", "FAIL",
             "Negative — no role selected (roleId Validators.required)"},
            {"RV_ADD_EMP_13", "no.dept.{ts}@rivio.com", "Password123!", "Employee",
             "NoDept", "User", "EMP{ts}C",
             "", "", "Bengaluru HQ",
             "Full Time", "2026-06-30", "FAIL",
             "Negative — no department (departmentId + designationId required)"},
            {"RV_ADD_EMP_14", "no.loc.{ts}@rivio.com", "Password123!", "Employee",
             "NoLoc", "User", "EMP{ts}D",
             "Engineering", "Backend Developer", "",
             "Full Time", "2026-07-01", "FAIL",
             "Negative — no location selected (locationId Validators.required)"},
    };

    /**
     * Make sure EmployeeData.xlsx has an "AddEmployee" sheet populated
     * with the CURRENT canonical dataset. Safe to call before every test class.
     *
     * Rewrites the sheet whenever:
     *   - the file or sheet is missing, OR
     *   - the existing sheet has no `{ts}` placeholder in column B (email),
     *     which means it was written by an older version of this builder.
     *
     * Lock-tolerant: if the file is currently open in Excel (Windows holds a
     * write lock), we log a warning and keep using whatever is already on disk
     * instead of throwing. The test will still run as long as the sheet
     * exists. Only if the sheet is completely missing AND the file is locked
     * do we fail — and then with a clear, actionable error message.
     */
    public static synchronized void ensureSheet() {
        String path = AppConstants.EMPLOYEE_DATA_PATH;
        File file = new File(path);

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try (Workbook wb = new XSSFWorkbook()) {
                    populateSheet(wb);
                    writeOrFail(wb, file);
                }
                return;
            }

            // Read first; decide whether to rewrite based on schema check.
            Workbook wb;
            try (FileInputStream fis = new FileInputStream(file)) {
                wb = new XSSFWorkbook(fis);
            }

            try (Workbook open = wb) {
                Sheet existing = open.getSheet(SHEET_NAME);
                boolean missing = existing == null;
                boolean stale   = !missing && !looksCurrent(existing);

                if (!missing && !stale) {
                    return; // Sheet present and current — nothing to write.
                }

                if (stale) {
                    open.removeSheetAt(open.getSheetIndex(existing));
                }
                populateSheet(open);

                try {
                    writeOrFail(open, file);
                } catch (IOException writeErr) {
                    if (isFileLocked(writeErr)) {
                        // Schema needs upgrading but the file is locked.
                        // Falling back to stale data would cause confusing
                        // test failures (e.g. "employmentType wanted FULL_TIME"
                        // when the code expects "Full Time"), so fail loudly
                        // with a clear, actionable message.
                        throw new RuntimeException(
                            "\n\n========================================================\n"
                          + "  EmployeeData.xlsx is OPEN in Excel and cannot be\n"
                          + "  upgraded to the current schema.\n\n"
                          + "  → Please CLOSE Excel and re-run the test.\n\n"
                          + "  File: " + file.getAbsolutePath() + "\n"
                          + "========================================================\n",
                            writeErr);
                    }
                    throw writeErr;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to bootstrap AddEmployee sheet at " + path, e);
        }
    }

    /** Write workbook to file. Surfaces IOException so caller can branch on lock. */
    private static void writeOrFail(Workbook wb, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
    }

    /**
     * True for the canonical Windows "file in use" error. We don't want to
     * suppress real IO errors (disk full, permissions, etc.) — only the
     * "Excel has the file open" case.
     */
    private static boolean isFileLocked(IOException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("being used by another process")
            || lower.contains("the process cannot access the file")
            || lower.contains("sharing violation");
    }

    /**
     * True if the sheet uses the CURRENT schema AND has the correct data:
     *   - Row count >= ROWS.length (header excluded)
     *   - Email column carries the `{ts}` placeholder (added in v2)
     *   - employmentType carries the display label "Full Time" not "FULL_TIME" (v3+)
     *   - Row 4 (Finance) does NOT carry the old designation "Accountant" (fixed in v4)
     */
    private static boolean looksCurrent(Sheet sheet) {
        // getLastRowNum() is 0-based index of the last row. With 1 header + N data
        // rows the value is N. If fewer rows than ROWS.length, the sheet is stale.
        if (sheet.getLastRowNum() < ROWS.length) return false;

        Row firstData = sheet.getRow(1);
        if (firstData == null) return false;

        Cell email = firstData.getCell(1);
        if (email == null) return false;
        String emailVal = email.toString();
        if (emailVal == null || !emailVal.contains("{ts}")) return false;

        // Column index 10 = employmentType (see HEADERS).
        Cell empType = firstData.getCell(10);
        if (empType == null) return false;
        String etVal = empType.toString();
        if (etVal == null) return false;
        // v2 sheets stored "FULL_TIME"; current stores "Full Time".
        if (etVal.contains("_")) return false;

        // v4: row 4 (0-based index 4 = header + rows 01-04) carries the corrected
        // Finance designation "Payroll Specialist". Old sheets had "Accountant".
        // Column 8 = designation (see HEADERS array).
        Row row4 = sheet.getRow(4);
        if (row4 == null) return false;
        Cell desig = row4.getCell(8);
        if (desig == null) return false;
        if ("Accountant".equals(desig.toString())) return false; // stale v3 data

        return true;
    }

    /**
     * Substitute the {ts} placeholder in a value with the supplied stamp.
     * Used by the DataProvider so each run gets unique email/employeeCode.
     */
    public static String substitute(String value, String tsStamp) {
        if (value == null) return "";
        return value.replace("{ts}", tsStamp);
    }

    private static void populateSheet(Workbook wb) {
        Sheet sheet = wb.createSheet(SHEET_NAME);

        CellStyle header = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        header.setFont(bold);
        header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row hRow = sheet.createRow(0);
        for (int c = 0; c < HEADERS.length; c++) {
            Cell cell = hRow.createCell(c);
            cell.setCellValue(HEADERS[c]);
            cell.setCellStyle(header);
        }

        for (int r = 0; r < ROWS.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < ROWS[r].length; c++) {
                row.createCell(c).setCellValue(ROWS[r][c]);
            }
        }

        for (int c = 0; c < HEADERS.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }
}
