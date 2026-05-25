package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * EmployeeOnboardPage — wraps the "Onboard New Employee" p-dialog opened from
 * the Employees page (Admin > Add Employee).
 *
 * Field-lookup waits are intentionally short (2-3s). Selenium's default
 * explicit wait is 20s; without an upper bound a missing field would block
 * the whole data-driven run for several minutes.
 */
public class EmployeeOnboardPage {

    private final WebDriver driver;

    /** Short timeout for individual modal fields — keeps the data-driven run fast. */
    private static final int FIELD_TIMEOUT_SEC = 3;

    public EmployeeOnboardPage(WebDriver driver) { this.driver = driver; }

    // ── State checks ─────────────────────────────────────────────────────────
    //
    // IMPORTANT — PrimeNG portal behavior:
    //   <p-dialog [modal]="true"> moves its content into the document body as
    //   <div class="p-dialog-mask"><div class="p-dialog">… The original
    //   <p-dialog> tag stays in the template's DOM position but is EMPTY.
    //   That means any selector starting with `p-dialog X` finds nothing.
    //   Below, we use `.p-dialog` (the rendered class) or raw form-control
    //   selectors that don't need scoping.

    /** CSS for the rendered dialog wrapper (after PrimeNG portals it). */
    private static final String DIALOG = ".p-dialog";

    public boolean isModalOpen() {
        return !driver.findElements(By.cssSelector(DIALOG)).isEmpty();
    }

    /** Dialog is gone — typically means submit succeeded. */
    public boolean isModalClosed() {
        return driver.findElements(By.cssSelector(DIALOG)).isEmpty();
    }

