package com.kusitms.kkium.user.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record TermsAgreementRequest(@AssertTrue(message = "약관에 동의해야 합니다.") boolean termsAgreed) {}
