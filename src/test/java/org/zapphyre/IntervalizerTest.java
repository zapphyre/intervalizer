package org.zapphyre;

import org.junit.jupiter.api.Test;
import org.zapphyre.config.ESeriesStart;
import org.zapphyre.config.IntervaliProps;
import org.zapphyre.model.Event;
import org.zapphyre.model.IntervalGroup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zapphyre.EventGenerator.generateEventsWithFixedInterval;
import static org.zapphyre.Intervalizer.intervalize;
import static org.zapphyre.Intervalizer.nextBeginningComputer;

public class IntervalizerTest {

    @Test
    void testGrouping() {
        List<Event> events = generateEventsWithFixedInterval(222, LocalDateTime.now());

        IntervaliProps props = IntervaliProps.builder()
                .interval(Duration.ofHours(5))
                .first(ESeriesStart.LATEST)
                .maxElements(3)
                .build();

        List<IntervalGroup<Event>> described = intervalize(events)
                .described(props);

        assertEquals(45, described.size());
    }


    // === Level 1: Interval-only ===

    @Test
    void level1_closureFromInterval15() {
        // Given
        final Duration INTERVAL_15 = Duration.ofMinutes(15);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 0, 1);

        // When
        var baseTimeFn = nextBeginningComputer.computeInterval(INTERVAL_15);
        LocalDateTime result = baseTimeFn.computeBaseTime(BASE_TIME).computeTime(TIME);

        // Then
        assertEquals(BASE_TIME, result);
    }

    @Test
    void level1_closureFromInterval30() {
        // Given
        final Duration INTERVAL_30 = Duration.ofMinutes(30);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 1, 0);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 1, 25);

        // When
        var baseTimeFn = nextBeginningComputer.computeInterval(INTERVAL_30);
        LocalDateTime result = baseTimeFn.computeBaseTime(BASE_TIME).computeTime(TIME);

        // Then
        assertEquals(BASE_TIME, result);
    }

    // === Level 2: Interval + BaseTime ===

    @Test
    void level2_closureWithBaseTime() {
        // Given
        final Duration INTERVAL_20 = Duration.ofMinutes(20);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 1, 0);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 1, 5);

        // When
        var timeFn = nextBeginningComputer.computeInterval(INTERVAL_20).computeBaseTime(BASE_TIME);
        LocalDateTime result = timeFn.computeTime(TIME);

        // Then
        assertEquals(BASE_TIME, result);
    }

    @Test
    void level2_handlesNegativeOffset() {
        // Given
        final Duration INTERVAL_10 = Duration.ofMinutes(10);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 0, 30);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 0, 25);

        // When
        var timeFn = nextBeginningComputer.computeInterval(INTERVAL_10).computeBaseTime(BASE_TIME);
        LocalDateTime result = timeFn.computeTime(TIME);

        // Then
        // Expected baseTime, because offset is negative and integer division truncates to zero
        assertEquals(BASE_TIME, result);
    }

    // === Level 3: Fully Applied ===

    @Test
    void level3_exactMatchBoundary() {
        // Given
        final Duration INTERVAL_60 = Duration.ofMinutes(60);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 3, 0);

        // When
        LocalDateTime groupStart = nextBeginningComputer
                .computeInterval(INTERVAL_60)
                .computeBaseTime(BASE_TIME)
                .computeTime(TIME);

        // Then
        assertEquals(TIME, groupStart);
    }

    @Test
    void level3_roundsBackProperly() {
        // Given
        final Duration INTERVAL_60 = Duration.ofMinutes(60);
        final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);
        final LocalDateTime TIME = LocalDateTime.of(2024, 1, 1, 3, 59);

        // When
        LocalDateTime groupStart = nextBeginningComputer
                .computeInterval(INTERVAL_60)
                .computeBaseTime(BASE_TIME)
                .computeTime(TIME);

        // Then
        assertEquals(LocalDateTime.of(2024, 1, 1, 3, 0), groupStart);
    }
}
