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
        sb.append("<body style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;max-width:700px;margin:0 auto;padding:0;background:#f5f5f5;'>");

        // Header — blue gradient matching Ant Design web app
        sb.append("<div style='background:linear-gradient(135deg,#1677ff 0%,#4096ff 100%);padding:28px 24px;text-align:center;border-radius:0 0 12px 12px;'>");
        sb.append("<h1 style='color:#ffffff;margin:0 0 6px 0;font-size:22px;font-weight:600;'>\uD83C\uDF82 Alfa Birthday</h1>");
        sb.append("<p style='color:rgba(255,255,255,0.85);margin:0;font-size:14px;'>Рассылка от ")
                .append(today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .append("</p></div>");

        sb.append("<div style='padding:20px;'>");

        for (Map.Entry<LocalDate, List<Employee>> entry : birthdays.entrySet()) {
            LocalDate date = entry.getKey();
            List<Employee> emps = entry.getValue();

            // Section colors matching web app palette
            String sectionLabel;
            String borderColor;
            String headerBg;
            String headerColor;
            String badgeBg;
            if (date.equals(today)) {
                sectionLabel = "\uD83C\uDF89 Сегодня";
                borderColor = "#fa541c";
                headerBg = "#fa541c";
                headerColor = "#ffffff";
                badgeBg = "#fff1b8";
            } else if (date.equals(tomorrow)) {
                sectionLabel = "\uD83D\uDD14 Завтра";
                borderColor = "#1677ff";
                headerBg = "#1677ff";
                headerColor = "#ffffff";
                badgeBg = "#e6f4ff";
            } else {
                int dow = date.getDayOfWeek().getValue();
                sectionLabel = "\uD83D\uDCC5 " + WEEKDAYS_RU[dow];
                borderColor = "#722ed1";
                headerBg = "#722ed1";
                headerColor = "#ffffff";
                badgeBg = "#f9f0ff";
            }

            sb.append("<div style='background:#ffffff;border-radius:8px;margin-bottom:16px;box-shadow:0 1px 4px rgba(0,0,0,0.08);border-left:3px solid ").append(borderColor).append(";overflow:hidden;'>");

            // Section header
            sb.append("<div style='padding:14px 20px;background:").append(badgeBg).append(";border-bottom:1px solid #f0f0f0;'>");
            sb.append("<span style='font-size:16px;font-weight:600;color:rgba(0,0,0,0.88);'>");
            sb.append(sectionLabel).append(" &mdash; ");
            sb.append(HtmlUtils.htmlEscape(formatDateRu(date)));
            sb.append("</span></div>");

            if (emps.isEmpty()) {
                sb.append("<div style='padding:16px 20px;'>");
                sb.append("<p style='color:rgba(0,0,0,0.45);font-style:italic;margin:0;'>Нет именинников</p>");
                sb.append("</div>");
            } else {
                sb.append("<table style='width:100%;border-collapse:collapse;'>");
                sb.append("<tr style='background:").append(headerBg).append(";'>");
                sb.append("<th style='padding:10px 16px;text-align:left;color:").append(headerColor).append(";font-size:13px;font-weight:500;'>ФИО</th>");
                sb.append("<th style='padding:10px 16px;text-align:left;color:").append(headerColor).append(";font-size:13px;font-weight:500;'>Должность</th>");
                sb.append("<th style='padding:10px 16px;text-align:left;color:").append(headerColor).append(";font-size:13px;font-weight:500;'>Подразделение</th></tr>");
                for (int i = 0; i < emps.size(); i++) {
                    Employee emp = emps.get(i);
                    String bg = i % 2 == 0 ? "#fafafa" : "#ffffff";
                    sb.append("<tr style='background:").append(bg).append(";'>")
                            .append("<td style='padding:10px 16px;border-bottom:1px solid #f0f0f0;color:rgba(0,0,0,0.88);'>\uD83C\uDF81 ")
                            .append(HtmlUtils.htmlEscape(emp.getFullName())).append("</td>")
                            .append("<td style='padding:10px 16px;border-bottom:1px solid #f0f0f0;color:rgba(0,0,0,0.65);'>")
                            .append(HtmlUtils.htmlEscape(emp.getPosition() != null ? emp.getPosition() : "—")).append("</td>")
                            .append("<td style='padding:10px 16px;border-bottom:1px solid #f0f0f0;color:rgba(0,0,0,0.65);'>")
                            .append(HtmlUtils.htmlEscape(emp.getDepartment() != null ? emp.getDepartment() : "—")).append("</td>")
                            .append("</tr>");
                }
                sb.append("</table>");
            }
            sb.append("</div>");
        }

        sb.append("</div>");

        // Footer
        sb.append("<div style='text-align:center;padding:16px 20px 24px;'>");
        sb.append("<p style='color:rgba(0,0,0,0.35);font-size:12px;margin:0;'>Автоматическая рассылка Alfa Birthday</p>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String formatDateRu(LocalDate d) {
        int dow = d.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        return d.getDayOfMonth() + " " + MONTHS_RU[d.getMonthValue()] + " " + d.getYear() + ", " + WEEKDAYS_RU[dow];
    }
}
