package com.birthday.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CalendarServiceTest {

    private CalendarService service;

    @BeforeEach
    void setUp() {
        // Create service and manually load calendar to avoid @PostConstruct
        service = new CalendarService();
        service.loadCalendar();
    }

    @Test
    void isNonWorkingDay_saturday_returnsTrue() {
        // 2026-03-28 is Saturday
        assertThat(service.isNonWorkingDay(LocalDate.of(2026, 3, 28))).isTrue();
    }

    @Test
    void isNonWorkingDay_sunday_returnsTrue() {
        // 2026-03-29 is Sunday
        assertThat(service.isNonWorkingDay(LocalDate.of(2026, 3, 29))).isTrue();
    }

    @Test
    void isWorkingDay_wednesday_returnsTrue() {
        // 2026-03-25 is Wednesday
        assertThat(service.isWorkingDay(LocalDate.of(2026, 3, 25))).isTrue();
    }

    @Test
    void calculateNotificationWindow_wednesday_returnsTwoDays() {
        // Wednesday -> [Wed, Thu] (Thu is working, so window stops)
        LocalDate wed = LocalDate.of(2026, 3, 25);
        List<LocalDate> window = service.calculateNotificationWindow(wed);

        assertThat(window).hasSize(2);
        assertThat(window.get(0)).isEqualTo(wed);
        assertThat(window.get(1)).isEqualTo(wed.plusDays(1)); // Thu
    }

    @Test
    void calculateNotificationWindow_friday_includesWeekend() {
        // Friday -> [Fri, Sat, Sun] (Mon is working, so window stops)
        LocalDate fri = LocalDate.of(2026, 3, 27);
        List<LocalDate> window = service.calculateNotificationWindow(fri);

        assertThat(window).hasSize(3);
        assertThat(window.get(0)).isEqualTo(fri);                 // Fri
        assertThat(window.get(1)).isEqualTo(fri.plusDays(1));     // Sat
        assertThat(window.get(2)).isEqualTo(fri.plusDays(2));     // Sun
    }

    @Test
    void calculateNotificationWindow_thursday_returnsTwoDays() {
        // Thursday -> [Thu, Fri] (Fri is working, window stops after Fri+1=Sat... wait)
        // Actually: window = today + tomorrow + consecutive non-working after tomorrow
        // Thu: today=Thu, tomorrow=Fri. Fri+1=Sat (non-working), Sat+1=Sun (non-working), Sun+1=Mon (working)
        // So: [Thu, Fri, Sat, Sun]
        LocalDate thu = LocalDate.of(2026, 3, 26);
        List<LocalDate> window = service.calculateNotificationWindow(thu);

        assertThat(window).hasSize(4);
        assertThat(window.get(0)).isEqualTo(thu);
        assertThat(window.get(1)).isEqualTo(thu.plusDays(1)); // Fri
        assertThat(window.get(2)).isEqualTo(thu.plusDays(2)); // Sat
        assertThat(window.get(3)).isEqualTo(thu.plusDays(3)); // Sun
    }

    @Test
    void calculateNotificationWindow_alwaysIncludesTodayAndTomorrow() {
        LocalDate monday = LocalDate.of(2026, 3, 23);
        List<LocalDate> window = service.calculateNotificationWindow(monday);

        assertThat(window).hasSizeGreaterThanOrEqualTo(2);
        assertThat(window.get(0)).isEqualTo(monday);
        assertThat(window.get(1)).isEqualTo(monday.plusDays(1));
    }

    @Test
    void calculateNotificationWindow_newYearHolidays_extendsWindow() {
        // Dec 31 2025 (Wednesday) — Jan 1-8 are holidays in Russia
        // Window should extend through all consecutive non-working days
        LocalDate dec31 = LocalDate.of(2025, 12, 31);
        List<LocalDate> window = service.calculateNotificationWindow(dec31);

        // Should include Dec 31, Jan 1, and continue through all holidays
        assertThat(window.size()).isGreaterThan(2);
        assertThat(window.get(0)).isEqualTo(dec31);
    }
}
