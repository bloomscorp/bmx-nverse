package com.bloomscorp.nverse;

import lombok.Getter;

@Getter
public record NVerseSurveillanceReport(boolean failed, int errorCode) {
}
