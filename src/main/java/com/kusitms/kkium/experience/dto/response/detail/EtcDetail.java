package com.kusitms.kkium.experience.dto.response.detail;

import java.time.LocalDate;

import com.kusitms.kkium.experience.domain.Etc;

public record EtcDetail(LocalDate startDate, LocalDate endDate) {

  public static EtcDetail from(Etc etc) {
    return new EtcDetail(etc.getStartDate(), etc.getEndDate());
  }
}
