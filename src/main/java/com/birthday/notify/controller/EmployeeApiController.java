package com.birthday.notify.controller;

import com.birthday.notify.dto.CsvImportResult;
import com.birthday.notify.dto.EmployeeDto;
import com.birthday.notify.model.Employee;
import com.birthday.notify.service.CsvImportService;
import com.birthday.notify.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;
    private final CsvImportService csvImportService;

    public EmployeeApiController(EmployeeService employeeService, CsvImportService csvImportService) {
        this.employeeService = employeeService;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public List<Employee> list() {
        return employeeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> get(@PathVariable Long id) {
        return employeeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EmployeeDto dto) {
        try {
            Employee created = employeeService.create(dto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody EmployeeDto dto) {
        try {
            Employee updated = employeeService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            employeeService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Удалено"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            CsvImportResult result = csvImportService.importCsv(file);
            return ResponseEntity.ok(Map.of(
                    "message", result.toMessage(),
                    "imported", result.getImported(),
                    "duplicatesSkipped", result.getDuplicatesSkipped(),
                    "errorsSkipped", result.getErrorsSkipped()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
