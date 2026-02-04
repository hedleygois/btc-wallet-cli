package com.btcwallet.transaction;

public record FeeEstimateResponse() {

  public FeeEstimateResponse(long low, double satoshisToBTC, long medium, double satoshisToBTC2, long high,
      double satoshisToBTC3, String name, String networkStatus) {
    this();
  }

}
