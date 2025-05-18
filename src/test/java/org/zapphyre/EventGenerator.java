package org.zapphyre;

import net.datafaker.Faker;
import org.zapphyre.model.Event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EventGenerator {

    public static List<Event> generateEvents(int count) {
        List<Event> events = new ArrayList<>();
        Faker faker = new Faker();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String title = faker.book().title();
            String description = faker.lorem().sentence();

            // Random offset between 1 minute and 5 days (in minutes)
            int minutesAgo = 1 + random.nextInt(5 * 24 * 60);
            LocalDateTime occurredOn = LocalDateTime.now().minusMinutes(minutesAgo);

            events.add(new Event(title, description, occurredOn));
        }

        return events;
    }

    public static List<Event> generateEventsWithFixedInterval(int count, LocalDateTime startTime) {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String title = "Title " + i;
            String description = "Description " + i;

            LocalDateTime occurredOn = startTime.plusMinutes(60L * i);
            events.add(new Event(title, description, occurredOn));
        }

        return events;
    }
}
