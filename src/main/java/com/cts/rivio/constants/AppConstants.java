package com.cts.rivio.constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AppConstants – central constants for Rivio HRMS test automation.
 *
 * URLs, role names, and credentials are sourced from:
 *   - Rivio_Angular-main/src/app/app.routes.ts  (route paths)
 *   - Rivio_Angular-main/src/app/core/layout/sidebar/sidebar.component.ts  (role names)
 *   - Rivio_HRMS_TestDesign.xlsx                (test data)
 */
public class AppConstants {

    private AppConstants() {}

    // ── Application URLs ──────────────────────────────────────────────────────
    public static final String BASE_URL          = "https://rivio-angular.vercel.app/";
    public static final String LOGIN_URL         = BASE_URL + "login";
    public static final String DASHBOARD_URL     = BASE_URL + "dashboard";
    public static final String EMPLOYEE_DIR_URL  = BASE_URL + "employees";
    public static final String LEAVE_URL         = BASE_URL + "leave";
    public static final String ATTENDANCE_URL    = BASE_URL + "attendance";
    public static final String PAYROLL_URL       = BASE_URL + "payroll";
    public static final String RECRUITMENT_URL   = BASE_URL + "ats";          // Angular route is /ats, not /recruitment
    public static final String JOB_BOARD_URL     = BASE_URL + "ats";          // same Kanban + Job Openings page
    public static final String COMPANY_URL       = BASE_URL + "company";
    public static final String ASK_RIVI_URL      = BASE_URL + "ask-rivi";


    // Self-service – Angular routes are /self-service/profile|attendance|leaves|payslips
    public static final String MY_PROFILE_URL    = BASE_URL + "self-service/profile";
    public static final String MY_ATTENDANCE_URL = BASE_URL + "self-service/attendance";
    public static final String MY_LEAVES_URL     = BASE_URL + "self-service/leaves";
    public static final String MY_PAYSLIPS_URL   = BASE_URL + "self-service/payslips";

    // ── Roles (must match strings used by Angular roleGuard – case & spaces matter) ──
    public static final String ROLE_SUPERADMIN      = "Super Admin";
    public static final String ROLE_HR              = "Hr";
    public static final String ROLE_MANAGER         = "Manager";
    public static final String ROLE_PAYROLL_MANAGER = "Payroll Manager";
    public static final String ROLE_EMPLOYEE        = "Employee";

    // ── Credentials – mirror the demo accounts on the live Vercel deployment ──
    public static final String ADMIN_EMAIL      = "admin@rivio.com";
    public static final String ADMIN_PASSWORD   = "password";
    public static final String HR_EMAIL         = "hr@rivio.com";
    public static final String HR_PASSWORD      = "password";
    public static final String MANAGER_EMAIL    = "manager@rivio.com";
    public static final String MANAGER_PASSWORD = "password";
    public static final String PAYROLL_EMAIL    = "payroll@rivio.com";
    public static final String PAYROLL_PASSWORD = "password";
    public static final String EMPLOYEE_EMAIL   = "employee@rivio.com";
    public static final String EMPLOYEE_PASSWORD = "password";

    // ── Wait Timeouts (seconds) ───────────────────────────────────────────────
    // NOTE: implicit wait kept at 0 — implicit + explicit waits combined produce
    // unpredictable timeouts. Selenium docs explicitly warn against mixing them.
    public static final int IMPLICIT_WAIT    = 0;
    public static final int EXPLICIT_WAIT    = 20;
    public static final int PAGE_LOAD_WAIT   = 60;
    public static final int ANGULAR_WAIT     = 15;
    public static final int POLLING_INTERVAL = 300; // ms

    // ── File Paths ────────────────────────────────────────────────────────────
    private static final String BASE_DIR = System.getProperty("user.dir");
    // Timestamp computed once at JVM startup → all parallel threads share one file per run,
    // but each fresh `mvn test` produces its own ExtentReport-<timestamp>.html (no overwrite).
    private static final String RUN_TIMESTAMP = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    public static final String CONFIG_PATH          = BASE_DIR + "/src/test/resources/config.properties";
    public static final String REPORT_DIR           = BASE_DIR + "/src/test/resources/test-output/reports/";
    public static final String REPORT_PATH          = REPORT_DIR + "ExtentReport-" + RUN_TIMESTAMP + ".html";
    public static final String SCREENSHOT_PATH      = BASE_DIR + "/src/test/resources/test-output/screenshots/";
    public static final String LOGIN_DATA_PATH      = BASE_DIR + "/src/test/resources/testdata/LoginData.xlsx";
    public static final String EMPLOYEE_DATA_PATH   = BASE_DIR + "/src/test/resources/testdata/EmployeeData.xlsx";
    public static final String LEAVE_DATA_PATH      = BASE_DIR + "/src/test/resources/testdata/LeaveData.xlsx";
    public static final String ATTENDANCE_DATA_PATH = BASE_DIR + "/src/test/resources/testdata/AttendanceData.xlsx";
    public static final String RECRUITMENT_DATA_PATH= BASE_DIR + "/src/test/resources/testdata/RecruitmentData.xlsx";
    public static final String FORM_TEST_DATA_PATH  = BASE_DIR + "/src/test/resources/testdata/FormTestData.xlsx";

    // ── Excel Sheet Names ─────────────────────────────────────────────────────
    public static final String SHEET_VALID_LOGIN   = "ValidLogin";
    public static final String SHEET_INVALID_LOGIN = "InvalidLogin";
    public static final String SHEET_EMPLOYEE      = "EmployeeData";
    public static final String SHEET_ADD_EMPLOYEE  = "AddEmployee";
    public static final String SHEET_LEAVE         = "LeaveData";
    public static final String SHEET_ATTENDANCE    = "AttendanceData";
    public static final String SHEET_RECRUITMENT   = "RecruitmentData";

    // ── FormTestData.xlsx sheet names (Leave / Payroll / Recruitment forms) ──
    public static final String SHEET_LEAVE_APPLY       = "LeaveApply";
    public static final String SHEET_SALARY_COMPONENT  = "SalaryComponent";
    public static final String SHEET_PAY_CYCLE         = "PayCycle";
    public static final String SHEET_JOB_OPENING       = "JobOpening";
    public static final String SHEET_SOURCED_CANDIDATE = "SourcedCandidate";

    // ── Browser ───────────────────────────────────────────────────────────────
    public static final String BROWSER_CHROME  = "chrome";
    public static final String BROWSER_FIREFOX = "firefox";
    public static final String BROWSER_EDGE    = "edge";

    // ── localStorage keys used by the Angular auth.state ─────────────────────
    // Tests can inject these to bypass the login UI in fast scenarios.
    public static final String LS_TOKEN    = "rivio_token";
    public static final String LS_NAME     = "rivio_user_name";
    public static final String LS_ROLE     = "rivio_role";
    public static final String LS_USER_ID  = "rivio_user_id";
    public static final String LS_EMP_ID   = "rivio_emp_id";
}
