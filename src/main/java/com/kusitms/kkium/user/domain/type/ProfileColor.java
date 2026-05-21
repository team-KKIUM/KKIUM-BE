package com.kusitms.kkium.user.domain.type;

import java.util.Random;

public enum ProfileColor {
  RED,
  MINT,
  BLUE,
  YELLOW;

  private static final ProfileColor[] VALUES = values();
  private static final Random RANDOM = new Random();

  public static ProfileColor random() {
    return VALUES[RANDOM.nextInt(VALUES.length)];
  }
}
