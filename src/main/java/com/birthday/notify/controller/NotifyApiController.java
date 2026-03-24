package com.birthday.notify.controller;

import com.birthday.notify.model.Employee;
import com.birthday.notify.service.BirthdayService;
import com.birthday.notify.service.EmailService;
import com.birthday.notify.service.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notify")
public class NotifyApiController {

    private final BirthdayService birthdayService;
    private final EmailService emailService;
    private final CalendarService calendarService;

    public NotifyApiController(BirthdayService birthdayService, EmailService emailService, CalendarService calendarService) {
        this.birthdayService = birthdayService;
        this.emailService = emailService;
        this.calendarService = calendarService;
    }

    @GetMapping("/today")
    public Map<String, List<Employee>> todayBirthdays() {
        Map<LocalDate, List<Employee>> birthdays = birthdayService.getNotificationWindowBirthdays();
        // Convert LocalDate keys to string for JSON
        Map<String, List<Employee>> result = new java.util.LinkedHashMap<>();
        birthdays.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    @PostMapping("/send-test")
    public ResponseEntity<?> sendTest() {
        Map<LocalDate, List<Employee>> birthdays = birthdayService.getNotificationWindowBirthdays();
        emailService.sendBirthdayNotification(birthdays);
        int count = birthdays.values().stream().mapToInt(List::size).sum();
        return ResponseEntity.ok(Map.of(
                "message", "Тестовое письмо отправлено",
                "birthdaysFound", count,
                "windowSize", birthdays.size()
        ));
    }

    /**
     * Preview notification window for a given date (for testing).
     * Example: GET /api/notify/window?date=2026-03-20
     */
    @GetMapping("/window")
    public ResponseEntity<?> previewWindow(@RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<LocalDate> window = calendarService.calculateNotificationWindow(targetDate);
        Map<LocalDate, List<Employee>> birthdays = birthdayService.getNotificationWindowBirthdays(targetDate);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("targetDate", targetDate.toString());
        result.put("windowDates", window.stream().map(LocalDate::toString).toList());
        result.put("windowSize", window.size());

        Map<String, Object> details = new java.util.LinkedHashMap<>();
        birthdays.forEach((d, emps) -> {
            Map<String, Object> dayInfo = new java.util.LinkedHashMap<>();
            dayInfo.put("dayOfWeek", d.getDayOfWeek().toString());
            dayInfo.put("isWeekend", d.getDayOfWeek().getValue() >= 6);
            dayInfo.put("birthdayCount", emps.size());
            dayInfo.put("employees", emps.stream().map(e -> e.getFullName()).toList());
            details.put(d.toString(), dayInfo);
        });
        result.put("details", details);

        return ResponseEntity.ok(result);
    }
}
