package codehumane.reactorplayground;

import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

public class BrokenSinkTest {

    @Test
    void run() {
        final BrokenSink brokenSink = new BrokenSink();

        LongStream
//                .range(0, 1_000_000_000)
                .range(0, 1_000)
                .map(v -> v % 10 == 0 ? -1 * v : v)
                .forEach(v -> {
                    brokenSink.run(v);
                    logWithMemory(v);
                });
    }

    private void logWithMemory(long value) {
        System.out.printf(
                "memory at %d - max: %d, total: %d, free: %d%n",
                value,
                Runtime.getRuntime().maxMemory(),
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory()
        );
    }

}
