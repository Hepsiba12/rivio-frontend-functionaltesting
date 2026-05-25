package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * PayrollFormPage – drives the two POST-method dialogs on /payroll
 * (Payroll Management page in Rivio_Angular-main).
 *
 * ── Dialog 1: Add Salary Component ───────────────────────────────────────────
 *   Opened by: "Add Component" button (enabled only after selecting an employee)
 *   Form fields (compForm in payroll-dashboard.ts):
 *     name  – input[formcontrolname='name']    placeholder "e.g. Basic Pay"
 *     type  – p-select[formcontrolname='type'] appendTo="body"
 *             options: "Earning (Adds to Gross Pay)" | "Deduction (Subtracts from Pay)"
 *     value – input[type='number'][formcontrolname='value']
 *   Submit:  button "Save"  [disabled]="compForm.invalid || isSubmitting()"
 *
 * ── Dialog 2: Initialize Pay Cycle ───────────────────────────────────────────
 *   Opened by: "Initialize Pay Cycle" button on "Pay Cycles & Slips" tab
 *   Form fields (cycleForm in payroll-dashboard.ts):
 *     name       – input[formcontrolname='name']      placeholder "e.g. April 2026"
 *     dateRange  – p-datepicker selectionMode="range" readonlyInput=true appendTo="body"
 *   Submit:  button "Initialize"  [disabled]="cycleForm.invalid || isSubmitting()"
 *
 * ComponentResult / CycleResult carry the outcome of each flow.
 */
public class PayrollFormPage {

    // ── Result DTOs ───────────────────────────────────────────────────────────

    public static class ComponentResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;

