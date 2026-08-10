package org.application.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DateTimeConverter {

    private DateTimeConverter() {
    }

    public static OffsetDateTime toApplicationTime(OffsetDateTime dateTime, ZoneId zoneId) {
        return dateTime == null ? null : dateTime.toInstant().atZone(zoneId).toOffsetDateTime();
    }

    public static Instant toInstant(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }
}
