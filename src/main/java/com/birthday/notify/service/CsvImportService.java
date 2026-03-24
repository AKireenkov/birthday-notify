package com.birthday.notify.service;

import com.birthday.notify.dto.CsvImportResult;
import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Set<String> REQUIRED_COLUMNS = Set.of("ФИО", "Дата рождения", "Должность", "Подразделение");

    private final EmployeeRepository repository;

    public CsvImportService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CsvImportResult importCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".CSV"))) {
            throw new IllegalArgumentException("Формат не подходит. Загрузите CSV-файл");
        }

        int imported = 0;
        int duplicatesSkipped = 0;
        int errorsSkipped = 0;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {

            // Read header
            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("Файл пуст — нет заголовков");
            }

            // Strip BOM and trim
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                String col = header[i].replace("\uFEFF", "").trim();
                colIndex.put(col, i);
            }

            // Validate required columns
            Set<String> missing = new HashSet<>(REQUIRED_COLUMNS);
            missing.removeAll(colIndex.keySet());
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Отсутствуют колонки: " + String.join(", ", missing));
            }

            int nameIdx = colIndex.get("ФИО");
            int dateIdx = colIndex.get("Дата рождения");
            int posIdx = colIndex.get("Должность");
            int deptIdx = colIndex.get("Подразделение");

            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    if (line.length <= nameIdx || line[nameIdx].isBlank()) {
                        log.warn("Skipped row {} — empty name", rowNum);
                        errorsSkipped++;
                        continue;
                    }

                    String fullName = line[nameIdx].trim();
                    String dateStr = line.length > dateIdx ? line[dateIdx].trim() : "";

                    LocalDate birthDate;
                    try {
                        birthDate = LocalDate.parse(dateStr, DATE_FMT);
                    } catch (DateTimeParseException e) {
                        log.warn("Skipped row {} — invalid date: {}", rowNum, dateStr);
                        errorsSkipped++;
                        continue;
                    }

                    String position = line.length > posIdx ? line[posIdx].trim() : "";
                    String department = line.length > deptIdx ? line[deptIdx].trim() : "";

                    // Dedup check in DB
                    Optional<Employee> existing = repository.findByFullNameAndBirthDate(fullName, birthDate);
                    if (existing.isPresent()) {
                        duplicatesSkipped++;
                        continue;
                    }

                    Employee emp = new Employee(fullName, birthDate, position, department);
                    repository.save(emp);
                    imported++;

                } catch (Exception e) {
                    log.warn("Skipped row {} — error: {}", rowNum, e.getMessage());
                    errorsSkipped++;
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка чтения CSV: " + e.getMessage());
        }

        log.info("CSV import: {} imported, {} duplicates, {} errors", imported, duplicatesSkipped, errorsSkipped);
        return new CsvImportResult(imported, duplicatesSkipped, errorsSkipped);
    }
}
