package org.zapphyre.fun;

import java.time.Duration;

@FunctionalInterface
public interface IntervalComputer {
    BaseTimeComputer computeInterval(Duration interval);
}
