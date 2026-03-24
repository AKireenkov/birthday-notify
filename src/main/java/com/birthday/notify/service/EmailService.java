package com.birthday.notify.service;

import com.birthday.notify.model.Employee;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String[] MONTHS_RU = {
            "", "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };
    private static final String[] WEEKDAYS_RU = {
            "", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    };

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String emailFrom;

    @Value("${app.email.recipients}")
    private String emailRecipients;

    @Value("${app.smtp.retry.delay:30000}")
    private long retryDelayMs;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send birthday notification email.
     * 1 retry on transient failure, NO retry on auth failure (PRD E-12).
     * Throws RuntimeException after 2 failed attempts so caller can detect failure.
     */
    public void sendBirthdayNotification(Map<LocalDate, List<Employee>> birthdays) {
        String subject = "\uD83C\uDF82 Дни рождения сотрудников — " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        String html = buildHtml(birthdays);
        String[] recipients = emailRecipients.split(",");

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(emailFrom);
                helper.setTo(recipients);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(msg);
                log.info("Email sent to {} recipients", recipients.length);
                return;
            } catch (MailAuthenticationException e) {
                // PRD E-12: auth failure — no retry
                log.error("SMTP authentication failed — will NOT retry: {}", e.getMessage());
                throw new RuntimeException("SMTP auth failure", e);
            } catch (Exception e) {
                // Check nested cause for AuthenticationFailedException
                if (hasAuthCause(e)) {
                    log.error("SMTP authentication failed (nested) — will NOT retry: {}", e.getMessage());
                    throw new RuntimeException("SMTP auth failure", e);
                }
                log.warn("SMTP attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < 2) {
                    try { Thread.sleep(retryDelayMs); } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        log.error("Failed to send email after 2 attempts");
        throw new RuntimeException("Failed to send email after 2 attempts");
    }

    private boolean hasAuthCause(Throwable e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof AuthenticationFailedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String buildHtml(Map<LocalDate, List<Employee>> birthdays) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head>");
        sb.append("<body style='font-family:Arial,sans-serif;max-width:700px;margin:0 auto;padding:20px;background:#f5f7fa;'>");

        // Header
        sb.append("<div style='background:white;border-radius:12px;padding:24px;margin-bottom:16px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);'>");
        sb.append("<h1 style='color:#2c3e50;margin:0 0 8px 0;'>\uD83C\uDF82 Дни рождения сотрудников</h1>");
        sb.append("<p style='color:#7f8c8d;margin:0;'>Рассылка от ")
                .append(today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .append("</p></div>");

        for (Map.Entry<LocalDate, List<Employee>> entry : birthdays.entrySet()) {
            LocalDate date = entry.getKey();
            List<Employee> emps = entry.getValue();

            // Section label: today / tomorrow / weekday
            String sectionLabel;
            String sectionColor;
            if (date.equals(today)) {
                sectionLabel = "\uD83C\uDF1F Сегодня";
                sectionColor = "#e74c3c";
            } else if (date.equals(tomorrow)) {
                sectionLabel = "\uD83D\uDD14 Завтра";
                sectionColor = "#f39c12";
            } else {
                // Weekend/holiday days in the extended window
                int dow = date.getDayOfWeek().getValue();
                sectionLabel = "\uD83D\uDCC5 " + WEEKDAYS_RU[dow];
                sectionColor = "#8e44ad";
            }

            sb.append("<div style='background:white;border-radius:12px;padding:20px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,0.06);border-left:4px solid ").append(sectionColor).append(";'>");

            // Section header
            sb.append("<h2 style='color:#2c3e50;margin:0 0 4px 0;font-size:18px;'>");
            if (!sectionLabel.isEmpty()) {
                sb.append(sectionLabel).append(" &mdash; ");
            }
            sb.append(HtmlUtils.htmlEscape(formatDateRu(date)));
            sb.append("</h2>");

            if (emps.isEmpty()) {
                sb.append("<p style='color:#95a5a6;font-style:italic;margin:12px 0 0 0;'>Нет именинников</p>");
            } else {
                sb.append("<table style='width:100%;border-collapse:collapse;margin-top:12px;'>");
                sb.append("<tr style='background:").append(sectionColor).append(";color:white;'>");
                sb.append("<th style='padding:10px 12px;text-align:left;border-radius:6px 0 0 0;'>ФИО</th>");
                sb.append("<th style='padding:10px 12px;text-align:left;'>Должность</th>");
                sb.append("<th style='padding:10px 12px;text-align:left;border-radius:0 6px 0 0;'>Подразделение</th></tr>");
                for (int i = 0; i < emps.size(); i++) {
                    Employee emp = emps.get(i);
                    String bg = i % 2 == 0 ? "#f8f9fa" : "#ffffff";
                    sb.append("<tr style='background:").append(bg).append(";'>")
                            .append("<td style='padding:10px 12px;border-bottom:1px solid #ecf0f1;'>\uD83C\uDF81 ")
                            .append(HtmlUtils.htmlEscape(emp.getFullName())).append("</td>")
                            .append("<td style='padding:10px 12px;border-bottom:1px solid #ecf0f1;'>")
                            .append(HtmlUtils.htmlEscape(emp.getPosition() != null ? emp.getPosition() : "")).append("</td>")
                            .append("<td style='padding:10px 12px;border-bottom:1px solid #ecf0f1;'>")
                            .append(HtmlUtils.htmlEscape(emp.getDepartment() != null ? emp.getDepartment() : "")).append("</td>")
                            .append("</tr>");
                }
                sb.append("</table>");
            }
            sb.append("</div>");
        }

        // Footer
        sb.append("<p style='color:#95a5a6;font-size:12px;text-align:center;margin-top:20px;'>Автоматическая рассылка Birthday Notify</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String formatDateRu(LocalDate d) {
        int dow = d.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        return d.getDayOfMonth() + " " + MONTHS_RU[d.getMonthValue()] + " " + d.getYear() + ", " + WEEKDAYS_RU[dow];
    }
}
