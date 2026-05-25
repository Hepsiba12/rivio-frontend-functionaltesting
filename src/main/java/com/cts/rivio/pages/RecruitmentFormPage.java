package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * RecruitmentFormPage – drives the two POST-method dialogs on /ats
 * (Recruitment Pipeline page in Rivio_Angular-main).
 *
 * ── Dialog 1: Create Job Requisition ─────────────────────────────────────────
 *   Opened by: "New Requisition" button on the Job Openings tab
 *   Form fields (jobForm):
 *     title        – input[formcontrolname='title']    placeholder "Senior Frontend…"
 *     departmentId – p-select[formcontrolname='departmentId'] appendTo="body"
 *     locationId   – p-select[formcontrolname='locationId']   appendTo="body"
 *   Submit: button "Create Requisition"  [disabled]="isSubmitting() || jobForm.invalid"
 *
 * ── Dialog 2: Add Sourced Candidate ──────────────────────────────────────────
 *   Opened by: "Add Sourced Candidate" button on the Pipeline (Kanban) tab
 *   Form fields (candidateForm):
 *     jobOpeningId – p-select[formcontrolname='jobOpeningId'] appendTo="body"
 *                    Special value "AUTO" → picks first available option
 *     name         – input[formcontrolname='name']
 *     email        – input[formcontrolname='email']    (type="email")
 *     resumeUrl    – input[formcontrolname='resumeUrl'] (optional)
 *   Submit: button "Add Candidate"  [disabled]="isSubmitting() || candidateForm.invalid"
 *
 * JobResult / CandidateResult carry the outcome of each flow.
 */
public class RecruitmentFormPage {

    // ── Result DTOs ───────────────────────────────────────────────────────────

    public static class JobResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;

