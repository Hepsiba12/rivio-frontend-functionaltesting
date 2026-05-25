package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.PayrollFormPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.FormTestDataBuilder;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * PayrollFormTest – data-driven tests for the two POST-method dialogs
 * on /payroll (Payroll Management, Payroll Manager role).
 *
 * ── Salary Component tests ────────────────────────────────────────────────────
 * Reads FormTestData.xlsx > SalaryComponent. Each row:
 *   testCaseId | employeeSearchText | componentName | componentType
 *              | componentValue | expectedResult | description
 *
 *   The "Add Component" button is enabled only after selecting an employee
 *   from the ngModel p-select at the top of the Salary tab.
 *
 *   componentType display labels (from payroll-dashboard.ts componentTypes array):
 *     "Earning (Adds to Gross Pay)"    → stored as EARNING
 *     "Deduction (Subtracts from Pay)" → stored as DEDUCTION
 *
 * ── Pay Cycle tests ───────────────────────────────────────────────────────────
 * Reads FormTestData.xlsx > PayCycle. Each row:
 *   testCaseId | cycleName | startDate | endDate | expectedResult | description
 *
 *   Cycle name with {ts} → unique per run.
 *   Date range is a p-datepicker range with readonlyInput=true (no sendKeys to
 *   the input; must click calendar day cells).
 */
public class PayrollFormTest extends BaseTest {

    @Override protected String getRole() { return ROLE_PAYROLL; }

    private PayrollFormPage payrollForm;

    @BeforeClass(alwaysRun = true)
    public void bootstrapTestData() {
        FormTestDataBuilder.ensureAllSheets();
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToPayroll() {
        driver.get(AppConstants.PAYROLL_URL);
        WaitUtils.waitForAngularLoad(driver);
        payrollForm = new PayrollFormPage(driver);

        // Close any stale modal from a previous test
        if (payrollForm.isComponentModalOpen()) {
            payrollForm.closeComponentModal();
            WaitUtils.hardWait(300);
        }
        if (payrollForm.isCycleModalOpen()) {
            payrollForm.closeCycleModal();
            WaitUtils.hardWait(300);
        }
    }

    // ── DataProviders ─────────────────────────────────────────────────────────

    @DataProvider(name = "salaryComponentRows")
    public Object[][] salaryComponentRows() {
        List<Map<String, String>> rows = ExcelUtils.readDataAsMapList(
                AppConstants.FORM_TEST_DATA_PATH,
                AppConstants.SHEET_SALARY_COMPONENT);

        // Single ts stamp per suite run — ensures unique componentName values
        String ts = String.valueOf(System.currentTimeMillis() % 100000);

        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            for (Map.Entry<String, String> e : row.entrySet()) {
                e.setValue(FormTestDataBuilder.substitute(e.getValue(), ts));
            }
            data[i][0] = row;
        }
        return data;
    }

