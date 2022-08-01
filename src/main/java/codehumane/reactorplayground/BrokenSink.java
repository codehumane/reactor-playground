package codehumane.reactorplayground;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

public class BrokenSink {

    final EmitterProcessor<Long> processor;
    final FluxSink<Long> sink;

    public BrokenSink() {
        processor = EmitterProcessor.create();
        sink = processor.sink(FluxSink.OverflowStrategy.ERROR);

        processor
                .publishOn(Schedulers.parallel())
                .subscribe(this::consumeItem, this::consumeError);
    }

    private void consumeItem(Long value) {
        if (value < 0) throw new BrokenSinkException(value);
        System.out.printf("## item consumed: %d%n", value);
    }

    private void consumeError(Throwable throwable) {
        System.out.println("## item consume failed" + throwable.getMessage());
    }

    public void run(Long value) {
        sink.next(value);
    }

    static class BrokenSinkException extends RuntimeException {

        public BrokenSinkException(long source) {
            super(String.format("Broken sink by %d", source));
        }

    }

}
