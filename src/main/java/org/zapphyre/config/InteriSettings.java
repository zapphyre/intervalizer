package org.zapphyre.config;

import org.zapphyre.model.OccurringElement;

import java.util.List;

@FunctionalInterface
public interface InteriSettings<T extends OccurringElement> {

    InteriBuilder<T> intervalSettings(List<T> elements);
}
