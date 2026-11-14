package com.sarbar.support.domain.model;

public enum ProblemType {
  ORDER_STATUS,
  LATE_DELIVERY,
  WRONG_ITEM,
  NOT_RECEIVED,
  COUPON_NOT_APPLIED,
  REFUND_REQUEST,
  DOUBLE_CHARGE,
  WRONG_FEE,
  DEBITED_ORDER_FAILED,
  INVOICE_COPY,
  OTHER;

  public boolean isPaymentProblem() {
    return this == DOUBLE_CHARGE
        || this == WRONG_FEE
        || this == DEBITED_ORDER_FAILED
        || this == INVOICE_COPY;
  }

  public boolean isOrderProblem() {
    return !isPaymentProblem() && this != OTHER;
  }
}
