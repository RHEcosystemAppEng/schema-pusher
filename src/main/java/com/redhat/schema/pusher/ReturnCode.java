package com.redhat.schema.pusher;

/** Enum for relaying push operation status codes. */
public enum ReturnCode {
  /** Operation completed successfully. */
  SUCCESS(0),
  /** Operation failed due to an error related to the directory containing the schema files. */
  DIRECTORY_ERROR(999),
  /** Operation failed due to an error related to the Kafka producer. */
  PRODUCER_ERROR(998);

  private final int privCode;

  ReturnCode(final int setCode) {
    this.privCode = setCode;
  }

  /**
   * Get the relayed operation status code.
   *
   * @return the relayed operation status code.
   */
  public int code() {
    return privCode;
  }
}
