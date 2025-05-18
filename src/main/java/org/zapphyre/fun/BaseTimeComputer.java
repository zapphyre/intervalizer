package org.zapphyre.fun;

import java.time.LocalDateTime;

@FunctionalInterface
public interface BaseTimeComputer {
    TimeComputer computeBaseTime(LocalDateTime baseTime);
}