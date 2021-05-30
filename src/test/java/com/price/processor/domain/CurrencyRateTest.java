package com.price.processor.domain;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyRateTest {
    @Test
    public void priorityQueueShouldRemoveCurrencyRateIndependentlyFromRateAndUpdated() {
        CurrencyRate rate = new CurrencyRate("USDEUR", 1.12, ZonedDateTime.now().minusMinutes(2L));
        CurrencyRate rate1 = new CurrencyRate("USDEUR", 1.22, ZonedDateTime.now());
        PriorityQueue<CurrencyRate> currencyRates = new PriorityQueue<>(Comparator.comparing(CurrencyRate::getUpdated));

        currencyRates.add(rate);
        currencyRates.remove(rate1);

        assertTrue(currencyRates.isEmpty());
    }

    @Test
    public void priorityQueuePollShouldReturnEarliestCurrencyRate() {
        CurrencyRate rate = new CurrencyRate("USDEUR", 1.12, ZonedDateTime.now().minusMinutes(2L));
        CurrencyRate rate1 = new CurrencyRate("USDEUR", 1.22, ZonedDateTime.now());
        PriorityQueue<CurrencyRate> currencyRates = new PriorityQueue<>(Comparator.comparing(CurrencyRate::getUpdated));

        currencyRates.add(rate1);
        Assumptions.assumeTrue(!currencyRates.isEmpty());
        currencyRates.add(rate);

        assertSame(rate, currencyRates.poll());
    }
}