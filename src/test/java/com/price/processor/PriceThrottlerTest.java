package com.price.processor;

import com.price.processor.service.PriceProcessor;
import com.price.processor.service.PriceThrottler;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class PriceThrottlerTest {
    private final Logger log = LoggerFactory.getLogger(PriceThrottlerTest.class);
    private final List<String> currencyPairs = Arrays.asList(
            "AUDCAD", "AUDCHF", "AUDCZK", "AUDDKK", "AUDHKD", "AUDHUF", "AUDJPY", "AUDMXN", "AUDNOK", "AUDNZD", "AUDPLN",
            "AUDSEK", "AUDSGD", "AUDUSD", "AUDZAR", "CADCHF", "CADCZK", "CADDKK", "CADHKD", "CADHUF", "CADJPY", "CADMXN",
            "CADNOK", "CADPLN", "CADSEK", "CADSGD", "CADZAR", "CHFCZK", "CHFDKK", "CHFHKD", "CHFHUF", "CHFJPY", "CHFMXN",
            "CHFNOK", "CHFPLN", "CHFSEK", "CHFSGD", "CHFTRY", "CHFZAR", "DKKCZK", "DKKHKD", "DKKHUF", "DKKMXN", "DKKNOK",
            "DKKPLN", "DKKSEK", "DKKSGD", "DKKZAR", "EURAUD", "EURCAD", "EURCHF", "EURCZK", "EURDKK", "EURGBP", "EURHKD",
            "EURHUF", "EURJPY", "EURMXN", "EURNOK", "EURNZD"
    );

    @Test
    public void testToSeeStatistics() throws InterruptedException {
        final Random rnd = new Random();
        final ExecutorService subscribersExecutor = Executors.newCachedThreadPool();
        final ExecutorService currenciesExecutor = Executors.newSingleThreadExecutor();
        final PriceProcessor priceThrottler = new PriceThrottler(subscribersExecutor, currenciesExecutor);

        PriceProcessor subscriberStub1 = createSubscriberStub(100);
        priceThrottler.subscribe(subscriberStub1);
        PriceProcessor subscriberStub2 = createSubscriberStub(1000);
        priceThrottler.subscribe(subscriberStub2);
        PriceProcessor subscriberStub3 = createSubscriberStub(4000);
        priceThrottler.subscribe(subscriberStub3);


        int timeout = 10000;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            String currencyPair = randomCurrencyPair();
            double rate = rnd.nextDouble() * 10;
            priceThrottler.onPrice(currencyPair, rate);
            Thread.sleep(1000);
        }

        priceThrottler.unsubscribe(subscriberStub2);

        start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            String currencyPair = randomCurrencyPair();
            double rate = rnd.nextDouble() * 10;
            priceThrottler.onPrice(currencyPair, rate);
            Thread.sleep(1000);
        }

        subscribersExecutor.shutdown();
        currenciesExecutor.shutdown();

        priceThrottler.onPrice(randomCurrencyPair(), rnd.nextDouble() * 10);

        boolean b = subscribersExecutor.awaitTermination(30, TimeUnit.SECONDS);
        log.debug("Subscribers Executor {}Terminated", b ? "" : "Not ");
        boolean b1 = currenciesExecutor.awaitTermination(10, TimeUnit.SECONDS);
        log.debug("Currencies Executor {}Terminated", b1 ? "" : "Not ");
    }

    private PriceProcessor createSubscriberStub(long delay) {
        PriceProcessor subscriberStub = mock(PriceProcessor.class);
        doAnswer((Answer<String>) invocationOnMock -> {
            log.info(
                    "[{}] Called onPrice with parameters '{}'",
                    subscriberStub.toString(),
                    Arrays.toString(invocationOnMock.getArguments())
            );
            Thread.sleep(delay);
            return "nothing";
        }).when(subscriberStub).onPrice(anyString(), anyDouble());
        return subscriberStub;
    }

    private String randomCurrencyPair() {
        final Random rnd = new Random();
        return currencyPairs.get(rnd.nextInt(currencyPairs.size()));
    }
}