        public JobResult(boolean modalClosed, boolean validationVisible, boolean submitDisabled) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
        }

        @Override public String toString() {
            return "JobResult{closed=" + modalClosed
                 + ", validation=" + validationVisible
                 + ", submitDisabled=" + submitDisabled + "}";
        }
    }

    public static class CandidateResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;

        public CandidateResult(boolean modalClosed, boolean validationVisible, boolean submitDisabled) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
        }

        @Override public String toString() {
            return "CandidateResult{closed=" + modalClosed
                 + ", validation=" + validationVisible
                 + ", submitDisabled=" + submitDisabled + "}";
        }
    }

    // ── Locators ──────────────────────────────────────────────────────────────

    // Tab buttons
    private static final By PIPELINE_TAB  = By.xpath(
        "//button[contains(normalize-space(.),'Kanban Board')]");
    private static final By JOB_OPENINGS_TAB = By.xpath(
        "//button[contains(normalize-space(.),'Job Openings')]");

    // Job Openings tab buttons
    private static final By NEW_REQUISITION_BTN = By.xpath(
        "//button[contains(normalize-space(.),'New Requisition')]");

    // Kanban / Pipeline tab buttons
    private static final By ADD_SOURCED_CANDIDATE_BTN = By.xpath(
        "//button[contains(normalize-space(.),'Add Sourced Candidate')]");

    // Job Requisition modal form controls
    private static final By JOB_TITLE_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='title']");
    // Target the inner span.p-select-label — the p-select host is not "clickable" in WebDriver.
    // CSS multi-selector falls back to the host itself if the label span is absent.
    private static final By JOB_DEPT_DROPDOWN = By.cssSelector(
        ".p-dialog [formcontrolname='departmentId'] span.p-select-label, " +
        ".p-dialog [formcontrolname='departmentId'] span[role='combobox']");
    private static final By JOB_LOC_DROPDOWN = By.cssSelector(
        ".p-dialog [formcontrolname='locationId'] span.p-select-label, " +
        ".p-dialog [formcontrolname='locationId'] span[role='combobox']");
    private static final By JOB_SUBMIT_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[contains(.,'Create Requisition')] | " +
        "//button[contains(.,'Create Requisition')]");

    // Candidate modal form controls
    private static final By CAND_JOB_DROPDOWN = By.cssSelector(
        ".p-dialog [formcontrolname='jobOpeningId'] span.p-select-label, " +
        ".p-dialog [formcontrolname='jobOpeningId'] span[role='combobox']");
    private static final By CAND_NAME_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='name']");
    private static final By CAND_EMAIL_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='email']");
    private static final By CAND_RESUME_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='resumeUrl']");
    private static final By CAND_SUBMIT_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[contains(.,'Add Candidate')] | " +
        "//button[contains(.,'Add Candidate')]");

    // Dialog headers
    private static final By JOB_DIALOG_HEADER = By.xpath(
        "//*[contains(@class,'p-dialog-title')][contains(.,'Job Requisition')]");
    private static final By CAND_DIALOG_HEADER = By.xpath(
        "//*[contains(@class,'p-dialog-title')][contains(.,'Add Sourced Candidate')]");

    private final WebDriver driver;

    public RecruitmentFormPage(WebDriver driver) { this.driver = driver; }

    // ── Tab navigation ────────────────────────────────────────────────────────

    public void openPipelineTab() {
        try {
            WebElement tab = WaitUtils.waitForClickability(driver, PIPELINE_TAB);
            WaitUtils.scrollAndClick(driver, tab);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(400);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] openPipelineTab: " + e.getMessage());
        }
    }

    public void openJobOpeningsTab() {
        try {
            WebElement tab = WaitUtils.waitForClickability(driver, JOB_OPENINGS_TAB);
            WaitUtils.scrollAndClick(driver, tab);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.waitForPresence(driver, NEW_REQUISITION_BTN, 10);
            WaitUtils.hardWait(400);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] openJobOpeningsTab: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // JOB OPENING (REQUISITION) FLOW
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full create-job-requisition flow from a data-map row:
     *   title      – text input value ({ts} already substituted)
     *   department – p-select display label ("Engineering", "HR", etc.)
     *   location   – p-select display label ("Bengaluru HQ", "Mumbai", etc.)
     */
    public JobResult createJobRequisitionFlow(Map<String, String> row, int waitSeconds) {
        String title  = row.getOrDefault("title", "");
        String dept   = row.getOrDefault("department", "");
        String loc    = row.getOrDefault("location", "");

        // 1. Open the Job Openings tab and click New Requisition
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, NEW_REQUISITION_BTN);
            WaitUtils.safeClick(driver, btn);
            waitForModal(JOB_DIALOG_HEADER);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] Could not open Job Requisition modal: " + e.getMessage());
        }

        // 2. Fill title
        if (!title.isEmpty()) {
            typeIntoDialogInput(JOB_TITLE_INPUT, title);
        } else {
            clearDialogInput(JOB_TITLE_INPUT);
        }

        // 3. Select department
        if (!dept.isEmpty()) {
            selectDropdownInDialog(JOB_DEPT_DROPDOWN, dept);
            WaitUtils.hardWait(300);
        }

        // 4. Select location
        if (!loc.isEmpty()) {
            selectDropdownInDialog(JOB_LOC_DROPDOWN, loc);
            WaitUtils.hardWait(300);
        }

        // 5. Capture disabled state
        boolean submitDisabled = isButtonDisabled(JOB_SUBMIT_BTN);

        // 6. Click submit
        clickDialogButton(JOB_SUBMIT_BTN);
        WaitUtils.hardWait(600);

        // 7. Results
        boolean validation = pollForValidation(JOB_DIALOG_HEADER, 3);
        boolean closed     = waitForModalClose(JOB_DIALOG_HEADER, waitSeconds);

        return new JobResult(closed, validation, submitDisabled);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SOURCED CANDIDATE FLOW
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full add-sourced-candidate flow from a data-map row:
     *   jobOpeningTitle – display label to select OR "AUTO" (picks first option)
     *   candidateName   – text input ({ts} already substituted)
     *   email           – email input ({ts} already substituted)
     *   resumeUrl       – optional text input (may be empty)
     */
    public CandidateResult addSourcedCandidateFlow(Map<String, String> row, int waitSeconds) {
        String jobTitle   = row.getOrDefault("jobOpeningTitle", "");
        String name       = row.getOrDefault("candidateName", "");
        String email      = row.getOrDefault("email", "");
        String resumeUrl  = row.getOrDefault("resumeUrl", "");

        // 1. Open the modal from the Pipeline tab
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, ADD_SOURCED_CANDIDATE_BTN);
            WaitUtils.safeClick(driver, btn);
            waitForModal(CAND_DIALOG_HEADER);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] Could not open Add Candidate modal: " + e.getMessage());
        }

        // 2. Select job opening
        if (!jobTitle.isEmpty()) {
            if ("AUTO".equalsIgnoreCase(jobTitle)) {
                selectFirstDropdownOption(CAND_JOB_DROPDOWN);
            } else {
                selectDropdownInDialog(CAND_JOB_DROPDOWN, jobTitle);
            }
            WaitUtils.hardWait(300);
        }

        // 3. Fill name
        if (!name.isEmpty()) {
            typeIntoDialogInput(CAND_NAME_INPUT, name);
        } else {
            clearDialogInput(CAND_NAME_INPUT);
        }

        // 4. Fill email
        if (!email.isEmpty()) {
            typeIntoDialogInput(CAND_EMAIL_INPUT, email);
        } else {
            clearDialogInput(CAND_EMAIL_INPUT);
        }

        // 5. Fill optional resume URL
        if (!resumeUrl.isEmpty()) {
            typeIntoDialogInput(CAND_RESUME_INPUT, resumeUrl);
        }

        // 6. Capture disabled state
        boolean submitDisabled = isButtonDisabled(CAND_SUBMIT_BTN);

        // 7. Click submit
        clickDialogButton(CAND_SUBMIT_BTN);
        WaitUtils.hardWait(600);

        // 8. Results
        boolean validation = pollForValidation(CAND_DIALOG_HEADER, 3);
        boolean closed     = waitForModalClose(CAND_DIALOG_HEADER, waitSeconds);

        return new CandidateResult(closed, validation, submitDisabled);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHARED HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void typeIntoDialogInput(By locator, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, locator);
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(text);
            el.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] typeIntoDialogInput failed: " + e.getMessage());
        }
    }

    private void clearDialogInput(By locator) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, locator);
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] clearDialogInput failed: " + e.getMessage());
        }
    }

    /**
     * Opens a PrimeNG p-select inside a dialog and clicks the option matching
     * optionText. Panel is appendTo="body" so it renders outside the dialog.
     *
     * Strategy (avoids elementToBeClickable which is unreliable on PrimeNG <li>):
     *  1. Regular click on the span.p-select-label trigger (JS fallback if intercepted).
     *  2. Wait for ANY li.p-select-option to be PRESENT in the DOM (panel open check).
     *  3. Iterate all options; match by aria-label first, then getText() — case-insensitive.
     *  4. JS-click the matched option to avoid pointer-event interception.
     *  5. Span-child text as a final fallback.
     *  6. Diagnostic log listing all available option labels when the target isn't found.
     */
    private void selectDropdownInDialog(By dropdownLocator, String optionText) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                // 1. Find the trigger span inside the dialog
                WebElement trigger = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

                // 2. Regular click first so Angular's (click) binding fires normally.
                //    JS click only if the element is intercepted by an overlay.
                try { trigger.click(); }
                catch (Exception e) { WaitUtils.jsClick(driver, trigger); }
                WaitUtils.hardWait(600);

                // 3. Confirm the panel opened — wait for ANY option to be present.
                //    presenceOfElementLocated (not clickable) avoids false negatives
                //    caused by PrimeNG CSS animations on the <li> items.
                By anyOption = By.cssSelector(
                    "li.p-select-option, .p-select-overlay li[role='option']");
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.presenceOfElementLocated(anyOption));
                } catch (Exception panelEx) {
                    System.err.println("[RecruitmentFormPage] Panel did not open for '"
                        + optionText + "' (attempt " + (attempt + 1) + ")");
                    dismissOpenPanel();
                    WaitUtils.hardWait(400);
                    continue;
                }

                // 4. Collect all options and find by aria-label / text content.
                List<WebElement> allOpts = driver.findElements(anyOption);
                for (WebElement opt : allOpts) {
                    try {
                        String label = opt.getAttribute("aria-label");
                        if (label == null || label.isEmpty()) label = opt.getText();
                        if (label != null && optionText.equalsIgnoreCase(label.trim())) {
                            WaitUtils.jsClick(driver, opt);
                            WaitUtils.hardWait(300);
                            return;
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }

                // 5. Span-text fallback — option label may be in a child <span>.
                By spanFallback = By.xpath(
                    "//li[contains(@class,'p-select-option')]" +
                    "//span[normalize-space()='" + optionText + "'] | " +
                    "//li[contains(@class,'p-select-option') and @aria-label='" + optionText + "']");
                List<WebElement> spanMatches = driver.findElements(spanFallback);
                if (!spanMatches.isEmpty()) {
                    WaitUtils.jsClick(driver, spanMatches.get(0));
                    WaitUtils.hardWait(300);
                    return;
                }

                // 6. Diagnostic — log what IS available to expose label mismatches.
                StringBuilder available = new StringBuilder();
                for (WebElement o : allOpts) {
                    try {
                        String lbl = o.getAttribute("aria-label");
                        if (lbl == null || lbl.isEmpty()) lbl = o.getText();
                        if (lbl != null && !lbl.trim().isEmpty())
                            available.append("[").append(lbl.trim()).append("] ");
                    } catch (Exception ignored) {}
                }
                System.err.println("[RecruitmentFormPage] Option '" + optionText
                    + "' not found on attempt " + (attempt + 1)
                    + ". Available: " + available);

            } catch (StaleElementReferenceException stale) {
                WaitUtils.hardWait(300);
            } catch (Exception e) {
                System.err.println("[RecruitmentFormPage] selectDropdownInDialog attempt "
                    + (attempt + 1) + " failed: " + e.getMessage());
            }
            dismissOpenPanel();
            WaitUtils.hardWait(300);
        }
        System.err.println("[RecruitmentFormPage] Could not select option: " + optionText);
    }

    /**
     * Opens the p-select dropdown and clicks the FIRST available option.
     * Used for "AUTO" job opening selection so the test works even without
     * knowing which jobs are currently open in the backend.
     */
    private void selectFirstDropdownOption(By dropdownLocator) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement trigger = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));
                WaitUtils.jsClick(driver, trigger);
                WaitUtils.hardWait(500);

                By optBy = By.cssSelector(
                    ".p-select-option, .p-dropdown-item, li[class*='select-option']");

                List<WebElement> options = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(optBy));

                if (!options.isEmpty()) {
                    options.get(0).click();
                    WaitUtils.hardWait(200);
                    return;
                }
                System.err.println("[RecruitmentFormPage] No job opening options found in panel.");
                dismissOpenPanel();
                return;

            } catch (StaleElementReferenceException stale) {
                WaitUtils.hardWait(300);
            } catch (Exception e) {
                System.err.println("[RecruitmentFormPage] selectFirstDropdownOption attempt "
                    + (attempt + 1) + " failed: " + e.getMessage());
                WaitUtils.hardWait(300);
            }
            dismissOpenPanel();
        }
    }

    private void clickDialogButton(By locator) {
        try {
            WebElement btn = WaitUtils.waitForVisibility(driver, locator);
            try { btn.click(); }
            catch (Exception e) { WaitUtils.jsClick(driver, btn); }
        } catch (Exception e) {
            System.err.println("[RecruitmentFormPage] clickDialogButton failed: " + e.getMessage());
        }
    }

    private boolean isButtonDisabled(By locator) {
        try {
            List<WebElement> btns = driver.findElements(locator);
            if (btns.isEmpty()) return true;
            String dis = btns.get(0).getAttribute("disabled");
            return dis != null;
        } catch (Exception e) { return true; }
    }

    private void waitForModal(By headerLocator) {
        WaitUtils.waitForPresence(driver, headerLocator, 10);
        WaitUtils.hardWait(400);
    }

    private boolean waitForModalClose(By headerLocator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(headerLocator));
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean hasValidationErrors() {
        try {
            if (!driver.findElements(By.cssSelector(
                ".ng-invalid.ng-touched, .p-invalid, [aria-invalid='true']")).isEmpty())
                return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean pollForValidation(By dialogHeaderLocator, int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasValidationErrors()) return true;
            try {
                if (driver.findElements(dialogHeaderLocator).isEmpty()) return false;
            } catch (Exception ignored) {}
            WaitUtils.hardWait(150);
        }
        return hasValidationErrors();
    }

    private void dismissOpenPanel() {
        // Send ESC to the currently focused element (closes the dropdown overlay).
        // NEVER send ESC to body/document — PrimeNG dialog closes on document-level ESC.
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
            WaitUtils.hardWait(200);
        } catch (Exception ignored) {}
        // Fallback: click the dialog title bar — neutral area that won't close the dialog
        try {
            List<WebElement> titles = driver.findElements(By.cssSelector(".p-dialog-title"));
            for (WebElement t : titles) {
                try {
                    if (t.isDisplayed()) { t.click(); WaitUtils.hardWait(150); break; }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    public void closeJobModal() {
        clickCancelInActiveDialog();
    }

    public void closeCandidateModal() {
        clickCancelInActiveDialog();
    }

    private void clickCancelInActiveDialog() {
        try {
            WebElement cancelBtn = driver.findElement(By.xpath(
                "//div[contains(@class,'p-dialog')]//button[contains(.,'Cancel')]"));
            WaitUtils.safeClick(driver, cancelBtn);
            WaitUtils.hardWait(300);
        } catch (Exception ignored) {}
    }

    public boolean isJobModalOpen() {
        return !driver.findElements(JOB_DIALOG_HEADER).isEmpty();
    }

    public boolean isCandidateModalOpen() {
        return !driver.findElements(CAND_DIALOG_HEADER).isEmpty();
    }
}
