package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.RecruitmentFormPage;
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
 * RecruitmentFormTest – data-driven tests for the two POST-method dialogs
 * on /ats (Recruitment Pipeline, Admin role).
 *
 * ── Job Opening (Requisition) tests ──────────────────────────────────────────
 * Reads FormTestData.xlsx > JobOpening. Each row:
 *   testCaseId | title | department | location | expectedResult | description
 *
 *   title with {ts} → unique per run.
 *   department / location are display labels from p-select dropdowns.
 *   Submit button: "Create Requisition"
 *
 * ── Sourced Candidate tests ───────────────────────────────────────────────────
 * Reads FormTestData.xlsx > SourcedCandidate. Each row:
 *   testCaseId | jobOpeningTitle | candidateName | email | resumeUrl
 *              | expectedResult | description
 *
 *   jobOpeningTitle = "AUTO" → page object picks first open job from the panel.
 *   email / candidateName with {ts} → unique per run.
 *   resumeUrl is optional (may be empty for positive cases without a resume link).
 *   Submit button: "Add Candidate"
 *
 * NOTE: The sourced-candidate tests run AFTER the job-opening tests so there is
 * at least one open job for the "AUTO" selection to pick from.
 */
public class RecruitmentFormTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private RecruitmentFormPage recruitmentForm;

    @BeforeClass(alwaysRun = true)
    public void bootstrapTestData() {
        FormTestDataBuilder.ensureAllSheets();
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToRecruitment() {
        driver.get(AppConstants.RECRUITMENT_URL);
        WaitUtils.waitForAngularLoad(driver);
        recruitmentForm = new RecruitmentFormPage(driver);

        // Close any stale modals from the previous iteration
        if (recruitmentForm.isJobModalOpen()) {
            recruitmentForm.closeJobModal();
            WaitUtils.hardWait(300);
        }
        if (recruitmentForm.isCandidateModalOpen()) {
            recruitmentForm.closeCandidateModal();
            WaitUtils.hardWait(300);
        }
    }

    // ── DataProviders ─────────────────────────────────────────────────────────

    @DataProvider(name = "jobOpeningRows")
    public Object[][] jobOpeningRows() {
        List<Map<String, String>> rows = ExcelUtils.readDataAsMapList(
                AppConstants.FORM_TEST_DATA_PATH,
                AppConstants.SHEET_JOB_OPENING);

        // Single ts stamp per run for unique titles
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

    @DataProvider(name = "sourcedCandidateRows")
    public Object[][] sourcedCandidateRows() {
        List<Map<String, String>> rows = ExcelUtils.readDataAsMapList(
                AppConstants.FORM_TEST_DATA_PATH,
                AppConstants.SHEET_SOURCED_CANDIDATE);

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
     * RV_REC_F_001 – Recruitment Pipeline page loads for Admin role.
     */
    @Test(priority = 1, groups = {"smoke", "regression"},
          description = "RV_REC_F_001 – Recruitment Pipeline page loads")
    public void RV_REC_F_001_pageLoads() {
        boolean loaded = WaitUtils.waitForH1Text(driver, "Recruitment Pipeline", 15);
        Assert.assertTrue(loaded, "Recruitment Pipeline page should load");
        ExtentManager.getTest().pass("Recruitment Pipeline page loaded successfully");
    }

    /**
     * RV_REC_F_002 – New Requisition modal opens from Job Openings tab.
     */
    @Test(priority = 2, groups = {"smoke", "regression"},
          description = "RV_REC_F_002 – New Requisition modal opens")
    public void RV_REC_F_002_newRequisitionModalOpens() {
        recruitmentForm.openJobOpeningsTab();

        try {
            org.openqa.selenium.WebElement btn = WaitUtils.waitForClickability(driver,
                org.openqa.selenium.By.xpath(
                    "//button[contains(normalize-space(.),'New Requisition')]"));
            WaitUtils.safeClick(driver, btn);
        } catch (Exception e) {
            throw new AssertionError("New Requisition button not found", e);
        }

        boolean open = recruitmentForm.isJobModalOpen();
        Assert.assertTrue(open, "'New Requisition' button should open the Create Job Requisition dialog");
        recruitmentForm.closeJobModal();
        ExtentManager.getTest().pass("Create Job Requisition modal opens and closes correctly");
    }

    /**
     * RV_REC_F_003 – Add Sourced Candidate modal opens from Pipeline tab.
     */
    @Test(priority = 3, groups = {"smoke", "regression"},
          description = "RV_REC_F_003 – Add Sourced Candidate modal opens")
    public void RV_REC_F_003_addCandidateModalOpens() {
        recruitmentForm.openPipelineTab();

        try {
            org.openqa.selenium.WebElement btn = WaitUtils.waitForClickability(driver,
                org.openqa.selenium.By.xpath(
                    "//button[contains(normalize-space(.),'Add Sourced Candidate')]"));
            WaitUtils.safeClick(driver, btn);
        } catch (Exception e) {
            throw new AssertionError("Add Sourced Candidate button not found", e);
        }

        boolean open = recruitmentForm.isCandidateModalOpen();
        Assert.assertTrue(open, "'Add Sourced Candidate' button should open the dialog");
        recruitmentForm.closeCandidateModal();
        ExtentManager.getTest().pass("Add Sourced Candidate modal opens and closes correctly");
    }

    // ── Job opening data-driven test ──────────────────────────────────────────

    /**
     * RV_REC_F_004 – Create Job Requisition from Excel dataset.
     *
     * For each row in FormTestData.xlsx > JobOpening:
     *   PASS → modal closes after "Create Requisition" (job created in backend).
     *   FAIL → submit disabled OR validation errors; modal stays open.
     */
    @Test(priority = 4, dataProvider = "jobOpeningRows",
          groups = {"regression", "datadriven"},
          description = "RV_REC_F_004 – Create Job Requisition data-driven loop")
    public void RV_REC_F_004_createJobRequisitionFromExcel(Map<String, String> row) {
        String tcId     = row.getOrDefault("testCaseId", "RV_JOB_??");
        String expected = row.getOrDefault("expectedResult", "PASS").toUpperCase();
        String desc     = row.getOrDefault("description", "");

        ExtentManager.getTest().info(tcId + " — " + desc);

        // Navigate to Job Openings tab before each iteration
        if (!driver.getCurrentUrl().contains("/ats")) {
            driver.get(AppConstants.RECRUITMENT_URL);
            WaitUtils.waitForAngularLoad(driver);
        }
        recruitmentForm.openJobOpeningsTab();

        RecruitmentFormPage.JobResult result = recruitmentForm.createJobRequisitionFlow(row, 10);
        ExtentManager.getTest().info(tcId + " result: " + result);

        if ("PASS".equals(expected)) {
            Assert.assertTrue(result.modalClosed,
                tcId + ": Expected job requisition to be created (modal close) but dialog stayed open. "
              + result);
            ExtentManager.getTest().pass(tcId + " — job requisition created, modal closed");
        } else {
            boolean stillOpen = !result.modalClosed;
            boolean invalid   = result.validationVisible || result.submitDisabled;
            Assert.assertTrue(stillOpen && invalid,
                tcId + ": Expected form validation failure but requisition was accepted. " + result);
            ExtentManager.getTest().pass(tcId + " — correctly rejected by validation");
            recruitmentForm.closeJobModal();
        }
    }

    // ── Sourced candidate data-driven test ────────────────────────────────────

    /**
     * RV_REC_F_005 – Add Sourced Candidate from Excel dataset.
     *
     * IMPORTANT: This test group runs AFTER RV_REC_F_004 so there will be
     * at least one open job for "AUTO" selection to work. If no jobs exist,
     * the test is skipped with a clear message.
     *
     * For each row in FormTestData.xlsx > SourcedCandidate:
     *   PASS → modal closes after "Add Candidate" (candidate saved in backend).
     *   FAIL → submit disabled OR validation errors; modal stays open.
     */
    @Test(priority = 5, dataProvider = "sourcedCandidateRows",
          groups = {"regression", "datadriven"},
          description = "RV_REC_F_005 – Add Sourced Candidate data-driven loop")
    public void RV_REC_F_005_addSourcedCandidateFromExcel(Map<String, String> row) {
        String tcId       = row.getOrDefault("testCaseId", "RV_CND_??");
        String expected   = row.getOrDefault("expectedResult", "PASS").toUpperCase();
        String desc       = row.getOrDefault("description", "");
        String jobTitle   = row.getOrDefault("jobOpeningTitle", "");

        ExtentManager.getTest().info(tcId + " — " + desc);

        // Navigate to the Pipeline (Kanban) tab
        if (!driver.getCurrentUrl().contains("/ats")) {
            driver.get(AppConstants.RECRUITMENT_URL);
            WaitUtils.waitForAngularLoad(driver);
        }
        recruitmentForm.openPipelineTab();

        // For positive "AUTO" rows, verify there is at least one open job before proceeding
        if ("AUTO".equalsIgnoreCase(jobTitle) && "PASS".equals(expected)) {
            boolean hasJobs = hasAtLeastOneOpenJob();
            if (!hasJobs) {
                ExtentManager.getTest().warning(
                    tcId + " — No open jobs in the system. Skipping positive candidate test.");
                throw new org.testng.SkipException(
                    tcId + ": No open jobs available for AUTO selection — "
                  + "run the Job Opening tests first");
            }
        }

        RecruitmentFormPage.CandidateResult result =
            recruitmentForm.addSourcedCandidateFlow(row, 10);
        ExtentManager.getTest().info(tcId + " result: " + result);

        if ("PASS".equals(expected)) {
            Assert.assertTrue(result.modalClosed,
                tcId + ": Expected candidate to be added (modal close) but dialog stayed open. "
              + result);
            ExtentManager.getTest().pass(tcId + " — candidate added, modal closed");
        } else {
            boolean stillOpen = !result.modalClosed;
            boolean invalid   = result.validationVisible || result.submitDisabled;
            Assert.assertTrue(stillOpen && invalid,
                tcId + ": Expected form validation failure but candidate was accepted. " + result);
            ExtentManager.getTest().pass(tcId + " — correctly rejected by validation");
            recruitmentForm.closeCandidateModal();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Checks whether there is at least one open job in the Job Openings tab.
     * Navigates to the tab, counts rows with status not "CLOSED".
     */
    private boolean hasAtLeastOneOpenJob() {
        try {
            recruitmentForm.openJobOpeningsTab();
            // Look for any job row in the table
            List<org.openqa.selenium.WebElement> rows = driver.findElements(
                org.openqa.selenium.By.cssSelector("p-table tbody tr"));
            if (rows.isEmpty()) return false;
            // Check that not all are CLOSED
            for (org.openqa.selenium.WebElement tr : rows) {
                String text = tr.getText();
                if (!text.toUpperCase().contains("CLOSED")) return true;
            }
        } catch (Exception e) {
            System.err.println("[RecruitmentFormTest] hasAtLeastOneOpenJob: " + e.getMessage());
        }
        return false;
    }
}
