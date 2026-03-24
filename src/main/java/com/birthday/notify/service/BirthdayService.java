package com.birthday.notify.service;

import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;

@Service
public class BirthdayService {

    private static final Logger log = LoggerFactory.getLogger(BirthdayService.class);

    private final EmployeeRepository repository;
    private final CalendarService calendarService;

    public BirthdayService(EmployeeRepository repository, CalendarService calendarService) {
        this.repository = repository;
        this.calendarService = calendarService;
    }

    /**
     * Find employees with birthdays on a given date.
     * Feb 29 rule: in non-leap years, Feb 29 birthdays show on Mar 1.
     */
    public List<Employee> findBirthdaysOn(LocalDate date) {
        List<Employee> result = new ArrayList<>(
                repository.findByBirthMonthAndDay(date.getMonthValue(), date.getDayOfMonth()));

        // In non-leap year, on March 1 also include Feb 29 birthdays
        boolean isLeapYear = Year.of(date.getYear()).isLeap();
        if (!isLeapYear && date.getMonthValue() == 3 && date.getDayOfMonth() == 1) {
            List<Employee> feb29 = repository.findByBirthMonthAndDay(2, 29);
            result.addAll(feb29);
        }

        return result;
    }

    /**
     * Get today's birthday count (for display on main page).
     */
    public int getTodayBirthdayCount() {
        return findBirthdaysOn(LocalDate.now()).size();
    }

    /**
     * Get today's birthdays only (single date map).
     */
    public Map<LocalDate, List<Employee>> getTodayBirthdays() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<Employee>> result = new LinkedHashMap<>();
        List<Employee> birthdays = findBirthdaysOn(today);
        result.put(today, birthdays);
        log.info("Found {} birthday(s) on {}", birthdays.size(), today);
        return result;
    }

    /**
     * Get birthdays for the full notification window (per PRD FR-3).
     *
     * Window = today + tomorrow + all consecutive non-working days after tomorrow.
     *
     * Examples:
     * - Wednesday: [Wed, Thu]
     * - Friday: [Fri, Sat, Sun]
     * - Friday before May holidays: [Fri, Sat, Sun, Mon(hol), ...]
     * - Dec 31 (Wed): [Dec 31, Jan 1..8]
     */
    public Map<LocalDate, List<Employee>> getNotificationWindowBirthdays() {
        LocalDate today = LocalDate.now();
        return getNotificationWindowBirthdays(today);
    }

    /**
     * Overload for testing with arbitrary date.
     */
    public Map<LocalDate, List<Employee>> getNotificationWindowBirthdays(LocalDate today) {
        List<LocalDate> window = calendarService.calculateNotificationWindow(today);
        Map<LocalDate, List<Employee>> result = new LinkedHashMap<>();

        int totalCount = 0;
        for (LocalDate date : window) {
            List<Employee> birthdays = findBirthdaysOn(date);
            result.put(date, birthdays);
            totalCount += birthdays.size();
        }

        log.info("Notification window {} -> {}: {} dates, {} birthday(s)",
                window.get(0), window.get(window.size() - 1), window.size(), totalCount);
        return result;
    }
}
