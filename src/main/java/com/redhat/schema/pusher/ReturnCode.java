package com.redhat.schema.pusher;

public enum ReturnCode {
  SUCCESS(0),
  DIRECTORY_ERROR(999),
  PRODUCER_ERROR(998);

  private final int privCode;

  ReturnCode(final int setCode) {
    this.privCode = setCode;
  }

  public int code() {
    return privCode;
  }
}