        public ComponentResult(boolean modalClosed, boolean validationVisible, boolean submitDisabled) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
        }

        @Override public String toString() {
            return "ComponentResult{closed=" + modalClosed
                 + ", validation=" + validationVisible
                 + ", submitDisabled=" + submitDisabled + "}";
        }
    }

    public static class CycleResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;

        public CycleResult(boolean modalClosed, boolean validationVisible, boolean submitDisabled) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
        }

        @Override public String toString() {
            return "CycleResult{closed=" + modalClosed
                 + ", validation=" + validationVisible
                 + ", submitDisabled=" + submitDisabled + "}";
        }
    }

    // ── Locators ──────────────────────────────────────────────────────────────

    // Employee selector on the Salary tab (ngModel p-select, NOT formControlName)
    private static final By EMPLOYEE_DROPDOWN = By.cssSelector(
        "p-select[placeholder*='Employee'], p-select[filterplaceholder*='Search']");

    // Add Component button (disabled until employee is selected)
    private static final By ADD_COMPONENT_BTN = By.xpath(
        "//button[contains(normalize-space(.),'Add Component')]");

    // Initialize Pay Cycle button (Pay Cycles tab)
    private static final By INIT_CYCLE_BTN = By.xpath(
        "//button[contains(normalize-space(.),'Initialize Pay Cycle')]");

    // Component modal form controls
    private static final By COMP_NAME_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='name']");
    private static final By COMP_TYPE_DROPDOWN = By.cssSelector(
        ".p-dialog [formcontrolname='type']");
    private static final By COMP_VALUE_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='value'], " +
        ".p-dialog input[type='number'][formcontrolname='value']");
    private static final By COMP_SAVE_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[normalize-space()='Save'] | " +
        "//button[normalize-space()='Save']");

    // Cycle modal form controls
    private static final By CYCLE_NAME_INPUT = By.cssSelector(
        ".p-dialog input[formcontrolname='name']");
    private static final By CYCLE_DATE_TRIGGER = By.cssSelector(
        ".p-dialog [formcontrolname='dateRange'] button, " +
        ".p-dialog [formcontrolname='dateRange'] .p-datepicker-trigger");
    private static final By CYCLE_INIT_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[normalize-space()='Initialize'] | " +
        "//button[normalize-space()='Initialize']");

    // Dialog detection – one for each modal (by header text)
    private static final By COMP_DIALOG_HEADER = By.xpath(
        "//*[contains(@class,'p-dialog-title')][contains(.,'Salary Component') or contains(.,'Edit Component')]");
    private static final By CYCLE_DIALOG_HEADER = By.xpath(
        "//*[contains(@class,'p-dialog-title')][contains(.,'Initialize Pay Cycle')]");

    // Calendar panel (appendTo=body)
    private static final By CALENDAR_PANEL = By.cssSelector(
        "body > .p-datepicker, body > div.p-datepicker, " +
        ".p-datepicker-panel, .p-overlay .p-datepicker");

    // Tab buttons
    private static final By SALARY_TAB = By.xpath(
        "//button[contains(normalize-space(.),'Employee Salaries')]");
    private static final By PAY_CYCLES_TAB = By.xpath(
        "//button[contains(normalize-space(.),'Pay Cycles')]");

    private final WebDriver driver;

    public PayrollFormPage(WebDriver driver) { this.driver = driver; }

    // ══════════════════════════════════════════════════════════════════════════
    // TAB NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    public void openSalaryTab() {
        try {
            WebElement tab = WaitUtils.waitForClickability(driver, SALARY_TAB);
            WaitUtils.scrollAndClick(driver, tab);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(400);
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] Could not click Salary tab: " + e.getMessage());
        }
    }

    public void openPayCyclesTab() {
        try {
            WebElement tab = WaitUtils.waitForClickability(driver, PAY_CYCLES_TAB);
            WaitUtils.scrollAndClick(driver, tab);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.waitForPresence(driver, INIT_CYCLE_BTN, 10);
            WaitUtils.hardWait(400);
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] Could not click Pay Cycles tab: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SALARY COMPONENT FLOW
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full salary-component add flow from a data-map row:
     *   employeeSearchText – text to type in the employee filter to select
     *   componentName      – text input value ({ts} already substituted)
     *   componentType      – p-select display label
     *   componentValue     – number string ("50000", "0", etc.)
     */
    public ComponentResult addSalaryComponentFlow(Map<String, String> row, int waitSeconds) {
        String empSearch   = row.getOrDefault("employeeSearchText", "");
        String name        = row.getOrDefault("componentName", "");
        String type        = row.getOrDefault("componentType", "");
        String valueStr    = row.getOrDefault("componentValue", "");

        // 1. Select employee if not already selected
        if (!empSearch.isEmpty()) {
            selectEmployee(empSearch);
            WaitUtils.hardWait(500);
        }

        // 2. Click "Add Component"
        try {
            WebElement addBtn = WaitUtils.waitForClickability(driver, ADD_COMPONENT_BTN);
            WaitUtils.safeClick(driver, addBtn);
            waitForComponentModal();
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] Could not open Add Component modal: " + e.getMessage());
        }

        // 3. Fill form fields
        if (!name.isEmpty()) {
            typeIntoDialogInput(COMP_NAME_INPUT, name);
        } else {
            clearDialogInput(COMP_NAME_INPUT);
        }

        if (!type.isEmpty()) {
            selectDropdownInDialog(COMP_TYPE_DROPDOWN, type);
            WaitUtils.hardWait(300);
        }

        if (!valueStr.isEmpty()) {
            typeNumberIntoDialogInput(COMP_VALUE_INPUT, valueStr);
        }

        // 4. Capture disabled state before submit
        boolean submitDisabled = isButtonDisabled(COMP_SAVE_BTN);

        // 5. Click Save
        clickDialogButton(COMP_SAVE_BTN);
        WaitUtils.hardWait(600);

        // 6. Fast validation check + modal close check
        boolean validation = pollForValidation(COMP_DIALOG_HEADER, 3);
        boolean closed     = waitForModalClose(COMP_DIALOG_HEADER, waitSeconds);

        return new ComponentResult(closed, validation, submitDisabled);
    }

    // ── Employee selector ─────────────────────────────────────────────────────

    /**
     * Selects an employee from the p-select with [filter] on the Salary tab.
     * The employee p-select uses [(ngModel)] (not formControlName) and has
     * a filter input inside its panel.
     */
    public void selectEmployee(String searchText) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                // Open the dropdown
                WebElement dropdown = WaitUtils.waitForClickability(driver, EMPLOYEE_DROPDOWN);
                WaitUtils.safeClick(driver, dropdown);
                WaitUtils.hardWait(400);

                // Type into the filter input inside the panel
                By filterInput = By.cssSelector(
                    ".p-select-filter, input.p-select-filter, " +
                    ".p-select-panel input, .p-dropdown-filter, " +
                    ".p-overlay input[type='text']");

                try {
                    WebElement filter = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.visibilityOfElementLocated(filterInput));
                    filter.clear();
                    filter.sendKeys(searchText);
                    WaitUtils.hardWait(400);
                } catch (Exception filterEx) {
                    // Some PrimeNG versions auto-filter without a separate input
                }

                // Click the first visible option
                By optionLocator = By.cssSelector(
                    ".p-select-option, .p-dropdown-item, li[class*='select-option']");

                List<WebElement> options = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(optionLocator));

                if (!options.isEmpty()) {
                    options.get(0).click();
                    WaitUtils.hardWait(400);
                    return;
                }
            } catch (StaleElementReferenceException stale) {
                WaitUtils.hardWait(300);
            } catch (Exception e) {
                System.err.println("[PayrollFormPage] selectEmployee attempt " + (attempt+1)
                    + " failed: " + e.getMessage());
                WaitUtils.hardWait(300);
            }
            dismissOpenPanel();
        }
        System.err.println("[PayrollFormPage] Could not select employee with text: " + searchText);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAY CYCLE FLOW
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full pay-cycle initialization flow from a data-map row:
     *   cycleName  – text input value ({ts} already substituted)
     *   startDate  – YYYY-MM-DD
     *   endDate    – YYYY-MM-DD
     */
    public CycleResult initializePayCycleFlow(Map<String, String> row, int waitSeconds) {
        String cycleName = row.getOrDefault("cycleName", "");
        String startDate = row.getOrDefault("startDate", "");
        String endDate   = row.getOrDefault("endDate", "");

        // 1. Click "Initialize Pay Cycle"
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, INIT_CYCLE_BTN);
            WaitUtils.safeClick(driver, btn);
            waitForCycleModal();
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] Could not open Pay Cycle modal: " + e.getMessage());
        }

        // 2. Fill cycle name
        if (!cycleName.isEmpty()) {
            typeIntoDialogInput(CYCLE_NAME_INPUT, cycleName);
        } else {
            clearDialogInput(CYCLE_NAME_INPUT);
        }

        // 3. Select date range
        if (!startDate.isEmpty() && !endDate.isEmpty()) {
            selectCycleDateRange(startDate, endDate);
            WaitUtils.hardWait(300);
        }

        // 4. Capture disabled state
        boolean submitDisabled = isButtonDisabled(CYCLE_INIT_BTN);

        // 5. Click Initialize
        clickDialogButton(CYCLE_INIT_BTN);
        WaitUtils.hardWait(600);

        // 6. Validation + close checks
        boolean validation = pollForValidation(CYCLE_DIALOG_HEADER, 3);
        boolean closed     = waitForModalClose(CYCLE_DIALOG_HEADER, waitSeconds);

        return new CycleResult(closed, validation, submitDisabled);
    }

    // ── Date range for pay cycle (readonlyInput=true, appendTo=body) ──────────

    private void selectCycleDateRange(String startDate, String endDate) {
        openCalendarPanel(CYCLE_DATE_TRIGGER);
        WaitUtils.hardWait(500);

        clickDateInPanel(startDate);
        WaitUtils.hardWait(400);

        if (!isCalendarPanelOpen()) {
            openCalendarPanel(CYCLE_DATE_TRIGGER);
            WaitUtils.hardWait(400);
        }
        clickDateInPanel(endDate);
        WaitUtils.hardWait(300);

        dismissOpenPanel();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHARED FORM HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Types text into a dialog input, replacing any existing value. */
    private void typeIntoDialogInput(By locator, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, locator);
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(text);
            el.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] typeIntoDialogInput failed for " + locator + ": " + e.getMessage());
        }
    }

    /** Clears a dialog input (fires blur so Angular marks it as touched). */
    private void clearDialogInput(By locator) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, locator);
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] clearDialogInput failed: " + e.getMessage());
        }
    }

    /** Sets a number input — clears and sends the string value. */
    private void typeNumberIntoDialogInput(By locator, String value) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, locator);
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(value);
            el.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] typeNumberIntoDialogInput failed: " + e.getMessage());
        }
    }

    /**
     * Selects an option from a PrimeNG p-select inside a dialog.
     * The dropdown has appendTo="body" so its panel renders outside the dialog.
     */
    private void selectDropdownInDialog(By dropdownLocator, String optionText) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement dropdown = WaitUtils.waitForClickability(driver, dropdownLocator);
                WaitUtils.safeClick(driver, dropdown);
                WaitUtils.hardWait(400);

                By optionBy = By.xpath(
                    "//li[contains(@class,'p-select-option') and normalize-space(.)='" + optionText + "'] | " +
                    "//li[contains(@class,'p-dropdown-item') and normalize-space(.)='" + optionText + "']");

                try {
                    WebElement opt = new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.elementToBeClickable(optionBy));
                    opt.click();
                    WaitUtils.hardWait(200);
                    return;
                } catch (Exception e) {
                    // Fallback to overlay panel text search
                    By fallback = By.xpath(
                        "//*[contains(@class,'p-overlay') or contains(@class,'p-select-panel') " +
                        "or contains(@class,'p-dropdown-panel')]" +
                        "//*[normalize-space(.)='" + optionText + "']");
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
        System.err.println("[PayrollFormPage] Could not select option: " + optionText);
    }

    private void clickDialogButton(By locator) {
        try {
            WebElement btn = WaitUtils.waitForVisibility(driver, locator);
            try { btn.click(); }
            catch (Exception e) { WaitUtils.jsClick(driver, btn); }
        } catch (Exception e) {
            System.err.println("[PayrollFormPage] clickDialogButton failed for " + locator + ": " + e.getMessage());
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

    // ── Modal open waits ──────────────────────────────────────────────────────

    private void waitForComponentModal() {
        WaitUtils.waitForPresence(driver, COMP_DIALOG_HEADER, 10);
        WaitUtils.hardWait(400);
    }

    private void waitForCycleModal() {
        WaitUtils.waitForPresence(driver, CYCLE_DIALOG_HEADER, 10);
        WaitUtils.hardWait(400);
    }

    private boolean waitForModalClose(By headerLocator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(headerLocator));
            return true;
        } catch (Exception e) { return false; }
    }

    // ── Validation detection ──────────────────────────────────────────────────

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
            // Also check if dialog closed (positive) — stop early
            try {
                if (driver.findElements(dialogHeaderLocator).isEmpty()) return false;
            } catch (Exception ignored) {}
            WaitUtils.hardWait(150);
        }
        return hasValidationErrors();
    }

    // ── Calendar helpers (shared by pay cycle date range) ────────────────────

    private void openCalendarPanel(By triggerLocator) {
        for (int i = 0; i < 2; i++) {
            try {
                List<WebElement> btns = driver.findElements(triggerLocator);
                if (!btns.isEmpty()) {
                    WaitUtils.scrollAndClick(driver, btns.get(0));
                    WaitUtils.hardWait(300);
                    if (isCalendarPanelOpen()) return;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: click the input itself
        try {
            WebElement inp = driver.findElement(
                By.cssSelector(".p-dialog [formcontrolname='dateRange'] input"));
            WaitUtils.safeClick(driver, inp);
        } catch (Exception ignored) {}
    }

    private boolean isCalendarPanelOpen() {
        try {
            List<WebElement> panels = driver.findElements(CALENDAR_PANEL);
            return panels.stream().anyMatch(WebElement::isDisplayed);
        } catch (Exception e) { return false; }
    }

    private void clickDateInPanel(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.isEmpty()) return;
        String[] parts = yyyyMmDd.split("-");
        int targetYear  = Integer.parseInt(parts[0]);
        int targetMonth = Integer.parseInt(parts[1]);
        int targetDay   = Integer.parseInt(parts[2]);

        for (int i = 0; i < 24; i++) {
            int[] cur = readCalendarMonthYear();
            if (cur == null) break;
            if (cur[0] == targetYear && cur[1] == targetMonth) break;
            boolean fwd = (cur[0] < targetYear) || (cur[0] == targetYear && cur[1] < targetMonth);
            clickCalendarNav(fwd);
            WaitUtils.hardWait(300);
        }
        clickDayCell(targetDay);
    }

    private int[] readCalendarMonthYear() {
        String[] monthSels = {".p-datepicker-month-title", "button.p-datepicker-month",
                              ".p-datepicker-title span:first-child"};
        String[] yearSels  = {".p-datepicker-year-title", "button.p-datepicker-year",
                              ".p-datepicker-title span:last-child"};
        String monthText = null, yearText = null;
        for (String s : monthSels) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(s));
                if (!els.isEmpty()) { monthText = els.get(0).getText().trim(); if (!monthText.isEmpty()) break; }
            } catch (Exception ignored) {}
        }
        for (String s : yearSels) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(s));
                if (!els.isEmpty()) { yearText = els.get(0).getText().trim(); if (!yearText.isEmpty()) break; }
            } catch (Exception ignored) {}
        }
        if (monthText == null || yearText == null) return null;
        try {
            int year = Integer.parseInt(yearText.replaceAll("[^0-9]",""));
            return new int[]{year, parseMonthName(monthText)};
        } catch (Exception e) { return null; }
    }

    private int parseMonthName(String name) {
        String n = name.trim().toUpperCase();
        String[] m = {"JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
                      "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"};
        for (int i = 0; i < m.length; i++) {
            if (m[i].startsWith(n.substring(0, Math.min(3, n.length())))) return i + 1;
        }
        return 1;
    }

    private void clickCalendarNav(boolean forward) {
        String[] sels = forward
            ? new String[]{".p-datepicker-next-button",".p-datepicker-next","button[aria-label='Next Month']"}
            : new String[]{".p-datepicker-prev-button",".p-datepicker-prev","button[aria-label='Previous Month']"};
        for (String sel : sels) {
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

    private void clickDayCell(int day) {
        String dayStr = String.valueOf(day);
        By[] locators = {
            By.xpath("//div[contains(@class,'p-datepicker')]" +
                     "//td[not(contains(@class,'p-disabled'))][not(contains(@class,'p-datepicker-other-month'))]" +
                     "//span[normalize-space()='" + dayStr + "' and not(contains(@class,'p-disabled'))]"),
            By.xpath("//div[contains(@class,'p-datepicker')]" +
                     "//td[not(@data-p-other-month='true')][not(@data-p-disabled='true')]" +
                     "//span[normalize-space()='" + dayStr + "']"),
        };
        for (By by : locators) {
            try {
                List<WebElement> cells = driver.findElements(by);
                for (WebElement cell : cells) {
                    if (cell.isDisplayed()) {
                        WaitUtils.scrollAndClick(driver, cell);
                        WaitUtils.hardWait(200);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
        System.err.println("[PayrollFormPage] Could not click day cell: " + day);
    }

    private void dismissOpenPanel() {
        try {
            if (isCalendarPanelOpen()) {
                driver.findElement(By.cssSelector("body")).sendKeys(Keys.ESCAPE);
                WaitUtils.hardWait(200);
            }
        } catch (Exception ignored) {}
        try {
            // Also close any PrimeNG select/dropdown panel that may still be open
            List<WebElement> panels = driver.findElements(By.cssSelector(
                ".p-select-panel, .p-dropdown-panel, .p-overlay"));
            for (WebElement p : panels) {
                try { if (p.isDisplayed()) p.sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ── Public close helpers ──────────────────────────────────────────────────

    public void closeComponentModal() {
        try {
            WebElement cancelBtn = driver.findElement(By.xpath(
                "//div[contains(@class,'p-dialog')]//button[contains(.,'Cancel')]"));
            WaitUtils.safeClick(driver, cancelBtn);
            WaitUtils.hardWait(300);
        } catch (Exception ignored) {}
    }

    public void closeCycleModal() {
        try {
            WebElement cancelBtn = driver.findElement(By.xpath(
                "//div[contains(@class,'p-dialog')]//button[contains(.,'Cancel')]"));
            WaitUtils.safeClick(driver, cancelBtn);
            WaitUtils.hardWait(300);
        } catch (Exception ignored) {}
    }

    public boolean isComponentModalOpen() {
        return !driver.findElements(COMP_DIALOG_HEADER).isEmpty();
    }

    public boolean isCycleModalOpen() {
        return !driver.findElements(CYCLE_DIALOG_HEADER).isEmpty();
    }
}