    @DataProvider(name = "payCycleRows")
    public Object[][] payCycleRows() {
        List<Map<String, String>> rows = ExcelUtils.readDataAsMapList(
                AppConstants.FORM_TEST_DATA_PATH,
                AppConstants.SHEET_PAY_CYCLE);

        String ts = String.valueOf(System.currentTimeMillis() % 100000);

        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            for (Map.Entry<String, String> e : row.entrySet()) {
                e.setValue(FormTestDataBuilder.substitute(e.getValue(), ts));
            }
            data[i][0] = row;
        }
        return data;
    }

    // ── Smoke tests ───────────────────────────────────────────────────────────

    /**
     * RV_PAY_F_001 – Payroll Management page loads for Payroll Manager role.
     */
    @Test(priority = 1, groups = {"smoke", "regression"},
          description = "RV_PAY_F_001 – Payroll Management page loads")
    public void RV_PAY_F_001_pageLoads() {
        boolean loaded = WaitUtils.waitForH1Text(driver, "Payroll Management", 15);
        Assert.assertTrue(loaded, "Payroll Management page should load");
        ExtentManager.getTest().pass("Payroll Management page loaded successfully");
    }

    /**
     * RV_PAY_F_002 – Add Component button is disabled when no employee selected.
     */
    @Test(priority = 2, groups = {"regression"},
          description = "RV_PAY_F_002 – Add Component button disabled before employee selection")
    public void RV_PAY_F_002_addComponentDisabledWithoutEmployee() {
        payrollForm.openSalaryTab();
        boolean disabled = isAddComponentDisabledForNoEmployee();
        Assert.assertTrue(disabled,
                "Add Component button should be disabled until an employee is selected");
        ExtentManager.getTest().pass("Add Component correctly disabled with no employee selected");
    }

    /**
     * RV_PAY_F_003 – Initialize Pay Cycle button appears on Pay Cycles tab.
     */
    @Test(priority = 3, groups = {"smoke", "regression"},
          description = "RV_PAY_F_003 – Initialize Pay Cycle button visible on Pay Cycles tab")
    public void RV_PAY_F_003_initCycleButtonVisible() {
        payrollForm.openPayCyclesTab();
        boolean visible = WaitUtils.waitForPresence(driver,
            org.openqa.selenium.By.xpath(
                "//button[contains(normalize-space(.),'Initialize Pay Cycle')]"), 10);
        Assert.assertTrue(visible, "Initialize Pay Cycle button should appear on the Pay Cycles tab");
        ExtentManager.getTest().pass("Initialize Pay Cycle button visible");
    }

    // ── Salary component data-driven test ─────────────────────────────────────

    /**
     * RV_PAY_F_004 – Add Salary Component from Excel dataset.
     *
     * For each row in FormTestData.xlsx > SalaryComponent:
     *   PASS → modal closes after Save (component created in backend).
     *   FAIL → submit disabled OR form shows validation errors; modal stays open.
     */
    @Test(priority = 4, dataProvider = "salaryComponentRows",
          groups = {"regression", "datadriven"},
          description = "RV_PAY_F_004 – Add Salary Component data-driven loop")
    public void RV_PAY_F_004_addSalaryComponentFromExcel(Map<String, String> row) {
        String tcId     = row.getOrDefault("testCaseId", "RV_SAL_??");
        String expected = row.getOrDefault("expectedResult", "PASS").toUpperCase();
        String desc     = row.getOrDefault("description", "");

        ExtentManager.getTest().info(tcId + " — " + desc);

        // Always start on Salary tab
        if (!driver.getCurrentUrl().contains("payroll")) {
            driver.get(AppConstants.PAYROLL_URL);
            WaitUtils.waitForAngularLoad(driver);
        }
        payrollForm.openSalaryTab();

        PayrollFormPage.ComponentResult result = payrollForm.addSalaryComponentFlow(row, 10);
        ExtentManager.getTest().info(tcId + " result: " + result);

        if ("PASS".equals(expected)) {
            Assert.assertTrue(result.modalClosed,
                tcId + ": Expected component to be saved (modal close) but dialog stayed open. "
              + result);
            ExtentManager.getTest().pass(tcId + " — component saved, modal closed");
        } else {
            boolean stillOpen = !result.modalClosed;
            boolean invalid   = result.validationVisible || result.submitDisabled;
            Assert.assertTrue(stillOpen && invalid,
                tcId + ": Expected form validation failure but component was accepted. " + result);
            ExtentManager.getTest().pass(tcId + " — correctly rejected by validation");
            payrollForm.closeComponentModal();
        }
    }

    // ── Pay cycle data-driven test ────────────────────────────────────────────

    /**
     * RV_PAY_F_005 – Initialize Pay Cycle from Excel dataset.
     *
     * For each row in FormTestData.xlsx > PayCycle:
     *   PASS → modal closes after Initialize (cycle created in backend).
     *   FAIL → submit disabled OR form shows validation errors; modal stays open.
     */
    @Test(priority = 5, dataProvider = "payCycleRows",
          groups = {"regression", "datadriven"},
          description = "RV_PAY_F_005 – Initialize Pay Cycle data-driven loop")
    public void RV_PAY_F_005_initPayCycleFromExcel(Map<String, String> row) {
        String tcId     = row.getOrDefault("testCaseId", "RV_CYC_??");
        String expected = row.getOrDefault("expectedResult", "PASS").toUpperCase();
        String desc     = row.getOrDefault("description", "");

        ExtentManager.getTest().info(tcId + " — " + desc);

        // Navigate to Pay Cycles tab
        if (!driver.getCurrentUrl().contains("payroll")) {
            driver.get(AppConstants.PAYROLL_URL);
            WaitUtils.waitForAngularLoad(driver);
        }
        payrollForm.openPayCyclesTab();

        PayrollFormPage.CycleResult result = payrollForm.initializePayCycleFlow(row, 10);
        ExtentManager.getTest().info(tcId + " result: " + result);

        if ("PASS".equals(expected)) {
            Assert.assertTrue(result.modalClosed,
                tcId + ": Expected pay cycle to be initialized (modal close) but dialog stayed open. "
              + result);
            ExtentManager.getTest().pass(tcId + " — pay cycle initialized, modal closed");
        } else {
            boolean stillOpen = !result.modalClosed;
            boolean invalid   = result.validationVisible || result.submitDisabled;
            Assert.assertTrue(stillOpen && invalid,
                tcId + ": Expected form validation failure but cycle was accepted. " + result);
            ExtentManager.getTest().pass(tcId + " — correctly rejected by validation");
            payrollForm.closeCycleModal();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAddComponentDisabledForNoEmployee() {
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(
                org.openqa.selenium.By.xpath("//button[contains(.,'Add Component')]"));
            String dis = btn.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }
}
