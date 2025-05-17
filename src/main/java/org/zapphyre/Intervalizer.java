package org.zapphyre;

import lombok.experimental.UtilityClass;
import org.zapphyre.config.InteriSettings;
import org.zapphyre.model.IntervalGroup;
import org.zapphyre.model.OccurringElement;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Gatherer;

@UtilityClass
public class Intervalizer {

    class Memoizer<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final Function<K, V> computer;

        Memoizer(Function<K, V> computer) {
            this.computer = computer;
        }

        V get(K key) {
            return cache.computeIfAbsent(key, computer);
        }
    }

    public <T extends OccurringElement> InteriSettings<T> interiBuilder() {
        return elements -> settings -> {
            // Sort elements and collect to list for memoization
            List<T> sortedElements = elements.stream()
                    .filter(e -> e.getOccurredOn() != null)
                    .sorted(Comparator.comparing(OccurringElement::getOccurredOn))
                    .toList();

            // Memoized function to compute group start time
            Memoizer<LocalDateTime, LocalDateTime> getGroupStart = new Memoizer<>(time ->
                    sortedElements.stream()
                            .map(OccurringElement::getOccurredOn)
                            .filter(t -> !time.isBefore(t) && time.isBefore(t.plus(settings.getInterval())))
                            .findFirst()
                            .orElse(time)
            );

            AtomicInteger groupCount = new AtomicInteger();

            // Custom Gatherer to assign elements to groups
            Gatherer<T, Map<LocalDateTime, IntervalGroup.IntervalGroupBuilder<T>>, IntervalGroup<T>> groupGatherer =
                    Gatherer.ofSequential(
                            HashMap::new, // Initializer: map of start time to Group
                            Gatherer.Integrator.ofGreedy((state, element, downstream) -> {
                                LocalDateTime time = element.getOccurredOn();
                                LocalDateTime groupStart = getGroupStart.get(time);

                                // Update or create group
                                IntervalGroup.IntervalGroupBuilder<T> updatedGroup = state.compute(groupStart, (k, g) ->
                                        (g == null ? IntervalGroup.<T>builder()
                                                .start(groupStart) : g));

                                // Add to either based on element count
                                if (groupCount.getAndIncrement() > settings.getMaxElements())
                                    updatedGroup.discardedElement(element);
                                else
                                    updatedGroup.occurringElement(element);

                                // Emit the group if it's complete (next element belongs to a new group)
                                boolean isLastInGroup = sortedElements.stream()
                                        .filter(e -> e.getOccurredOn().isAfter(time))
                                        .findFirst()
                                        .map(next -> !getGroupStart.get(next.getOccurredOn()).equals(groupStart))
                                        .orElse(true);

                                if (isLastInGroup) {
                                    downstream.push(updatedGroup
                                            .end(element.getOccurredOn())
                                            .build());

                                    groupCount.set(0);
                                }

                                return true;
                            }),
                            (state, downstream) -> {
                                // Emit any remaining groups
                                state.values().stream()
                                        .map(IntervalGroup.IntervalGroupBuilder::build)
                                        .forEach(downstream::push);
                            } // Finisher
                    );

            // Stream pipeline
            return sortedElements.stream()
                    .gather(groupGatherer)
                    .toList();
        };
    }
}
