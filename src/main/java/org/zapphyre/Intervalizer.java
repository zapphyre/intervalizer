package org.zapphyre;

import lombok.experimental.UtilityClass;
import org.zapphyre.config.ESeriesStart;
import org.zapphyre.config.InteriBuilder;
import org.zapphyre.fun.IntervalComputer;
import org.zapphyre.fun.TimeComputer;
import org.zapphyre.model.IntervalGroup;
import org.zapphyre.model.OccurringElement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherer;

@UtilityClass
public class Intervalizer {

    IntervalComputer nextBeginningComputer = interval -> baseTime -> time -> {
        Duration offset = Duration.between(baseTime, time);
        long steps = offset.toMinutes() / interval.toMinutes();
        return baseTime.plus(interval.multipliedBy(steps));
    };

    public static <T extends OccurringElement> InteriBuilder<T> intervalize(List<T> elements) {
        return settings -> {
            List<T> sortedElements = elements.stream()
                    .filter(e -> e.getOccurredOn() != null)
                    .sorted(settings.getFirst() == ESeriesStart.OLDEST
                            ? Comparator.comparing(OccurringElement::getOccurredOn)
                            : Comparator.comparing(OccurringElement::getOccurredOn).reversed())
                    .toList();

            if (sortedElements.isEmpty()) return List.of();

            LocalDateTime baseTime = sortedElements.getFirst().getOccurredOn();
            Duration interval = settings.getInterval();
            AtomicInteger currentIndex = new AtomicInteger();
            AtomicInteger groupCount = new AtomicInteger();

            // Function to determine groupStart based on baseTime and interval
            TimeComputer fromNowNextBeginning =
                    nextBeginningComputer.computeInterval(interval).computeBaseTime(baseTime);

            Gatherer<T, Map<LocalDateTime, IntervalGroup.IntervalGroupBuilder<T>>, IntervalGroup<T>> groupGatherer =
                    Gatherer.ofSequential(
                            HashMap::new,
                            Gatherer.Integrator.ofGreedy((state, element, downstream) -> {
                                int index = currentIndex.getAndIncrement();
                                LocalDateTime time = element.getOccurredOn();
                                LocalDateTime groupStart = fromNowNextBeginning.computeTime(time);

                                IntervalGroup.IntervalGroupBuilder<T> updatedGroup = state.compute(groupStart, (k, g) ->
                                        g == null ? IntervalGroup.<T>builder().start(groupStart) : g);

                                if (groupCount.getAndIncrement() >= settings.getMaxElements())
                                    updatedGroup.discardedElement(element);
                                else
                                    updatedGroup.occurringElement(element);

                                boolean emit = false;
                                if (index + 1 >= sortedElements.size()) {
                                    emit = true;
                                } else {
                                    T next = sortedElements.get(index + 1);
                                    LocalDateTime nextGroupStart = fromNowNextBeginning.computeTime(next.getOccurredOn());
                                    if (!nextGroupStart.equals(groupStart)) {
                                        emit = true;
                                    }
                                }

                                if (emit) {
                                    downstream.push(updatedGroup.end(element.getOccurredOn()).build());
                                    groupCount.set(0);
                                    state.remove(groupStart);
                                }

                                return true;
                            }),
                            (state, downstream) -> state.values().stream()
                                    .map(IntervalGroup.IntervalGroupBuilder::build)
                                    .forEach(downstream::push)
                    );

            return sortedElements.stream()
                    .gather(groupGatherer)
                    .toList();
        };
    }


}
