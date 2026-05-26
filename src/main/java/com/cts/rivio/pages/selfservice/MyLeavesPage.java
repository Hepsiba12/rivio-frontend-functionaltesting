package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Month;
import java.util.List;
import java.util.Map;

/**
 * MyLeavesPage – page object for /self-service/leaves.
 *
 * Selectors verified against the live DOM (2026-05-25 snapshot):
 *
 *   p-select[formcontrolname='leaveTypeId']
 *     └─ span.p-select-label[role='combobox']        ← click to open
 *     Panel: div.p-overlay[data-pc-name='pcoverlay'] (in body)
 *       └─ li.p-select-option[aria-label='Sick Leave'] ← click to choose
 *
 *   p-datepicker[formcontrolname='dateRange']
 *     ├─ input.p-datepicker-input[readonly]           ← readonlyInput=true
 *     └─ button.p-datepicker-dropdown[aria-label='Choose Date'] ← calendar trigger
 *
 *   Dialog:  span.p-dialog-title  text="Apply for Leave"
 *   Submit:  button[type='submit']  text contains "Submit Request"
 *   Cancel:  button[type='button']  text="Cancel"
 *
 * Available leave types (from live DOM):
 *   "Sick Leave" | "Casual Leave" | "Earned Leave"
 */
public class MyLeavesPage {

    // ── Result DTO ────────────────────────────────────────────────────────────

    public static class LeaveResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;

