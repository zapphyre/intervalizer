package org.zapphyre;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zapphyre.config.ESeriesStart;
import org.zapphyre.config.InteriBuilder;
import org.zapphyre.fun.IntervalComputer;
import org.zapphyre.fun.TimeComputer;
import org.zapphyre.model.IntervalGroup;
import org.zapphyre.model.OccurringElement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class Intervalizer {

    IntervalComputer nextBeginningComputer = interval -> baseTime -> time -> {
        Duration offset = Duration.between(baseTime, time);
        long steps = offset.toMinutes() / interval.toMinutes();
        return baseTime.plus(interval.multipliedBy(steps));
    };

    public <T extends OccurringElement> InteriBuilder<T> intervalize(List<T> elements) {
        return settings -> {
            log.debug("intervalizing #{} elements by interval {}; #{} per group",
                    elements.size(), settings.getInterval(), settings.getMaxElements());

            List<T> sortedElements = elements.stream()
                    .filter(e -> e.getOccurredOn() != null)
                    .sorted(settings.getFirst() == ESeriesStart.OLDEST
                            ? Comparator.comparing(OccurringElement::getOccurredOn)
                            : Comparator.comparing(OccurringElement::getOccurredOn).reversed())
                    .collect(Collectors.toList());

            if (sortedElements.isEmpty()) return Collections.emptyList();

            LocalDateTime baseTime = sortedElements.get(0).getOccurredOn();
            Duration interval = settings.getInterval();
            TimeComputer timeComputer = nextBeginningComputer.computeInterval(interval).computeBaseTime(baseTime);

            Map<LocalDateTime, IntervalGroup.IntervalGroupBuilder<T>> groupMap = new LinkedHashMap<>();
            List<IntervalGroup<T>> finalizedGroups = new ArrayList<>();
            AtomicInteger groupCount = new AtomicInteger(0);

            for (int i = 0; i < sortedElements.size(); i++) {
                T element = sortedElements.get(i);
                LocalDateTime time = element.getOccurredOn();
                LocalDateTime groupStart = timeComputer.computeTime(time);

                IntervalGroup.IntervalGroupBuilder<T> group = groupMap.computeIfAbsent(groupStart,
                        k -> IntervalGroup.<T>builder().start(groupStart));

                if (groupCount.incrementAndGet() > settings.getMaxElements()) {
                    group.discardedElement(element);
                } else {
                    group.occurringElement(element);
                }

                boolean isLast = i == sortedElements.size() - 1;
                boolean nextGroupDiffers = !isLast && !timeComputer.computeTime(sortedElements.get(i + 1).getOccurredOn()).equals(groupStart);

                if (isLast || nextGroupDiffers) {
                    finalizedGroups.add(group.end(time).build());
                    groupMap.remove(groupStart);
                    groupCount.set(0);
                }
            }

            return finalizedGroups;
        };
    }
}
