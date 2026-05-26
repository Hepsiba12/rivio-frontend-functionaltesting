package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LeaveDashboardPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * LeaveTest – Admin-side leave approval tests for /leave.
 *
 *   RV_LVE_001 – Leave Approvals page renders with "Action Required" badge
 *   RV_LVE_002 – Review modal opens when "Review" button is clicked
 *   RV_LVE_003 – Approve a pending leave request: modal closes + pending count decreases
 *   RV_LVE_004 – Reject a pending leave request: modal closes + pending count decreases
 *
 * RV_LVE_003 and RV_LVE_004 are skipped (not failed) when there are no
 * pending requests in the current environment so the suite stays green on
 * empty datasets.
 *
 * Verified against live Angular DOM (leave-dashboard.component.html):
 *   Approve button text: "Approve Request"
 *   Reject button text:  "Reject"
 *   Modal header:        "Review Leave Request"
 *   No rejection-reason field (direct signal call, no form control)
 */
public class LeaveTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private LeaveDashboardPage leave;

    @BeforeMethod(alwaysRun = true)
    public void openLeave() {
        driver.get(AppConstants.LEAVE_URL);
        WaitUtils.waitForAngularLoad(driver);
        leave = new LeaveDashboardPage(driver);

        // Close any stale modal left by a previous test
        if (leave.isReviewModalOpen()) {
            leave.closeReviewModal();
            WaitUtils.hardWait(300);
        }
    }

    // ── Smoke ─────────────────────────────────────────────────────────────────

    /**
     * RV_LVE_001 – Leave Approvals page renders with "Action Required" badge.
     */
    @Test(priority = 1, groups = {"smoke", "regression"},
          description = "RV_LVE_001 – Leave Approvals page renders")
    public void RV_LVE_001_leaveApprovalsPageRenders() {
        Assert.assertTrue(leave.isPageLoaded(),
                "Leave Approvals page should be loaded with h1 'Leave Approvals'");
        Assert.assertTrue(leave.isActionRequiredBadgeVisible(),
                "'Action Required' label should be visible above the pending table");
        ExtentManager.getTest().info("Pending request rows: " + leave.getLeaveRequestCount());
        ExtentManager.getTest().pass("Leave Approvals page renders correctly");
    }

    /**
     * RV_LVE_002 – Review modal opens when the "Review" button is clicked.
     */
    @Test(priority = 2, groups = {"smoke", "regression"},
          description = "RV_LVE_002 – Review Leave Request modal opens")
    public void RV_LVE_002_reviewModalOpens() {
        if (leave.getLeaveRequestCount() == 0 || !leave.hasReviewButton()) {
            ExtentManager.getTest().info(
                "No pending requests in current environment — skipping modal open check");
            return;
        }

        leave.clickFirstReview();
        Assert.assertTrue(leave.isReviewModalOpen(),
                "Clicking 'Review' should open the 'Review Leave Request' dialog");
        leave.closeReviewModal();
        WaitUtils.hardWait(400);
        ExtentManager.getTest().pass("Review Leave Request modal opens and closes correctly");
    }

    // ── Approval flow ─────────────────────────────────────────────────────────

    /**
     * RV_LVE_003 – Approve a pending leave request.
     *
     * Steps:
     *   1. Record the initial pending count.
     *   2. Click Review on the first pending request.
     *   3. Verify the Review modal opens.
     *   4. Click "Approve Request".
     *   5. Assert the modal closes (backend accepted the approval).
     *   6. Assert the pending count decreases by at least 1.
     */
    @Test(priority = 3, groups = {"regression"},
          description = "RV_LVE_003 – Approve pending leave request")
    public void RV_LVE_003_approveLeaveRequest() {
        int pendingBefore = leave.getLeaveRequestCount();
        if (pendingBefore == 0 || !leave.hasReviewButton()) {
            ExtentManager.getTest().warning(
                "No pending leave requests found — skipping RV_LVE_003");
            throw new org.testng.SkipException(
                "RV_LVE_003: No pending requests available to approve");
        }

        ExtentManager.getTest().info("Pending requests before approval: " + pendingBefore);

        // Open the review modal
        leave.clickFirstReview();
        Assert.assertTrue(leave.isReviewModalOpen(),
                "RV_LVE_003: Review modal must open before approving");

        // Click Approve Request
        boolean modalClosed = leave.approveRequest(15);
        Assert.assertTrue(modalClosed,
                "RV_LVE_003: Modal should close after clicking 'Approve Request' "
              + "(backend must have processed the approval)");
        ExtentManager.getTest().info("Review modal closed after approve");

        // Verify the pending table updates
        boolean countDecreased = leave.waitForPendingCountToDecrease(pendingBefore, 10);
        Assert.assertTrue(countDecreased,
                "RV_LVE_003: Pending request count should decrease after approval "
              + "(was " + pendingBefore + ")");

        ExtentManager.getTest().pass(
            "RV_LVE_003 — Leave request approved; pending count dropped from "
          + pendingBefore + " to " + leave.getLeaveRequestCount());
    }

    // ── Rejection flow ────────────────────────────────────────────────────────

    /**
     * RV_LVE_004 – Reject a pending leave request.
     *
     * Steps:
     *   1. Record the initial pending count.
     *   2. Click Review on the first pending request.
     *   3. Verify the Review modal opens.
     *   4. Click "Reject".
     *   5. Assert the modal closes (backend accepted the rejection).
     *   6. Assert the pending count decreases by at least 1.
     *
     * Note: The Angular component calls updateStatus('REJECTED') directly —
     * there is no rejection-reason form field in the current UI.
     */
    @Test(priority = 4, groups = {"regression"},
          description = "RV_LVE_004 – Reject pending leave request")
    public void RV_LVE_004_rejectLeaveRequest() {
        int pendingBefore = leave.getLeaveRequestCount();
        if (pendingBefore == 0 || !leave.hasReviewButton()) {
            ExtentManager.getTest().warning(
                "No pending leave requests found — skipping RV_LVE_004");
            throw new org.testng.SkipException(
                "RV_LVE_004: No pending requests available to reject");
        }

        ExtentManager.getTest().info("Pending requests before rejection: " + pendingBefore);

        // Open the review modal
        leave.clickFirstReview();
        Assert.assertTrue(leave.isReviewModalOpen(),
                "RV_LVE_004: Review modal must open before rejecting");

        // Click Reject
        boolean modalClosed = leave.rejectRequest(15);
        Assert.assertTrue(modalClosed,
                "RV_LVE_004: Modal should close after clicking 'Reject' "
              + "(backend must have processed the rejection)");
        ExtentManager.getTest().info("Review modal closed after reject");

        // Verify the pending table updates
        boolean countDecreased = leave.waitForPendingCountToDecrease(pendingBefore, 10);
        Assert.assertTrue(countDecreased,
                "RV_LVE_004: Pending request count should decrease after rejection "
              + "(was " + pendingBefore + ")");

        ExtentManager.getTest().pass(
            "RV_LVE_004 — Leave request rejected; pending count dropped from "
          + pendingBefore + " to " + leave.getLeaveRequestCount());
    }
}
