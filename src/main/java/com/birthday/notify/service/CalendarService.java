package com.birthday.notify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Production calendar service.
 * Determines working/non-working days using weekday rules + holiday JSON.
 * Calculates notification windows per PRD FR-3.
 */
@Service
public class CalendarService {

    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

    private final Set<LocalDate> holidays = new HashSet<>();

    @PostConstruct
    public void loadCalendar() {
        try {
            InputStream is = new ClassPathResource("calendar.json").getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode dates = root.get("non_working_dates");
            if (dates == null || !dates.isArray()) {
                throw new IllegalStateException("calendar.json missing 'non_working_dates' array");
            }
            for (JsonNode node : dates) {
                holidays.add(LocalDate.parse(node.asText(), DateTimeFormatter.ISO_LOCAL_DATE));
            }
            log.info("Production calendar loaded: {} holiday dates", holidays.size());
        } catch (IOException e) {
            throw new IllegalStateException("calendar.json not found — cannot start without production calendar", e);
        }
    }

    /**
     * Check if a date is a non-working day (weekend or holiday).
     */
    public boolean isNonWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return true;
        }
        return holidays.contains(date);
    }

    public boolean isWorkingDay(LocalDate date) {
        return !isNonWorkingDay(date);
    }

    /**
     * Calculate the notification window per PRD FR-3.
     *
     * Window = {T} ∪ {T+1} ∪ {all consecutive non-working days after T+1
     *           until first working day (not including that working day)}
     *
     * Examples:
     * - Wednesday (working):  Wed, Thu
     * - Friday (working):     Fri, Sat, Sun
     * - Friday before May holidays: Fri, Sat, Sun, Mon(holiday), Tue(holiday)...
     * - Wed Dec 31 (holiday): Dec 31, Jan 1, Jan 2, ... Jan 8
     */
    public List<LocalDate> calculateNotificationWindow(LocalDate today) {
        List<LocalDate> window = new ArrayList<>();

        // Always include today
        window.add(today);

        // Always include tomorrow
        LocalDate tomorrow = today.plusDays(1);
        window.add(tomorrow);

        // Extend: keep adding consecutive non-working days after tomorrow
        LocalDate next = tomorrow.plusDays(1);
        while (isNonWorkingDay(next)) {
            window.add(next);
            next = next.plusDays(1);
        }

        return window;
    }
}
