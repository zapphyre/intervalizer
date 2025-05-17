package org.zapphyre.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Value
@Builder
public class IntervalGroup<T extends OccurringElement> {

    LocalDateTime start;
    LocalDateTime end;

    @Singular
    List<T> occurringElements;
    List<T> discardedElements;
}