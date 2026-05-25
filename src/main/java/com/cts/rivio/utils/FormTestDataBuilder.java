package com.cts.rivio.utils;

import com.cts.rivio.constants.AppConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * FormTestDataBuilder — bootstraps FormTestData.xlsx with five sheets:
 *
 *   LeaveApply       – self-service leave apply form (Employee role)
 *   SalaryComponent  – payroll salary component form (Payroll Manager role)
 *   PayCycle         – initialize pay cycle form (Payroll Manager role)
 *   JobOpening       – create job requisition form (Admin role)
 *   SourcedCandidate – add sourced candidate form (Admin role)
 *
 * Idempotent: each sheet is only written when it is missing or stale.
 * {ts} placeholders are replaced at run-time with a millis-tail so
 * names / emails stay unique across repeated suite executions.
 */
public class FormTestDataBuilder {

    private FormTestDataBuilder() {}

    // ── Substitution helper ───────────────────────────────────────────────────

    public static String substitute(String value, String tsStamp) {
        if (value == null) return "";
        return value.replace("{ts}", tsStamp);
    }

    // ── Public entry point ────────────────────────────────────────────────────

    /** Ensure all five sheets exist in FormTestData.xlsx. Idempotent. */
    public static synchronized void ensureAllSheets() {
        String path = AppConstants.FORM_TEST_DATA_PATH;
        File file = new File(path);

        try {
            Workbook wb;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                wb = new XSSFWorkbook();
                buildAllSheets(wb);
                writeOrFail(wb, file);
                wb.close();
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                wb = new XSSFWorkbook(fis);
            }

            boolean dirty = false;
            dirty |= ensureSheet(wb, AppConstants.SHEET_LEAVE_APPLY,      LEAVE_HEADERS,      LEAVE_ROWS);
            dirty |= ensureSheet(wb, AppConstants.SHEET_SALARY_COMPONENT,  SALARY_HEADERS,     SALARY_ROWS);
            dirty |= ensureSheet(wb, AppConstants.SHEET_PAY_CYCLE,         CYCLE_HEADERS,      CYCLE_ROWS);
            dirty |= ensureSheet(wb, AppConstants.SHEET_JOB_OPENING,       JOB_HEADERS,        JOB_ROWS);
            dirty |= ensureSheet(wb, AppConstants.SHEET_SOURCED_CANDIDATE, CANDIDATE_HEADERS,  CANDIDATE_ROWS);

            if (dirty) {
                try {
                    writeOrFail(wb, file);
                } catch (IOException writeErr) {
                    if (isFileLocked(writeErr)) {
                        throw new RuntimeException(
                            "\n\n========================================================\n"
                          + "  FormTestData.xlsx is OPEN in Excel and cannot be\n"
                          + "  updated. Please CLOSE Excel and re-run the test.\n"
                          + "  File: " + file.getAbsolutePath() + "\n"
                          + "========================================================\n",
                            writeErr);
                    }
                    throw writeErr;
                }
            }
            wb.close();

        } catch (IOException e) {
            throw new RuntimeException("Failed to bootstrap FormTestData.xlsx at " + path, e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void buildAllSheets(Workbook wb) {
        populate(wb, AppConstants.SHEET_LEAVE_APPLY,      LEAVE_HEADERS,      LEAVE_ROWS);
        populate(wb, AppConstants.SHEET_SALARY_COMPONENT,  SALARY_HEADERS,     SALARY_ROWS);
        populate(wb, AppConstants.SHEET_PAY_CYCLE,         CYCLE_HEADERS,      CYCLE_ROWS);
        populate(wb, AppConstants.SHEET_JOB_OPENING,       JOB_HEADERS,        JOB_ROWS);
        populate(wb, AppConstants.SHEET_SOURCED_CANDIDATE, CANDIDATE_HEADERS,  CANDIDATE_ROWS);
    }

    /**
     * Returns true (dirty) if the sheet was written (missing or stale).
     * Currently only checks for sheet presence. A future version could
     * add a row-count check similar to AddEmployeeDataBuilder.looksCurrent().
     */
    private static boolean ensureSheet(Workbook wb, String sheetName,
                                        String[] headers, String[][] rows) {
        Sheet existing = wb.getSheet(sheetName);
        if (existing != null && existing.getLastRowNum() >= rows.length) {
            return false; // already present with the right number of rows
        }
        if (existing != null) {
            wb.removeSheetAt(wb.getSheetIndex(existing));
        }
        populate(wb, sheetName, headers, rows);
        return true;
    }

    private static void populate(Workbook wb, String sheetName,
                                  String[] headers, String[][] rows) {
        Sheet sheet = wb.createSheet(sheetName);

        CellStyle headerStyle = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row hRow = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = hRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                row.createCell(c).setCellValue(rows[r][c]);
            }
        }

        for (int c = 0; c < headers.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private static void writeOrFail(Workbook wb, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
    }

    private static boolean isFileLocked(IOException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("being used by another process")
            || lower.contains("the process cannot access the file")
            || lower.contains("sharing violation");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Sheet 1 – LeaveApply
    //   URL:  /self-service/leaves   Role: Employee
    //   Form: leaveTypeId (p-select) + dateRange (p-datepicker range)
    //   Submit button: "Submit Request"
    //
    //   Dates are Mon–Fri only (disabledDays=[0,6] blocks Sat/Sun).
    //   startDate / endDate format: YYYY-MM-DD
    // ═════════════════════════════════════════════════════════════════════════

    static final String[] LEAVE_HEADERS = {
        "testCaseId", "leaveType", "startDate", "endDate",
        "expectedResult", "description"
    };

    // Leave types as they actually appear in the DOM (from the live app HTML):
    //   "Sick Leave" | "Casual Leave" | "Earned Leave"
    static final String[][] LEAVE_ROWS = {
        // ── Positive ──────────────────────────────────────────────────────────
        {"RV_LVE_A_01", "Sick Leave", "2026-07-06", "2026-07-08",
         "PASS", "Positive — Sick Leave Mon–Wed (3 working days)"},
        {"RV_LVE_A_02", "Casual Leave", "2026-07-13", "2026-07-13",
         "PASS", "Positive — Casual Leave single Monday"},
        {"RV_LVE_A_03", "Earned Leave", "2026-07-20", "2026-07-22",
         "PASS", "Positive — Earned Leave Mon–Wed"},

        // ── Negative ──────────────────────────────────────────────────────────
        {"RV_LVE_A_04", "",            "2026-07-27", "2026-07-28",
         "FAIL", "Negative — no leave type selected (leaveTypeId required)"},
        {"RV_LVE_A_05", "Sick Leave",  "",           "",
         "FAIL", "Negative — no date range selected (dateRange required)"},
    };

    // ═════════════════════════════════════════════════════════════════════════
    // Sheet 2 – SalaryComponent
    //   URL:  /payroll  Role: Payroll Manager
    //   Form: employee select (ngModel p-select with filter), then:
    //         name (input), type (p-select), value (number input)
    //   Submit button: "Save"
    //
    //   componentType display labels (from payroll-dashboard.ts):
    //     "Earning (Adds to Gross Pay)"    → stored as EARNING
    //     "Deduction (Subtracts from Pay)" → stored as DEDUCTION
    //
    //   {ts} in componentName ensures each run creates a new unique component.
    // ═════════════════════════════════════════════════════════════════════════

    static final String[] SALARY_HEADERS = {
        "testCaseId", "employeeSearchText", "componentName",
        "componentType", "componentValue", "expectedResult", "description"
    };

    static final String[][] SALARY_ROWS = {
        // ── Positive ──────────────────────────────────────────────────────────
        {"RV_SAL_01", "employee", "Basic Pay {ts}",
         "Earning (Adds to Gross Pay)", "50000",
         "PASS", "Positive — Earning: Basic Pay"},
        {"RV_SAL_02", "employee", "PF Deduction {ts}",
         "Deduction (Subtracts from Pay)", "6000",
         "PASS", "Positive — Deduction: PF"},
        {"RV_SAL_03", "employee", "HRA {ts}",
         "Earning (Adds to Gross Pay)", "20000",
         "PASS", "Positive — Earning: HRA"},

        // ── Negative ──────────────────────────────────────────────────────────
        {"RV_SAL_04", "employee", "",
         "Earning (Adds to Gross Pay)", "10000",
         "FAIL", "Negative — empty component name (required)"},
        {"RV_SAL_05", "employee", "Transport {ts}",
         "", "5000",
         "FAIL", "Negative — no type selected (required)"},
        {"RV_SAL_06", "employee", "Insurance {ts}",
         "Earning (Adds to Gross Pay)", "0",
         "FAIL", "Negative — value=0 violates Validators.min(1)"},
    };

    // ═════════════════════════════════════════════════════════════════════════
    // Sheet 3 – PayCycle
    //   URL:  /payroll  Role: Payroll Manager
    //   Form: name (input) + dateRange (p-datepicker range, readonlyInput)
    //   Submit button: "Initialize"
    //
    //   {ts} in cycleName makes each run unique.
    // ═════════════════════════════════════════════════════════════════════════

    static final String[] CYCLE_HEADERS = {
        "testCaseId", "cycleName", "startDate", "endDate",
        "expectedResult", "description"
    };

    static final String[][] CYCLE_ROWS = {
        // ── Positive ──────────────────────────────────────────────────────────
        {"RV_CYC_01", "July 2026 Payroll {ts}", "2026-07-01", "2026-07-31",
         "PASS", "Positive — full month July cycle"},
        {"RV_CYC_02", "August 2026 Payroll {ts}", "2026-08-01", "2026-08-31",
         "PASS", "Positive — full month August cycle"},

        // ── Negative ──────────────────────────────────────────────────────────
        {"RV_CYC_03", "", "2026-09-01", "2026-09-30",
         "FAIL", "Negative — empty cycle name (required)"},
        {"RV_CYC_04", "October 2026 {ts}", "", "",
         "FAIL", "Negative — missing date range (required)"},
    };

    // ═════════════════════════════════════════════════════════════════════════
    // Sheet 4 – JobOpening
    //   URL:  /ats (Job Openings tab)  Role: Admin
    //   Form: title (input) + departmentId (p-select) + locationId (p-select)
    //   Submit button: "Create Requisition"
    //
    //   {ts} in title ensures uniqueness per run.
    // ═════════════════════════════════════════════════════════════════════════

    static final String[] JOB_HEADERS = {
        "testCaseId", "title", "department", "location",
        "expectedResult", "description"
    };

    static final String[][] JOB_ROWS = {
        // ── Positive ──────────────────────────────────────────────────────────
        {"RV_JOB_01", "Frontend Developer {ts}", "Engineering", "Bengaluru HQ",
         "PASS", "Positive — Engineering / Bengaluru HQ"},
        {"RV_JOB_02", "HR Analyst {ts}", "HR", "Mumbai",
         "PASS", "Positive — HR / Mumbai"},
        {"RV_JOB_03", "Sales Manager {ts}", "Sales", "Delhi",
         "PASS", "Positive — Sales / Delhi"},

        // ── Negative ──────────────────────────────────────────────────────────
        {"RV_JOB_04", "", "Engineering", "Bengaluru HQ",
         "FAIL", "Negative — empty title (required)"},
        {"RV_JOB_05", "QA Engineer {ts}", "", "Bengaluru HQ",
         "FAIL", "Negative — no department selected (required)"},
        {"RV_JOB_06", "DevOps Lead {ts}", "Engineering", "",
         "FAIL", "Negative — no location selected (required)"},
    };

    // ═════════════════════════════════════════════════════════════════════════
    // Sheet 5 – SourcedCandidate
    //   URL:  /ats (Pipeline / Kanban tab)  Role: Admin
    //   Form: jobOpeningId (p-select) + name (input) + email (input)
    //         + resumeUrl (optional input)
    //   Submit button: "Add Candidate"
    //
    //   jobOpeningTitle = "AUTO" → test picks the first open job from the panel.
    //   {ts} in email / candidateName ensures uniqueness per run.
    // ═════════════════════════════════════════════════════════════════════════

    static final String[] CANDIDATE_HEADERS = {
        "testCaseId", "jobOpeningTitle", "candidateName",
        "email", "resumeUrl", "expectedResult", "description"
    };

    static final String[][] CANDIDATE_ROWS = {
        // ── Positive ──────────────────────────────────────────────────────────
        {"RV_CND_01", "AUTO", "Alice Johnson {ts}",
         "alice.{ts}@test.com", "https://drive.google.com/resume-alice",
         "PASS", "Positive — full candidate with resume URL"},
        {"RV_CND_02", "AUTO", "Bob Smith {ts}",
         "bob.{ts}@test.com", "",
         "PASS", "Positive — candidate without resume URL"},
        {"RV_CND_03", "AUTO", "Charlie Brown {ts}",
         "charlie.{ts}@test.com", "https://linkedin.com/in/charlie",
         "PASS", "Positive — third candidate with LinkedIn"},

        // ── Negative ──────────────────────────────────────────────────────────
        {"RV_CND_04", "", "Dave Wilson {ts}",
         "dave.{ts}@test.com", "",
         "FAIL", "Negative — no job opening selected (jobOpeningId required)"},
        {"RV_CND_05", "AUTO", "",
         "eve.{ts}@test.com", "",
         "FAIL", "Negative — empty name (required)"},
        {"RV_CND_06", "AUTO", "Frank Lee {ts}",
         "not-a-valid-email", "",
         "FAIL", "Negative — malformed email (Validators.email)"},
        {"RV_CND_07", "AUTO", "Grace Kim {ts}",
         "", "",
         "FAIL", "Negative — empty email (required)"},
    };
}
