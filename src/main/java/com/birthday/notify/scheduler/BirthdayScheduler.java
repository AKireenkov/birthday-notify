package com.birthday.notify.scheduler;

import com.birthday.notify.model.Employee;
import com.birthday.notify.service.BirthdayService;
import com.birthday.notify.service.CalendarService;
import com.birthday.notify.service.EmailService;
import com.birthday.notify.service.FileSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
public class BirthdayScheduler {

    private static final Logger log = LoggerFactory.getLogger(BirthdayScheduler.class);

    private final BirthdayService birthdayService;
    private final EmailService emailService;
    private final FileSyncService fileSyncService;
    private final CalendarService calendarService;

    public BirthdayScheduler(BirthdayService birthdayService, EmailService emailService,
                             FileSyncService fileSyncService, CalendarService calendarService) {
        this.birthdayService = birthdayService;
        this.emailService = emailService;
        this.fileSyncService = fileSyncService;
        this.calendarService = calendarService;
    }

    /**
     * Run daily at 08:00 MSK (05:00 UTC).
     * Per PRD FR-1: skip non-working days (weekends + holidays).
     */
    @Scheduled(cron = "${app.scheduler.cron}")
    public void sendDailyBirthdayNotification() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Moscow"));

        if (!calendarService.isWorkingDay(today)) {
            log.info("Today {} is a non-working day. Skipping notification.", today);
            return;
        }

        log.info("=== Birthday notification job started for {} ===", today);

        Map<LocalDate, List<Employee>> birthdays = birthdayService.getNotificationWindowBirthdays();
        int count = birthdays.values().stream().mapToInt(List::size).sum();

        try {
            emailService.sendBirthdayNotification(birthdays);
            log.info("=== Birthday notification job finished: {} birthday(s) ===", count);
        } catch (RuntimeException e) {
            log.error("=== Birthday notification job FAILED: {} ===", e.getMessage());
        }
    }

    /**
     * Sync CSV files from folder weekly (Monday at 06:00 UTC = 09:00 MSK).
     */
    @Scheduled(cron = "${app.sync.cron}")
    public void scheduledFileSync() {
        log.info("=== Scheduled file sync started ===");
        String result = fileSyncService.syncFromFolder();
        log.info("=== Scheduled file sync finished: {} ===", result);
    }
}
