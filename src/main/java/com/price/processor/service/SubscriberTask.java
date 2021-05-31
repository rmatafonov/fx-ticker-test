package com.price.processor.service;

import com.price.processor.domain.CurrencyRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

public class SubscriberTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SubscriberTask.class);

    private final ExecutorService subscribersExecutor;
    private final PriorityBlockingQueue<CurrencyRate> subscriberQueue;
    private final PriceProcessor subscriber;

    public SubscriberTask(
            final ExecutorService subscribersExecutor,
            final PriorityBlockingQueue<CurrencyRate> subscriberQueue,
            final PriceProcessor subscriber
    ) {
        this.subscribersExecutor = subscribersExecutor;
        this.subscriberQueue = subscriberQueue;
        this.subscriber = subscriber;
    }

    @Override
    public void run() {
        try {
            while (!this.subscribersExecutor.isShutdown()) {
                if (!this.subscriberQueue.isEmpty()) {
                    CurrencyRate earliestRate = subscriberQueue.take();
                    log.debug("[{}] Extracted '{}'", subscriber.toString(), earliestRate);
                    subscriber.onPrice(earliestRate.getCcyPair(), earliestRate.getRate());
                }
            }
        } catch (InterruptedException e) {
            log.error("Processing has been interrupted", e);
        }
        // TODO: probably need to process the rest of queue. Review requirements
    }
}