        public LeaveResult(boolean modalClosed, boolean validationVisible, boolean submitDisabled) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
        }

        @Override public String toString() {
            return "LeaveResult{closed=" + modalClosed
                 + ", validation=" + validationVisible
                 + ", submitDisabled=" + submitDisabled + "}";
        }
    }

    // ── Locators — verified against live DOM ──────────────────────────────────

    // Page heading
    private static final By PAGE_HEADING = By.xpath("//h1[normalize-space()='My Leaves']");

    // "Apply for Leave" button on the page (outside the dialog)
    private static final By APPLY_BUTTON = By.xpath("//button[contains(.,'Apply for Leave')]");

    // Dialog: span.p-dialog-title contains "Apply for Leave"
    // (renders inside div.p-dialog inside div.p-dialog-mask)
    private static final By DIALOG_TITLE = By.cssSelector("span.p-dialog-title");

    // ── Inside the dialog ─────────────────────────────────────────────────────

    // Leave type p-select — click the combobox label to open the panel
    // DOM: <span role="combobox" class="p-select-label" ...>
    private static final By LEAVE_TYPE_COMBOBOX = By.cssSelector(
        "[formcontrolname='leaveTypeId'] span.p-select-label, " +
        "[formcontrolname='leaveTypeId'] span[role='combobox']");

    // Calendar trigger for the date range picker
    // DOM: <button class="p-datepicker-dropdown" aria-label="Choose Date">
    private static final By DATE_RANGE_TRIGGER = By.cssSelector(
        "button.p-datepicker-dropdown[aria-label='Choose Date'], " +
        "button.p-datepicker-dropdown");

    // Submit: <button type="submit" ...> Submit Request </button>
    private static final By SUBMIT_BTN = By.cssSelector("button[type='submit']");

    // Cancel: <button type="button" class="btn-glass ...">Cancel</button>
    private static final By CANCEL_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[@type='button'][normalize-space()='Cancel']");

    // ── Dropdown option panel ─────────────────────────────────────────────────

    // Panel root: <div class="p-overlay" data-pc-name="pcoverlay" ...>
    // rendered at body level when appendTo="body"
    private static final By OPTION_PANEL = By.cssSelector(
        "div.p-overlay[data-pc-name='pcoverlay'], " +
        "div.p-select-overlay, " +
        "div[class*='p-overlay'][class*='p-select']");

    // Individual option: <li role="option" class="p-ripple p-select-option" aria-label="Sick Leave">
    // aria-label is the option display text — most reliable selector from the DOM.
    private static final String OPTION_XPATH =
        "//li[@role='option' and contains(@class,'p-select-option') and @aria-label='%s'] | " +
        "//li[contains(@class,'p-select-option')]//span[@data-pc-section='optionlabel' and normalize-space()='%s']";

    // ── Calendar panel ────────────────────────────────────────────────────────

    // PrimeNG datepicker panel (appendTo="body"):
    //   <div class="p-datepicker p-overlay-content"> or via p-motion
    private static final By CALENDAR_PANEL = By.cssSelector(
        "div.p-datepicker:not([class*='p-datepicker-input']), " +
        ".p-datepicker-calendar, " +
        "[data-pc-name='datepicker'] .p-datepicker-calendar-container");

    private final WebDriver driver;

    public MyLeavesPage(WebDriver driver) { this.driver = driver; }

    // ── Page-level checks ─────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "My Leaves", 15);
    }

    public int getBalanceCardCount() {
        return driver.findElements(By.cssSelector(".glass-panel.p-6.relative.overflow-hidden")).size();
    }

    public int getHistoryRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    // ── Modal lifecycle ───────────────────────────────────────────────────────

    public void clickApplyForLeave() {
        WebElement btn = WaitUtils.waitForClickability(driver, APPLY_BUTTON);
        WaitUtils.safeClick(driver, btn);
        waitForModalOpen();
    }

    /**
     * Non-waiting check — uses findElements so it never blocks > a few ms.
     * Safe to call in @BeforeMethod to detect stale modals.
     */
    public boolean isApplyModalOpenFast() {
        try {
            List<WebElement> dialogs = driver.findElements(By.cssSelector(".p-dialog"));
            return !dialogs.isEmpty();
        } catch (Exception e) { return false; }
    }

    /**
     * Waiting check — up to 3 s. Use this when you expect the modal to appear.
     */
    public boolean isApplyModalOpen() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(3))
                .until(d -> {
                    List<WebElement> titles = d.findElements(DIALOG_TITLE);
                    return titles.stream().anyMatch(t -> {
                        try { return t.getText().contains("Apply for Leave"); }
                        catch (Exception e) { return false; }
                    });
                });
        } catch (Exception e) { return false; }
    }

    private void waitForModalOpen() {
        // Wait for the span.p-dialog-title to appear and contain "Apply for Leave"
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> {
                    List<WebElement> titles = d.findElements(DIALOG_TITLE);
                    return titles.stream().anyMatch(t -> {
                        try { return t.getText().contains("Apply for Leave"); }
                        catch (Exception e) { return false; }
                    });
                });
        } catch (Exception e) {
            System.err.println("[MyLeavesPage] Modal did not open in time: " + e.getMessage());
        }
        WaitUtils.hardWait(400); // Angular renders form controls after the dialog animation
    }

    public boolean waitForModalClose(int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(d -> {
                    List<WebElement> titles = d.findElements(DIALOG_TITLE);
                    return titles.stream().noneMatch(t -> {
                        try { return t.getText().contains("Apply for Leave"); }
                        catch (Exception e) { return false; }
                    });
                });
            return true;
        } catch (Exception e) { return false; }
    }

    public void closeModal() {
        try {
            WebElement cancel = WaitUtils.waitForClickability(driver, CANCEL_BTN);
            WaitUtils.safeClick(driver, cancel);
            WaitUtils.hardWait(400);
        } catch (Exception ignored) {
            // Fallback: close via the X button
            try {
                WebElement closeBtn = driver.findElement(By.cssSelector(
                    ".p-dialog-close-button, button.p-dialog-header-close"));
                WaitUtils.safeClick(driver, closeBtn);
                WaitUtils.hardWait(300);
            } catch (Exception ignored2) {}
        }
    }

    // ── Full apply-leave flow ─────────────────────────────────────────────────

    /**
     * Drives the entire Apply for Leave dialog from a data-map row.
     *
     * Keys read from the map:
     *   leaveType  – exact display name: "Sick Leave" | "Casual Leave" | "Earned Leave"
     *   startDate  – YYYY-MM-DD (must be a weekday; Sat/Sun are disabled by the app)
     *   endDate    – YYYY-MM-DD (same constraint)
     *
     * Returns LeaveResult. waitSeconds controls the modal-close poll for PASS rows.
     */
    public LeaveResult applyLeaveFlow(Map<String, String> row, int waitSeconds) {
        String leaveType = row.getOrDefault("leaveType", "");
        String startDate = row.getOrDefault("startDate", "");
        String endDate   = row.getOrDefault("endDate", "");

        // 1. Select leave type (empty = negative case, skip)
        if (!leaveType.isEmpty()) {
            selectLeaveType(leaveType);
            WaitUtils.hardWait(300);
        }

        // 2. Select date range (empty = negative case, skip)
        if (!startDate.isEmpty() && !endDate.isEmpty()) {
            selectDateRange(startDate, endDate);
            WaitUtils.hardWait(300);
        }

        // 3. Capture submit button disabled state before clicking
        boolean submitDisabled = isSubmitDisabled();

        // 4. Click submit (JS fallback handles the disabled attribute)
        clickSubmit();
        WaitUtils.hardWait(600);

        // 5. Fast poll for validation errors (negative cases respond in <1 s)
        boolean validationFound = pollForValidation(3);

        // 6. Wait for modal close (positive cases)
        boolean closed = waitForModalClose(waitSeconds);

        return new LeaveResult(closed, validationFound, submitDisabled);
    }

    // ── Leave type selector ───────────────────────────────────────────────────

    /**
     * Selects a leave type from the p-select.
     *
     * From the DOM:
     *   – Click span.p-select-label (the combobox trigger) to open the panel.
     *   – Panel appears as div.p-overlay at body level.
     *   – Options: li.p-select-option[aria-label='Sick Leave'] etc.
     */
    public void selectLeaveType(String leaveTypeName) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                // 1. Open the dropdown by clicking the combobox label
                WebElement combobox = WaitUtils.waitForClickability(driver, LEAVE_TYPE_COMBOBOX);
                WaitUtils.safeClick(driver, combobox);
                WaitUtils.hardWait(400);

                // 2. Click the matching option (aria-label is most reliable)
                String xpath = String.format(OPTION_XPATH, leaveTypeName, leaveTypeName);
                try {
                    WebElement opt = new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
                    opt.click();
                    WaitUtils.hardWait(200);
                    return;
                } catch (Exception e) {
                    // Fallback: any visible li with matching text in the overlay
                    By fallback = By.xpath(
                        "//*[contains(@class,'p-overlay') or contains(@class,'p-select-overlay')]" +
                        "//li[normalize-space()='" + leaveTypeName + "' " +
                        "or @aria-label='" + leaveTypeName + "']");
                    try {
                        WaitUtils.waitForClickability(driver, fallback).click();
                        WaitUtils.hardWait(200);
                        return;
                    } catch (Exception ignored) {}
                }
            } catch (StaleElementReferenceException stale) {
                WaitUtils.hardWait(300);
            }
            dismissOpenPanel();
            WaitUtils.hardWait(200);
        }
        System.err.println("[MyLeavesPage] Could not select leave type: " + leaveTypeName);
    }

    // ── Date range selector ───────────────────────────────────────────────────

    /**
     * Selects a date range using the PrimeNG datepicker calendar UI.
     *
     * From the DOM:
     *   readonlyInput=true  → the input has readonly=""; sendKeys is blocked.
     *   trigger: button.p-datepicker-dropdown[aria-label='Choose Date']
     *   selectionMode=range → click start date, then click end date.
     *   disabledDays=[0,6]  → Sat/Sun cells are disabled; only Mon–Fri valid.
     *
     * Both startDate and endDate must be YYYY-MM-DD weekdays.
     */
    public void selectDateRange(String startDate, String endDate) {
        // 1. Open the calendar panel
        openDatePickerPanel();
        WaitUtils.hardWait(600);

        // 2. Click the start date
        clickDateInOpenPanel(startDate);
        WaitUtils.hardWait(500);

        // 3. End date — panel stays open in range mode
        //    Re-open if it closed unexpectedly
        if (!isCalendarPanelOpen()) {
            openDatePickerPanel();
            WaitUtils.hardWait(400);
        }
        clickDateInOpenPanel(endDate);
        WaitUtils.hardWait(400);

        // 4. Dismiss if still open
        dismissOpenPanel();
    }

    // ── Calendar helpers ──────────────────────────────────────────────────────

    private void openDatePickerPanel() {
        // Primary: button.p-datepicker-dropdown[aria-label='Choose Date']
        for (By locator : new By[]{
            DATE_RANGE_TRIGGER,
            By.cssSelector(".p-dialog button.p-datepicker-dropdown"),
            By.cssSelector("p-datepicker button[aria-label='Choose Date']"),
            By.cssSelector("[formcontrolname='dateRange'] button"),
        }) {
            try {
                List<WebElement> btns = driver.findElements(locator);
                for (WebElement btn : btns) {
                    if (btn.isDisplayed()) {
                        WaitUtils.scrollAndClick(driver, btn);
                        WaitUtils.hardWait(300);
                        if (isCalendarPanelOpen()) return;
                    }
                }
            } catch (Exception ignored) {}
        }
        // Last resort: click the readonly input to trigger the calendar
        try {
            WebElement inp = driver.findElement(
                By.cssSelector("input.p-datepicker-input, [formcontrolname='dateRange'] input"));
            WaitUtils.safeClick(driver, inp);
        } catch (Exception ignored) {}
    }

    private boolean isCalendarPanelOpen() {
        try {
            // The calendar panel is a table inside the datepicker overlay
            List<WebElement> panels = driver.findElements(
                By.cssSelector("table.p-datepicker-day-view, " +
                                ".p-datepicker-calendar, " +
                                "div.p-datepicker table"));
            return panels.stream().anyMatch(WebElement::isDisplayed);
        } catch (Exception e) { return false; }
    }

    /**
     * Navigates the open calendar panel to the month of the target date,
     * then clicks the day cell.
     *
     * @param yyyyMmDd  YYYY-MM-DD string
     */
    private void clickDateInOpenPanel(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.isEmpty()) return;

        String[] parts = yyyyMmDd.split("-");
        int targetYear  = Integer.parseInt(parts[0]);
        int targetMonth = Integer.parseInt(parts[1]); // 1-based
        int targetDay   = Integer.parseInt(parts[2]);

        // Navigate to the correct month (max 24 arrow clicks)
        for (int i = 0; i < 24; i++) {
            int[] cur = readCalendarMonthYear();
            if (cur == null) break;
            if (cur[0] == targetYear && cur[1] == targetMonth) break;

            boolean goForward = (cur[0] < targetYear)
                || (cur[0] == targetYear && cur[1] < targetMonth);
            clickCalendarNav(goForward);
            WaitUtils.hardWait(350);
        }

        // Click the target day cell
        clickDayCell(targetDay);
    }

    /**
     * Reads the current month and year shown in the open calendar panel.
     * Returns [year, month(1-based)] or null if the header cannot be parsed.
     *
     * PrimeNG 17 calendar header (from live DOM):
     *   span/button with class p-datepicker-month-title  → "July"
     *   span/button with class p-datepicker-year-title   → "2026"
     */
    private int[] readCalendarMonthYear() {
        String[] monthSelectors = {
            // PrimeNG 17 — primary class name
            "button.p-datepicker-select-month",
            ".p-datepicker-select-month",
            // PrimeNG 16 fallbacks
            ".p-datepicker-month-title",
            "button.p-datepicker-month",
            ".p-datepicker-title > button:first-child",
            ".p-datepicker-title > span:first-child",
        };
        String[] yearSelectors = {
            // PrimeNG 17 — primary class name
            "button.p-datepicker-select-year",
            ".p-datepicker-select-year",
            // PrimeNG 16 fallbacks
            ".p-datepicker-year-title",
            "button.p-datepicker-year",
            ".p-datepicker-title > button:last-child",
            ".p-datepicker-title > span:last-child",
        };

        String monthText = null, yearText = null;
        for (String sel : monthSelectors) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(sel));
                if (!els.isEmpty()) {
                    monthText = els.get(0).getText().trim();
                    if (!monthText.isEmpty()) break;
                }
            } catch (Exception ignored) {}
        }
        for (String sel : yearSelectors) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(sel));
                if (!els.isEmpty()) {
                    yearText = els.get(0).getText().trim();
                    if (!yearText.isEmpty()) break;
                }
            } catch (Exception ignored) {}
        }

        if (monthText == null || yearText == null) return null;
        try {
            int year = Integer.parseInt(yearText.replaceAll("[^0-9]", ""));
            return new int[]{year, parseMonthName(monthText)};
        } catch (NumberFormatException e) { return null; }
    }

    private int parseMonthName(String name) {
        String n = name.trim().toUpperCase();
        String[] months = {
            "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
            "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"
        };
        for (int i = 0; i < months.length; i++) {
            if (months[i].startsWith(n.substring(0, Math.min(3, n.length())))) return i + 1;
        }
        try { return Month.valueOf(n).getValue(); } catch (Exception ignored) {}
        return 1;
    }

    private void clickCalendarNav(boolean forward) {
        String[] selectors = forward
            ? new String[]{
                "button.p-datepicker-next-button",
                "button.p-datepicker-next",
                "button[aria-label='Next Month']",
                ".p-datepicker-next",
              }
            : new String[]{
                "button.p-datepicker-prev-button",
                "button.p-datepicker-prev",
                "button[aria-label='Previous Month']",
                ".p-datepicker-prev",
              };

        for (String sel : selectors) {
            try {
                List<WebElement> btns = driver.findElements(By.cssSelector(sel));
                if (!btns.isEmpty() && btns.get(0).isDisplayed()) {
                    WaitUtils.safeClick(driver, btns.get(0));
                    WaitUtils.hardWait(200);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Clicks the day cell for the given day number.
     *
     * PrimeNG 17 day cells structure:
     *   <td data-p-other-month="false" data-p-disabled="false">   ← click handler here
     *     <span class="p-datepicker-day">6</span>
     *   </td>
     *
     * IMPORTANT: In PrimeNG 17 the (click) event handler is bound to the <td>,
     * not the inner <span>. We therefore select the <td> that *contains* the
     * matching span and click the <td> directly.
     */
    private void clickDayCell(int day) {
        String dayStr = String.valueOf(day);

        // Primary strategy: click the <td> directly (PrimeNG 17 attaches click to <td>)
        By[] tdSelectors = {
            // data-p attributes present (PrimeNG 17)
            By.xpath(
                "//td[@data-p-other-month='false' and @data-p-disabled='false']" +
                "[.//span[normalize-space()='" + dayStr + "']]"),
            // Older PrimeNG or no data-p attributes
            By.xpath(
                "//td[not(contains(@class,'p-disabled')) " +
                "and not(contains(@class,'p-datepicker-other-month'))]" +
                "[.//span[normalize-space()='" + dayStr + "']]"),
        };

        for (By by : tdSelectors) {
            try {
                List<WebElement> tds = driver.findElements(by);
                for (WebElement td : tds) {
                    try {
                        if (td.isDisplayed()) {
                            // Prefer JS click so events fire even if element is partially
                            // obscured by the dialog overlay
                            WaitUtils.jsClick(driver, td);
                            WaitUtils.hardWait(250);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        // Fallback: click the inner <span> — bubbling should still reach the <td>
        By spanFallback = By.xpath(
            "//td[@data-p-other-month='false' and @data-p-disabled='false']" +
            "/span[normalize-space()='" + dayStr + "']");
        try {
            List<WebElement> spans = driver.findElements(spanFallback);
            for (WebElement span : spans) {
                if (span.isDisplayed()) {
                    WaitUtils.scrollAndClick(driver, span);
                    WaitUtils.hardWait(250);
                    return;
                }
            }
        } catch (Exception ignored) {}

        System.err.println("[MyLeavesPage] Could not click day cell for day: " + day
            + " — td and span selectors both failed");
    }

    private void dismissOpenPanel() {
        // Escape key closes both dropdowns and calendar panels
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
            WaitUtils.hardWait(200);
        } catch (Exception ignored) {}
        // JS body click as fallback
        try {
            if (isCalendarPanelOpen()) {
                ((JavascriptExecutor) driver).executeScript("document.body.click()");
                WaitUtils.hardWait(200);
            }
        } catch (Exception ignored) {}
        // Close any lingering p-select panels
        try {
            List<WebElement> panels = driver.findElements(By.cssSelector(
                ".p-select-panel, .p-dropdown-panel, div[data-pc-name='pcoverlay']"));
            for (WebElement p : panels) {
                try { if (p.isDisplayed()) p.sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    /**
     * Checks whether the Submit Request button is disabled.
     * The button is disabled when: form invalid OR submitting OR
     * insufficientBalance OR daysRequested == 0.
     *
     * IMPORTANT: Angular sets the DOM *property* `disabled`, not the HTML *attribute*.
     * Therefore getAttribute("disabled") always returns null (misleading "enabled").
     * We use WebElement.isEnabled() which reads the property correctly, OR fall back
     * to a JS property check for robustness.
     */
    public boolean isSubmitDisabled() {
        try {
            List<WebElement> btns = driver.findElements(SUBMIT_BTN);
            if (btns.isEmpty()) return true;
            WebElement btn = btns.get(0);
            // isEnabled() reads the DOM property (not attribute) — correct for Angular
            if (!btn.isEnabled()) return true;
            // Double-check via JS property in case of driver inconsistency
            Object jsProp = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].disabled;", btn);
            if (Boolean.TRUE.equals(jsProp)) return true;
            return false;
        } catch (Exception e) { return true; }
    }

    /**
     * Clicks the Submit Request button.
     * If disabled, uses JS click to still trigger Angular's markAllAsTouched()
     * so validation markers appear.
     */
    public void clickSubmit() {
        try {
            List<WebElement> btns = driver.findElements(SUBMIT_BTN);
            if (!btns.isEmpty()) {
                try { btns.get(0).click(); }
                catch (Exception e) { WaitUtils.jsClick(driver, btns.get(0)); }
                return;
            }
        } catch (Exception ignored) {}
        // Last resort JS
        try {
            ((JavascriptExecutor) driver).executeScript(
                "var btn = document.querySelector('button[type=\"submit\"]');" +
                "if(btn) btn.click();");
        } catch (Exception ignored) {}
    }

    // ── Validation detection ──────────────────────────────────────────────────

    /**
     * Returns true if any Angular validation markers are visible in the dialog.
     * Checks for: ng-invalid+ng-touched, p-invalid, or the submit button disabled.
     */
    public boolean hasValidationErrors() {
        // Angular reactive form touched + invalid markers
        try {
            if (!driver.findElements(By.cssSelector(
                ".ng-invalid.ng-touched, .p-invalid, [aria-invalid='true']")).isEmpty())
                return true;
        } catch (Exception ignored) {}
        // Submit button still disabled (form invalid)
        if (isSubmitDisabled()) return true;
        return false;
    }

    private boolean pollForValidation(int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasValidationErrors()) return true;
            WaitUtils.hardWait(150);
        }
        return false;
    }
}