    /** Polls until the dialog actually disappears (success) or `seconds` elapse. */
    public boolean waitForModalClose(int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds), Duration.ofMillis(250))
                .until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(DIALOG)));
            return true;
        } catch (Exception e) {
            return isModalClosed();
        }
    }

    public boolean hasThreeSections() {
        long sections = driver.findElements(By.xpath(
            "//div[contains(@class,'p-dialog')]//h3")).size();
        return sections >= 3;
    }

    /**
     * Wait for the modal to be FULLY rendered. Polls for both the submit
     * button AND a form input to be present — which collectively indicate
     * the reactive form has actually bound.
     */
    public boolean waitForReady(int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            boolean submit = !driver.findElements(submitButtonLocator()).isEmpty();
            boolean input  = !driver.findElements(
                By.cssSelector("input[formcontrolname='email']")).isEmpty();
            if (submit && input) {
                WaitUtils.hardWait(250); // grace for last reactive-form bindings
                return true;
            }
            WaitUtils.hardWait(150);
        }
        return false;
    }

    // ── Section 1 — Account Credentials ──────────────────────────────────────

    public void fillEmail(String email) {
        // <input pInputText type="email" formControlName="email">
        typeIntoFirst(email,
            By.cssSelector("input[formcontrolname='email']"),
            By.cssSelector("input[type='email']"),
            By.cssSelector("input[placeholder*='@rivio' i]"),
            By.cssSelector("input[placeholder*='Email' i]"));
    }

    public void fillPassword(String pwd) {
        // <p-password formControlName="password"> renders an <input type="password">
        // INSIDE the host. The formControlName lives on <p-password>, not on the
        // inner input, so we target the inner input directly.
        typeIntoFirst(pwd,
            By.cssSelector("p-password input"),
            By.cssSelector("input[type='password']"),
            By.cssSelector("input[placeholder*='Password' i]"));
    }

    public void selectRole(String roleName) {
        // Real form control name in the Angular template is `roleId`, not `role`.
        // The displayed text on each option is the role's `name` (e.g. "Employee").
        selectDropdownByLabel("roleId", roleName);
    }

    // ── Section 2 — Personal Identity ────────────────────────────────────────

    public void fillFirstName(String n) {
        typeIntoFirst(n,
            By.cssSelector("input[formcontrolname='firstName']"),
            By.cssSelector("input[placeholder='Jane']"),
            By.cssSelector("input[placeholder*='First' i]"));
    }
    public void fillLastName(String n) {
        typeIntoFirst(n,
            By.cssSelector("input[formcontrolname='lastName']"),
            By.cssSelector("input[placeholder='Doe']"),
            By.cssSelector("input[placeholder*='Last' i]"));
    }
    public void fillEmployeeCode(String c) {
        typeIntoFirst(c,
            By.cssSelector("input[formcontrolname='employeeCode']"),
            By.cssSelector("input[placeholder*='EMP-' i]"),
            By.cssSelector("input[placeholder*='Employee Code' i]"),
            By.cssSelector("input[placeholder*='Code' i]"));
    }

    // ── Section 3 — Org Role ─────────────────────────────────────────────────

    // Real form control names in the Angular template are *Id versions —
    // verified against
    // https://github.com/miteshp2110/Rivio_Angular/blob/main/src/app/features/employees/employee-onboard/employee-onboard.html
    public void selectDepartment(String name)     { selectDropdownByLabel("departmentId", name); }
    public void selectDesignation(String name) {
        // Designation field is `[disabled]="!departmentId.value"` in the template,
        // so we MUST wait for it to become interactive after picking Department.
        waitForDesignationEnabled(3);
        selectDropdownByLabel("designationId", name);
    }
    public void selectLocation(String name)       { selectDropdownByLabel("locationId", name); }
    public void selectEmploymentType(String name) { selectDropdownByLabel("employmentType", name); }

    /** Block until the Designation p-select is no longer disabled. */
    private void waitForDesignationEnabled(int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> hits = driver.findElements(
                By.cssSelector("p-select[formcontrolname='designationId']"));
            if (!hits.isEmpty()) {
                String cls  = hits.get(0).getAttribute("class");
                String aria = hits.get(0).getAttribute("aria-disabled");
                boolean disabled =
                    (cls != null && cls.contains("p-disabled")) ||
                    "true".equalsIgnoreCase(aria);
                if (!disabled) return;
            }
            WaitUtils.hardWait(150);
        }
    }

    public void fillJoiningDate(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.isEmpty()) return;

        // PrimeNG datepicker default format is MM/DD/YYYY.
        // Convert from YYYY-MM-DD if that's what the test data carries.
        String dateToType = yyyyMmDd;
        if (yyyyMmDd.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = yyyyMmDd.split("-");
            dateToType = p[1] + "/" + p[2] + "/" + p[0]; // 06/01/2026
        }

        By[] locators = {
            By.cssSelector("p-datepicker input"),
            By.cssSelector("p-calendar input"),
            By.cssSelector("input[formcontrolname='joiningDate']")
        };

        for (int attempt = 0; attempt < 3; attempt++) {
            for (By by : locators) {
                List<WebElement> hits = driver.findElements(by);
                if (hits.isEmpty()) continue;
                WebElement el = hits.get(0);
                try {
                    scrollIntoView(el);
                    el.click();
                    WaitUtils.hardWait(100);
                    // Select-all + replace (avoids clear() which can null the Angular model)
                    el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    WaitUtils.hardWait(50);
                    el.sendKeys(dateToType);
                    WaitUtils.hardWait(150);
                    el.sendKeys(Keys.TAB); // confirm + blur (closes calendar panel safely)
                    return;
                } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                    WaitUtils.hardWait(200);
                    break;
                } catch (Exception e) {
                    try { WaitUtils.jsSetValue(driver, el, dateToType); } catch (Exception ignored) {}
                    return;
                }
            }
        }
    }

    // ── Submit / validate ────────────────────────────────────────────────────

    public void clickCompleteOnboarding() {
        try {
            WebElement btn = new WebDriverWait(driver,
                    Duration.ofSeconds(FIELD_TIMEOUT_SEC), Duration.ofMillis(250))
                .until(ExpectedConditions.elementToBeClickable(submitButtonLocator()));
            WaitUtils.safeClick(driver, btn);
        } catch (Exception ignored) {}
    }

    /** True if the submit button is currently disabled (i.e. the form is invalid). */
    public boolean isSubmitDisabled() {
        try {
            WebElement btn = driver.findElement(submitButtonLocator());
            String dis = btn.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }

    /**
     * True only when a visible error/help text or a touched-and-invalid control
     * is present. Plain `.ng-invalid` without `.ng-touched` is ignored — Angular
     * paints `ng-invalid` on every required field at startup and we don't want
     * that to look like a real error.
     */
    public boolean hasValidationErrors() {
        return !driver.findElements(By.cssSelector(
            ".p-dialog .ng-invalid.ng-touched, "
          + ".p-dialog .p-invalid.ng-touched, "
          + ".p-dialog small.p-error, "
          + ".p-dialog .field-error, "
          + ".p-dialog [role='alert'], "
          + ".p-dialog .bg-rose-50")).isEmpty();
    }

    /**
     * Polls hasValidationErrors() up to `seconds` seconds at 100ms intervals.
     * Returns true as soon as validation errors are visible — lets negative-case
     * rows exit within ~300ms instead of waiting the full waitForCloseSeconds.
     */
    public boolean pollForValidationErrors(int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasValidationErrors()) return true;
            WaitUtils.hardWait(100);
        }
        return false;
    }

    public void closeIfOpen() {
        try {
            WebElement x = driver.findElement(By.cssSelector(
                ".p-dialog-header-close, .p-dialog button[aria-label='Close']"));
            WaitUtils.safeClick(driver, x);
        } catch (Exception ignored) {}
        // Click the Cancel button (the template's explicit <button (click)="close()">)
        try {
            WebElement cancel = driver.findElement(By.xpath(
                "//div[contains(@class,'p-dialog')]//button[normalize-space()='Cancel']"));
            WaitUtils.safeClick(driver, cancel);
        } catch (Exception ignored) {}
        // ESC as last resort
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {}
    }

    // ── Bulk fill from a data row (DataProvider-friendly) ────────────────────

    /**
     * Fills every field in the modal from a Map keyed by the Excel column names
     * defined in {@link com.cts.rivio.utils.AddEmployeeDataBuilder}.
     * Blank values are sent as-is so negative cases (empty email, etc.) still test validators.
     */
    public void fillForm(Map<String, String> row) {
        // Text inputs first — keep them grouped so reactive-form re-renders
        // don't keep invalidating WebElement refs we'd resolve later.
        fillEmail(row.getOrDefault("email", ""));
        fillPassword(row.getOrDefault("password", ""));
        fillFirstName(row.getOrDefault("firstName", ""));
        fillLastName(row.getOrDefault("lastName", ""));
        fillEmployeeCode(row.getOrDefault("employeeCode", ""));

        // Dropdowns next. Each dropdown selection fires Angular change
        // detection, which re-renders dependent inputs (designation depends
        // on department, etc.). A short settle wait between dropdowns
        // prevents stale references on the NEXT lookup.
        selectRole(row.getOrDefault("role", ""));
        WaitUtils.hardWait(200);
        selectDepartment(row.getOrDefault("department", ""));
        WaitUtils.hardWait(250); // designation depends on department
        selectDesignation(row.getOrDefault("designation", ""));
        WaitUtils.hardWait(200);
        selectLocation(row.getOrDefault("location", ""));
        WaitUtils.hardWait(200);
        selectEmploymentType(row.getOrDefault("employmentType", ""));
        WaitUtils.hardWait(150);

        fillJoiningDate(row.getOrDefault("joiningDate", ""));
    }

    // ── End-to-end add-employee flow ─────────────────────────────────────────

    /** Outcome bundle for one Add Employee attempt — used by data-driven tests. */
    public static class AddResult {
        public final boolean modalClosed;
        public final boolean validationVisible;
        public final boolean submitDisabled;
        public final Map<String, String> capturedValues;

        public AddResult(boolean modalClosed, boolean validationVisible,
                         boolean submitDisabled, Map<String, String> capturedValues) {
            this.modalClosed       = modalClosed;
            this.validationVisible = validationVisible;
            this.submitDisabled    = submitDisabled;
            this.capturedValues    = capturedValues;
        }

        @Override
        public String toString() {
            return "AddResult{modalClosed=" + modalClosed
                 + ", validationVisible=" + validationVisible
                 + ", submitDisabled=" + submitDisabled
                 + ", captured=" + capturedValues + "}";
        }
    }

    /**
     * Drive the entire Add Employee flow from a data row:
     *   fillForm → readback the values actually committed →
     *   click submit → wait up to {@code waitForCloseSeconds} for the dialog
     *   to disappear → return everything we observed so the test can decide.
     *
     * Does NOT throw on any field-level miss — it returns the partial state
     * so the test can log exactly what got filled.
     */
    public AddResult addEmployeeFlow(Map<String, String> row, int waitForCloseSeconds) {
        // Critical: don't try to fill the form until Angular has finished
        // mounting it inside the dialog. Without this, the first row's
        // fillForm finds no inputs at all and every value is dropped.
        waitForReady(8);

        fillForm(row);
        WaitUtils.hardWait(300); // allow Angular to commit dropdown values

        Map<String, String> captured = new java.util.LinkedHashMap<>();
        captured.put("email",          readInputValue("input[formcontrolname='email']",
                                                     "input[type='email']"));
        captured.put("password",       readInputValue("p-password input",
                                                     "input[type='password']"));
        captured.put("role",           readDropdownValue("roleId"));
        captured.put("firstName",      readInputValue("input[formcontrolname='firstName']"));
        captured.put("lastName",       readInputValue("input[formcontrolname='lastName']"));
        captured.put("employeeCode",   readInputValue("input[formcontrolname='employeeCode']"));
        captured.put("department",     readDropdownValue("departmentId"));
        captured.put("designation",    readDropdownValue("designationId"));
        captured.put("location",       readDropdownValue("locationId"));
        captured.put("employmentType", readDropdownValue("employmentType"));
        captured.put("joiningDate",    readInputValue("p-datepicker input",
                                                     "p-calendar input",
                                                     "input[formcontrolname='joiningDate']"));

        boolean submitDisabledBefore = isSubmitDisabled();
        clickCompleteOnboarding();

        // Fast-path for negative cases: Angular calls markAllAsTouched() and
        // returns from onSubmit() within ~300ms when the form is invalid.
        // Detect that early so we don't waste waitForCloseSeconds per row.
        if (pollForValidationErrors(3)) {
            boolean stillOpen = isModalOpen();
            return new AddResult(!stillOpen, true, isSubmitDisabled(), captured);
        }

        // No quick validation errors → positive case (API call in flight).
        // Wait for the dialog to close on success.
        boolean closed = waitForModalClose(waitForCloseSeconds);
        boolean errors = !closed && hasValidationErrors();
        boolean disabled = !closed && isSubmitDisabled();

        return new AddResult(closed, errors, disabled || submitDisabledBefore, captured);
    }

    /** Read the current text-input value from the first selector that exists. */
    private String readInputValue(String... cssSelectors) {
        for (String css : cssSelectors) {
            List<WebElement> hits = driver.findElements(By.cssSelector(css));
            if (!hits.isEmpty()) {
                String v = hits.get(0).getAttribute("value");
                return v == null ? "" : v;
            }
        }
        return "";
    }

    /** Read the visible label on a PrimeNG dropdown by formcontrolname. */
    private String readDropdownValue(String fc) {
        WebElement host = resolveDropdownHost(fc);
        if (host == null) return "";
        try {
            List<WebElement> labels = host.findElements(By.cssSelector(
                ".p-dropdown-label, .p-select-label"));
            if (!labels.isEmpty()) {
                String t = labels.get(0).getText();
                if (t == null) return "";
                String trimmed = t.trim();
                // PrimeNG renders the placeholder inside the same span when no
                // value is picked — surface that as empty so the test can tell.
                if (trimmed.toLowerCase().startsWith("select") ||
                    trimmed.isEmpty()) return "";
                return trimmed;
            }
        } catch (Exception ignored) {}
        return "";
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private By submitButtonLocator() {
        // Scope to the rendered dialog wrapper (PrimeNG portals it out of the
        // <p-dialog> tag). Match button by visible text or by submit type.
        return By.xpath(
            "//div[contains(@class,'p-dialog')]//button[@type='submit'] | "
          + "//div[contains(@class,'p-dialog')]//button[contains(normalize-space(.),'Complete Onboarding')] | "
          + "//div[contains(@class,'p-dialog')]//button[normalize-space(.)='Save Changes']");
    }

    /**
     * Stale-tolerant text-field setter. Re-resolves the element on every
     * attempt, so an Angular re-render between findElements() and use just
     * triggers a retry instead of failing the whole row.
     *
     * Tries each By locator in order on each attempt. Returns true if the
     * value was committed (or text was blank/null and we intentionally
     * skipped). Returns false only if no locator matched after retries.
     */
    private boolean typeIntoFirst(String text, By... locators) {
        // Blank value: no-op (don't disturb other fields).
        if (text == null) return true;

        int attempts = 4;
        for (int i = 0; i < attempts; i++) {
            try {
                for (By by : locators) {
                    List<WebElement> hits = driver.findElements(by);
                    if (hits.isEmpty()) continue;
                    WebElement el = hits.get(0);

                    // scroll into view to avoid not-interactable
                    try {
                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center'});", el);
                    } catch (Exception ignored) {}

                    if (text.isEmpty()) {
                        // Click, wipe, then TAB so Angular fires blur and marks the
                        // control as ng-touched — required for validation to show.
                        try {
                            el.click();
                            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                            el.sendKeys(Keys.DELETE);
                            el.sendKeys(Keys.TAB);
                        } catch (Exception ignored) {}
                        return true;
                    }

                    // Try a native clear+sendKeys first; fall back to JS value-set.
                    try {
                        el.clear();
                        el.sendKeys(text);
                    } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                        throw stale; // bubble up so the outer loop retries
                    } catch (Exception e) {
                        try {
                            WaitUtils.jsSetValue(driver, el, text);
                        } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                            throw stale;
                        }
                    }
                    return true;
                }
                // No locator matched at all — short wait, try again
                WaitUtils.hardWait(250);
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                // Angular re-rendered between findElements and use — retry.
                WaitUtils.hardWait(200);
            }
        }
        return false;
    }

    /**
     * Robust PrimeNG dropdown picker that handles BOTH `p-dropdown` (v15-16)
     * and `p-select` (v17+). One pass through it:
     *
     *   1. Resolve the dropdown host via formcontrolname, with several fallbacks
     *      (also looks at the field's <label for=…> sibling).
     *   2. Scroll into view, then click the visible trigger area (the label/chevron),
     *      not the host element itself — clicking the host is a no-op in
     *      modern PrimeNG.
     *   3. Wait for the overlay panel to appear ANYWHERE on the page (CDK
     *      attaches it to <body>, not inside p-dialog).
     *   4. Find the option whose normalized text equals `label`. PrimeNG often
     *      wraps option text in <span>, so we match on the whole node, not
     *      just text().
     *   5. JS-click fallback when a native click is intercepted by the
     *      cdk-overlay-backdrop.
     *   6. Retry the whole flow once if the dropdown's own label didn't update.
     */
    private void selectDropdownByLabel(String fc, String label) {
        if (label == null || label.isEmpty()) return;

        for (int attempt = 0; attempt < 3; attempt++) {
            WebElement host = resolveDropdownHost(fc);
            if (host == null) {
                WaitUtils.hardWait(300);
                continue;
            }
            scrollIntoView(host);

            // Strategy 1 (preferred — the user asked for sendKeys-driven picks):
            // Focus the combobox span and use PrimeNG's built-in keyboard
            // typeahead. This works whether or not the panel is currently open.
            if (selectByTypeahead(host, label)) return;

            // Strategy 2: open the panel and click the matching <li>.
            // Wrapped in stale-catch: Angular can re-render the overlay list
            // between findElements() and the JS-click fallback, making the
            // option reference stale. Catch it here and let the outer loop retry.
            try {
                if (openDropdown(host) && clickPanelOption(label)) {
                    WaitUtils.hardWait(200);
                    if (dropdownShows(host, label)) return;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                WaitUtils.hardWait(300); // panel re-rendered; outer loop will retry
            }

            // Strategy 3: panel still didn't take. Dismiss and retry from scratch.
            dismissOpenPanel();
            WaitUtils.hardWait(200);
        }
    }

    /**
     * SendKeys-based dropdown selection.
     *
     * The PrimeNG p-select renders a `<span role="combobox" tabindex="0">`
     * inside the host. We:
     *   1. Click it to focus.
     *   2. Press Enter / Space to open the listbox (some PrimeNG builds open
     *      on focus, others require an explicit key — sending both is safe).
     *   3. Type the option text one character at a time. PrimeNG's combobox
     *      typeahead jumps the highlight to the first option whose label
     *      starts with the typed prefix.
     *   4. Press Enter to commit the highlighted option.
     *   5. Verify by reading back the displayed label.
     */
    private boolean selectByTypeahead(WebElement host, String label) {
        WebElement combobox;
        try {
            List<WebElement> hits = host.findElements(By.cssSelector(
                ".p-select-label[role='combobox'], "
              + ".p-dropdown-label[role='combobox'], "
              + "[role='combobox']"));
            if (hits.isEmpty()) return false;
            combobox = hits.get(0);
        } catch (Exception e) { return false; }

        try {
            combobox.click();
        } catch (Exception e) {
            try { WaitUtils.jsClick(driver, combobox); } catch (Exception ignored) {}
        }
        WaitUtils.hardWait(150);

        // Open the listbox explicitly — covers both "open-on-focus" and
        // "open-on-Enter" PrimeNG builds.
        try { combobox.sendKeys(Keys.ENTER); } catch (Exception ignored) {}
        WaitUtils.hardWait(150);
        if (!panelHasOptions()) {
            try { combobox.sendKeys(Keys.SPACE); } catch (Exception ignored) {}
            WaitUtils.hardWait(150);
        }
        if (!panelHasOptions()) {
            try { combobox.sendKeys(Keys.ARROW_DOWN); } catch (Exception ignored) {}
            WaitUtils.hardWait(150);
        }

        // Type the label one character at a time so PrimeNG's typeahead
        // navigates. Reset the prefix buffer between attempts by waiting
        // a moment first.
        WaitUtils.hardWait(200);
        try {
            for (char c : label.toCharArray()) {
                combobox.sendKeys(String.valueOf(c));
                WaitUtils.hardWait(40);
            }
        } catch (Exception e) {
            return false;
        }

        WaitUtils.hardWait(200);
        try { combobox.sendKeys(Keys.ENTER); } catch (Exception ignored) {}
        WaitUtils.hardWait(200);

        return dropdownShows(host, label);
    }

    /** Find the <p-dropdown> / <p-select> for a given form control name. */
    private WebElement resolveDropdownHost(String fc) {
        // No `p-dialog ` prefix: PrimeNG portals the dialog content under
        // <body>, so the form controls are NOT descendants of <p-dialog>.
        // Raw form-control selectors are unique enough — only one onboarding
        // form is mounted at a time.
        By[] candidates = new By[] {
            By.cssSelector("p-select[formcontrolname='"   + fc + "']"),
            By.cssSelector("p-dropdown[formcontrolname='" + fc + "']"),
            By.xpath("//p-select[.//*[@formcontrolname='"   + fc + "']]"),
            By.xpath("//p-dropdown[.//*[@formcontrolname='" + fc + "']]")
        };
        for (By by : candidates) {
            List<WebElement> hits = driver.findElements(by);
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    /** Click the inner trigger area of a PrimeNG dropdown host. */
    private boolean openDropdown(WebElement host) {
        // Already open? Skip the click.
        if (panelHasOptions()) return true;

        // The clickable surface is the label/chevron, NOT the host element.
        By[] triggers = new By[] {
            By.cssSelector(".p-dropdown-label, .p-select-label"),
            By.cssSelector(".p-dropdown-trigger, .p-select-trigger"),
            By.cssSelector("[role='combobox']")
        };
        WebElement target = null;
        for (By by : triggers) {
            List<WebElement> hits = host.findElements(by);
            if (!hits.isEmpty()) { target = hits.get(0); break; }
        }
        if (target == null) target = host; // last resort

        try {
            target.click();
        } catch (Exception e) {
            WaitUtils.jsClick(driver, target);
        }

        // Wait up to 5s for the panel to render with at least one option.
        // (On the first open the option list is fetched from the backend, so
        //  the <li> children appear ~300-1500ms after the panel opens.)
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (panelHasOptions()) return true;
            WaitUtils.hardWait(150);
        }
        return false;
    }

    /** True when a PrimeNG dropdown panel is rendered AND contains at least one option. */
    private boolean panelHasOptions() {
        return !driver.findElements(By.cssSelector(
            // Generic ARIA — most reliable, works for any combobox impl
            "[role='listbox'] [role='option'], "
          + "li[role='option'], "
            // PrimeNG-specific (v15/16/17+)
          + ".p-dropdown-panel li, "
          + ".p-select-overlay li, "
          + ".p-dropdown-items li, "
          + ".p-select-list li")).isEmpty();
    }

    /** Click an option in the currently-open panel by its visible text. */
    private boolean clickPanelOption(String label) {
        String safe = label.replace("'", "&apos;");
        By[] optionLocators = new By[] {
            // ARIA-semantic exact match (highest priority — works across PrimeNG versions)
            By.xpath("//*[@role='option' and normalize-space(.)='" + safe + "']"),
            // PrimeNG-class exact match
            By.xpath("//li[contains(@class,'p-select-option') and normalize-space(.)='" + safe + "']"),
            By.xpath("//li[contains(@class,'p-dropdown-item') and normalize-space(.)='" + safe + "']"),
            // Any <li> inside a panel/overlay
            By.xpath("//div[contains(@class,'p-dropdown-panel') or contains(@class,'p-select-overlay') "
                   + "or @role='listbox']//li[normalize-space(.)='" + safe + "']"),
            // Case-insensitive contains() as last resort
            By.xpath("//*[@role='option' and contains(translate(normalize-space(.),"
                   + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                   + safe.toLowerCase() + "')]"),
            By.xpath("//li[contains(translate(normalize-space(.),"
                   + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                   + safe.toLowerCase() + "')]")
        };

        for (By by : optionLocators) {
            List<WebElement> hits = driver.findElements(by);
            for (WebElement opt : hits) {
                if (!opt.isDisplayed()) continue;
                try {
                    scrollIntoView(opt);
                    opt.click();
                } catch (Exception e) {
                    WaitUtils.jsClick(driver, opt);
                }
                return true;
            }
        }
        return false;
    }

    /** Did the dropdown's own label now reflect the picked value? */
    private boolean dropdownShows(WebElement host, String label) {
        try {
            List<WebElement> labels = host.findElements(By.cssSelector(
                ".p-dropdown-label, .p-select-label, .p-inputtext"));
            for (WebElement lbl : labels) {
                String t = lbl.getText();
                if (t == null) continue;
                String trimmed = t.trim();
                // Ignore the placeholder rendered inside the same span
                if (trimmed.toLowerCase().startsWith("select ")) continue;
                if (trimmed.equalsIgnoreCase(label.trim())) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void dismissOpenPanel() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {}
    }

    private void scrollIntoView(WebElement el) {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", el);
        } catch (Exception ignored) {}
    }

    // Legacy compat
    public boolean isFormValid() { return !hasValidationErrors(); }
}
