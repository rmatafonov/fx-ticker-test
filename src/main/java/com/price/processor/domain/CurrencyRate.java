package com.price.processor.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

public class CurrencyRate {
    private final String ccyPair;
    private final double rate;
    private final ZonedDateTime updated;

    public CurrencyRate(final String ccyPair, final double rate, ZonedDateTime updated) {
        this.ccyPair = ccyPair;
        this.rate = rate;
        this.updated = updated;
    }

    public String getCcyPair() {
        return ccyPair;
    }

    public double getRate() {
        return rate;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyRate that = (CurrencyRate) o;
        return Objects.equals(ccyPair, that.ccyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ccyPair);
    }

    @Override
    public String toString() {
        return "CurrencyRate{" +
                "ccyPair='" + ccyPair + '\'' +
                ", rate=" + rate +
                ", updated=" + updated +
                '}';
    }
}
