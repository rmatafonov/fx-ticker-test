package com.price.processor.service;

import com.price.processor.domain.CurrencyRate;
import com.price.processor.domain.SubscriberAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * PriceThrottler: see com.price.processor.service.PriceProcessor for the spec
 * Required DI:
 * <ul>
 *     <li>subscribersExecutor - java.util.concurrent.ExecutorService for processing subscribers</li>
 *     <li>onPriceExecutor - java.util.concurrent.ExecutorService for processing incoming rates</li>
 * </ul>
 * <p>
 * Multithreading policies are delegated to a higher level
 * ExecutorService's should be started and stopped at a higher level
 * TODO: Clarify policy of submitting rates after subscribersExecutor is shutdown
 * <p>
 * java --version
 * openjdk 11.0.11 2021-04-20
 * OpenJDK Runtime Environment AdoptOpenJDK-11.0.11+9 (build 11.0.11+9)
 * OpenJDK 64-Bit Server VM AdoptOpenJDK-11.0.11+9 (build 11.0.11+9, mixed mode)
 * <p>
 * Maven Version: 3.6.3
 */
public class PriceThrottler implements PriceProcessor {
    private final Logger log = LoggerFactory.getLogger(PriceThrottler.class);

    private final ExecutorService subscribersExecutor;
    private final ExecutorService onPriceExecutor;
    private final List<SubscriberAttributes> subscribers = new CopyOnWriteArrayList<>();

    public PriceThrottler(
            final ExecutorService subscribersExecutor,
            final ExecutorService onPriceExecutor
    ) {
        this.subscribersExecutor = subscribersExecutor;
        this.onPriceExecutor = onPriceExecutor;
    }

    @Override
    public void onPrice(final String ccyPair, final double rate) {
        if (this.onPriceExecutor.isShutdown() || this.onPriceExecutor.isTerminated()) {
            log.warn("onPrice Executor has been shutdown. Can't accept '{}' with rate '{}'", ccyPair, rate);
            return;
        }
        final Runnable onPriceTask = () -> this.subscribers.forEach(attr -> {
            CurrencyRate currencyRate = new CurrencyRate(ccyPair, rate, ZonedDateTime.now());
            addOrUpdateCurrencyRateInQueue(attr.getQueue(), currencyRate);
        });

        this.onPriceExecutor.submit(onPriceTask);
    }

    private void addOrUpdateCurrencyRateInQueue(
            PriorityBlockingQueue<CurrencyRate> queue,
            CurrencyRate currentRate
    ) {
        queue.remove(currentRate);
        if (!queue.offer(currentRate)) {
            log.error("Unable to add rate '{}'. The queue is full", currentRate);
        }
    }

    @Override
    public void subscribe(final PriceProcessor priceProcessor) {
        List<SubscriberAttributes> subscribersFound = getEqualsSubscribers(priceProcessor);
        if (subscribersFound != null && subscribersFound.size() > 0) {
            log.error("Processor '{}' has already been subscribed", priceProcessor.toString());
            return;
        }
        if (this.subscribersExecutor.isShutdown() || this.subscribersExecutor.isTerminated()) {
            log.warn("Subscribers Executor has been shutdown. Can't subscribe '{}'", priceProcessor.toString());
            return;
        }

        try {
            PriorityBlockingQueue<CurrencyRate> subscriberQueue = new PriorityBlockingQueue<>(
                    200, Comparator.comparing(CurrencyRate::getUpdated)
            );
            Future<?> subscriberTask = this.subscribersExecutor.submit(
                    new SubscriberTask(this.subscribersExecutor, subscriberQueue, priceProcessor)
            );

            log.info("The processor '{}' has been subscribed", priceProcessor.toString());
            this.subscribers.add(new SubscriberAttributes(priceProcessor, subscriberQueue, subscriberTask));
        } catch (RejectedExecutionException e) {
            log.error("The processor '{}' hasn't been subscribed: {}", priceProcessor.toString(), e);
        }
    }

    @Override
    public void unsubscribe(final PriceProcessor priceProcessor) {
        List<SubscriberAttributes> subscribersFound = getEqualsSubscribers(priceProcessor);
        if (subscribersFound == null) return;

        if (subscribersFound.size() == 0) {
            log.error("Nothing to unsubscribe");
            return;
        }
        if (subscribersFound.size() > 1) {
            log.warn("Found subscribers: '{}'. Unsubscribing all", subscribersFound);
        }

        subscribersFound.forEach(it -> it.getTask().cancel(false));

        if (!this.subscribers.removeIf(attr -> attr.getProcessor().equals(priceProcessor))) {
            log.warn("The processor '{}' hasn't been unsubscribed", priceProcessor.toString());
        } else {
            log.info("The processor '{}' has been unsubscribed", priceProcessor.toString());
        }
    }

    private List<SubscriberAttributes> getEqualsSubscribers(PriceProcessor priceProcessor) {
        if (priceProcessor == null) {
            log.error("The passed priceProcessor is NULL");
            return null;
        }

        return this.subscribers.stream()
                .filter(it -> it.getProcessor().equals(priceProcessor))
                .collect(Collectors.toList());
    }
}
