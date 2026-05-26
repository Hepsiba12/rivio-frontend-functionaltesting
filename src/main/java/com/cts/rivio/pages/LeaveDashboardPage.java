package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * LeaveDashboardPage – page object for /leave (Admin role).
 *
 * Verified against live Angular DOM (leave-dashboard.component.html):
 *
 *   h1 text:           "Leave Approvals"
 *   Badge:             element containing "Action Required"
 *   Pending table:     p-table with columns: Employee | Leave Type | Dates | Days | Actions
 *   Review button:     <button>Review</button> inside each table row
 *
 * Review modal (p-dialog):
 *   header:            "Review Leave Request"
 *   Content:           read-only employee info (Angular signals, no formControlNames)
 *   Approve button:    "Approve Request"
 *   Reject button:     "Reject"
 *   No rejection-reason field — rejection is a direct action (updateStatus('REJECTED'))
 */
public class LeaveDashboardPage {

    // ── Locators ──────────────────────────────────────────────────────────────

    private static final By PAGE_HEADING = By.xpath("//h1[normalize-space()='Leave Approvals']");

    private static final By ACTION_REQUIRED_BADGE = By.xpath(
        "//*[contains(normalize-space(),'Action Required')]");

    private static final By PENDING_TABLE_ROWS = By.cssSelector("p-table tbody tr");

    private static final By REVIEW_BTN = By.xpath(
        "//p-table//button[normalize-space()='Review']");

    // Modal selectors — dialog header is "Review Leave Request"
    private static final By REVIEW_MODAL_TITLE = By.xpath(
        "//*[contains(@class,'p-dialog-title')][contains(.,'Review Leave Request')]");

    private static final By APPROVE_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[contains(normalize-space(),'Approve Request')] | " +
        "//button[contains(normalize-space(),'Approve Request')]");

    private static final By REJECT_BTN = By.xpath(
        "//div[contains(@class,'p-dialog')]//button[normalize-space()='Reject'] | " +
        "//button[normalize-space()='Reject']");

    // Close / Cancel button (X icon or explicit Cancel)
    private static final By CLOSE_MODAL_BTN = By.cssSelector(
        ".p-dialog-close-button, button.p-dialog-header-close, " +
        ".p-dialog-header button[aria-label='Close']");

    private final WebDriver driver;

    public LeaveDashboardPage(WebDriver driver) { this.driver = driver; }

    // ── Page checks ───────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Leave Approvals", 15);
    }

    public boolean isActionRequiredBadgeVisible() {
        try {
            List<WebElement> badges = driver.findElements(ACTION_REQUIRED_BADGE);
            return badges.stream().anyMatch(b -> {
                try { return b.isDisplayed(); } catch (Exception e) { return false; }
            });
        } catch (Exception e) { return false; }
    }

    /** Returns the number of rows currently visible in the pending requests table. */
    public int getLeaveRequestCount() {
        try {
            return driver.findElements(PENDING_TABLE_ROWS).size();
        } catch (Exception e) { return 0; }
    }

    public boolean hasReviewButton() {
        return !driver.findElements(REVIEW_BTN).isEmpty();
    }

    // ── Review modal lifecycle ────────────────────────────────────────────────

    /** Clicks the first "Review" button in the pending table. */
    public void clickFirstReview() {
        WebElement btn = WaitUtils.waitForClickability(driver, REVIEW_BTN);
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(400);
    }

    /**
     * Returns true if the "Review Leave Request" dialog is open.
     * Uses the p-dialog-title text — more reliable than checking for a
     * generic .p-dialog element.
     */
    public boolean isReviewModalOpen() {
        try {
            List<WebElement> titles = driver.findElements(REVIEW_MODAL_TITLE);
            return titles.stream().anyMatch(t -> {
                try { return t.isDisplayed(); } catch (Exception e) { return false; }
            });
        } catch (Exception e) { return false; }
    }

    /** Waits up to {@code seconds} for the Review modal to close. */
    public boolean waitForModalClose(int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(REVIEW_MODAL_TITLE));
            return true;
        } catch (Exception e) { return false; }
    }

    /** Closes the review modal via the X / close button. */
    public void closeReviewModal() {
        try {
            WebElement closeBtn = WaitUtils.waitForClickability(driver, CLOSE_MODAL_BTN);
            WaitUtils.safeClick(driver, closeBtn);
            WaitUtils.hardWait(300);
        } catch (Exception ignored) {}
    }

    // ── Approve / Reject actions ──────────────────────────────────────────────

    /**
     * Clicks "Approve Request" inside the open Review modal.
     * Returns true if the modal closed within {@code waitSeconds} (backend accepted).
     */
    public boolean approveRequest(int waitSeconds) {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, APPROVE_BTN);
            WaitUtils.safeClick(driver, btn);
            WaitUtils.hardWait(500);
        } catch (Exception e) {
            System.err.println("[LeaveDashboardPage] approveRequest click failed: " + e.getMessage());
            return false;
        }
        return waitForModalClose(waitSeconds);
    }

    /**
     * Clicks "Reject" inside the open Review modal.
     * No rejection-reason field exists in the current UI (direct signal call).
     * Returns true if the modal closed within {@code waitSeconds}.
     */
    public boolean rejectRequest(int waitSeconds) {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, REJECT_BTN);
            WaitUtils.safeClick(driver, btn);
            WaitUtils.hardWait(500);
        } catch (Exception e) {
            System.err.println("[LeaveDashboardPage] rejectRequest click failed: " + e.getMessage());
            return false;
        }
        return waitForModalClose(waitSeconds);
    }

    /**
     * Polls until the pending request count drops below {@code previousCount},
     * or until the timeout expires.  Use after approve/reject to confirm the
     * backend updated the record and the table re-rendered.
     *
     * @param previousCount  row count before the action
     * @param seconds        max seconds to wait
     * @return true if count decreased
     */
    public boolean waitForPendingCountToDecrease(int previousCount, int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                int current = getLeaveRequestCount();
                if (current < previousCount) return true;
            } catch (Exception ignored) {}
            WaitUtils.hardWait(500);
        }
        return false;
    }
}
