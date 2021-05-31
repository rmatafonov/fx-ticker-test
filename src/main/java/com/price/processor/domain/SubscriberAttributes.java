package com.price.processor.domain;

import com.price.processor.service.PriceProcessor;

import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;

public class SubscriberAttributes {
    private final PriceProcessor processor;
    private final PriorityBlockingQueue<CurrencyRate> queue;
    private final Future<?> task;

    public SubscriberAttributes(PriceProcessor processor, PriorityBlockingQueue<CurrencyRate> queue, Future<?> task) {
        this.processor = processor;
        this.queue = queue;
        this.task = task;
    }

    public PriceProcessor getProcessor() {
        return processor;
    }

    public PriorityBlockingQueue<CurrencyRate> getQueue() {
        return queue;
    }

    public Future<?> getTask() {
        return task;
    }
}
