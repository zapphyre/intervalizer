package org.zapphyre.model;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@RequiredArgsConstructor
public class Event implements OccurringElement {

    String title;
    String description;
    LocalDateTime occurredOn;

}