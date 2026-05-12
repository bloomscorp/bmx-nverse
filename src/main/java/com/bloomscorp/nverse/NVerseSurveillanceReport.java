package com.bloomscorp.nverse;

/**
 * Immutable result record returned by {@link NVerseGatekeeper#runSurveillance}.
 *
 * <p>A {@code NVerseSurveillanceReport} carries two fields:</p>
 * <ul>
 *   <li>{@code failed} — {@code true} if any check in the surveillance pipeline
 *       did not pass; {@code false} if the request is fully authorized.</li>
 *   <li>{@code errorCode} — the specific error code indicating why surveillance failed,
 *       drawn from {@link com.bloomscorp.hastar.code.ErrorCode}. When {@code failed} is
 *       {@code false} this field contains {@link com.bloomscorp.hastar.code.ActionCode#NO_ACTION}
 *       ({@code 0}), which can be safely ignored.</li>
 * </ul>
 *
 * <p>Typical usage in a protected controller or service method:</p>
 * <pre>{@code
 * NVerseSurveillanceReport report = gatekeeper.runSurveillance(currentUser, OPERATION_CODE);
 * if (report.failed())
 *     return rainTree.failureResponse(ErrorCode.decode(report.errorCode()));
 * }</pre>
 *
 * @param failed    {@code true} if the surveillance check failed.
 * @param errorCode the error code associated with the failure, or
 *                  {@link com.bloomscorp.hastar.code.ActionCode#NO_ACTION} on success.
 */
public record NVerseSurveillanceReport(boolean failed, int errorCode) {
}
