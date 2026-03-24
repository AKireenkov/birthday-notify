package com.birthday.notify.service;

import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileSyncService {

    private static final Logger log = LoggerFactory.getLogger(FileSyncService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Set<String> REQUIRED_COLUMNS = Set.of("ФИО", "Дата рождения", "Должность", "Подразделение");

    private final EmployeeRepository repository;

    @Value("${app.sync.folder:./sync}")
    private String syncFolder;

    private LocalDateTime lastSyncTime;
    private String lastSyncStatus;

    public FileSyncService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public String getLastSyncStatus() { return lastSyncStatus; }

    /**
     * Scan sync folder for CSV files and import them.
     * Returns summary message.
     */
    @Transactional
    public String syncFromFolder() {
        Path folder = Paths.get(syncFolder);

        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
                log.info("Created sync folder: {}", folder.toAbsolutePath());
            } catch (IOException e) {
                lastSyncStatus = "Ошибка: не удалось создать папку " + folder;
                lastSyncTime = LocalDateTime.now();
                return lastSyncStatus;
            }
        }

        List<Path> csvFiles;
        try (Stream<Path> stream = Files.list(folder)) {
            csvFiles = stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            lastSyncStatus = "Ошибка чтения папки: " + e.getMessage();
            lastSyncTime = LocalDateTime.now();
            return lastSyncStatus;
        }

        if (csvFiles.isEmpty()) {
            lastSyncStatus = "Нет CSV-файлов в папке " + folder.toAbsolutePath();
            lastSyncTime = LocalDateTime.now();
            return lastSyncStatus;
        }

        int totalImported = 0;
        int totalDuplicates = 0;
        int totalErrors = 0;
        int filesProcessed = 0;

        for (Path csvFile : csvFiles) {
            log.info("Syncing file: {}", csvFile.getFileName());
            try {
                int[] result = importCsvFile(csvFile);
                totalImported += result[0];
                totalDuplicates += result[1];
                totalErrors += result[2];
                filesProcessed++;

                // Move processed file to 'done' subfolder
                Path doneDir = folder.resolve("done");
                if (!Files.exists(doneDir)) Files.createDirectories(doneDir);
                Files.move(csvFile, doneDir.resolve(csvFile.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("Moved {} to done/", csvFile.getFileName());

            } catch (Exception e) {
                log.error("Error processing {}: {}", csvFile.getFileName(), e.getMessage());
                totalErrors++;
            }
        }

        lastSyncTime = LocalDateTime.now();
        lastSyncStatus = String.format("Обработано файлов: %d. Импортировано: %d, дубликатов: %d, ошибок: %d",
                filesProcessed, totalImported, totalDuplicates, totalErrors);
        log.info("Sync complete: {}", lastSyncStatus);
        return lastSyncStatus;
    }

    private int[] importCsvFile(Path csvFile) throws IOException, CsvValidationException {
        int imported = 0;
        int duplicates = 0;
        int errors = 0;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(Files.newInputStream(csvFile), StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build()) {

            String[] header = reader.readNext();
            if (header == null) throw new IllegalArgumentException("Файл пуст");

            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIndex.put(header[i].replace("\uFEFF", "").trim(), i);
            }

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
                        errors++;
                        continue;
                    }
                    String fullName = line[nameIdx].trim();
                    String dateStr = line.length > dateIdx ? line[dateIdx].trim() : "";

                    LocalDate birthDate;
                    try {
                        birthDate = LocalDate.parse(dateStr, DATE_FMT);
                    } catch (DateTimeParseException e) {
                        log.warn("File {}, row {} — invalid date: {}", csvFile.getFileName(), rowNum, dateStr);
                        errors++;
                        continue;
                    }

                    String position = line.length > posIdx ? line[posIdx].trim() : "";
                    String department = line.length > deptIdx ? line[deptIdx].trim() : "";

                    Optional<Employee> existing = repository.findByFullNameAndBirthDate(fullName, birthDate);
                    if (existing.isPresent()) {
                        // Update existing record
                        Employee emp = existing.get();
                        emp.setPosition(position);
                        emp.setDepartment(department);
                        repository.save(emp);
                        duplicates++;
                        continue;
                    }

                    repository.save(new Employee(fullName, birthDate, position, department));
                    imported++;
                } catch (Exception e) {
                    log.warn("File {}, row {} — error: {}", csvFile.getFileName(), rowNum, e.getMessage());
                    errors++;
                }
            }
        }

        return new int[]{imported, duplicates, errors};
    }
}
