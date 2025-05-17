package org.zapphyre.config;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class IntervaliProps {

    ESeriesStart first;
    int maxElements;
    Duration interval;

}
