package org.zapphyre.fun;

import java.time.LocalDateTime;

@FunctionalInterface
public interface TimeComputer {
    LocalDateTime computeTime(LocalDateTime time);
}
