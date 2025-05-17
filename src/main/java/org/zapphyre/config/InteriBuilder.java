package org.zapphyre.config;

import org.zapphyre.model.IntervalGroup;
import org.zapphyre.model.OccurringElement;

import java.util.List;

@FunctionalInterface
public interface InteriBuilder<T extends OccurringElement> {

    List<IntervalGroup<T>> described(IntervaliProps props);
}
