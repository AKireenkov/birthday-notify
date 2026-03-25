package com.birthday.notify.controller;

import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EmployeeRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void listEmployees_empty_returnsEmptyArray() throws Exception {
        mvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listEmployees_withData_returnsAll() throws Exception {
        repository.save(new Employee("Иванов И.И.", LocalDate.of(1990, 1, 1), "Dev", "IT"));
        repository.save(new Employee("Петров П.П.", LocalDate.of(1985, 5, 15), "QA", "QA"));

        mvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getEmployee_existing_returnsOk() throws Exception {
        Employee emp = repository.save(
                new Employee("Иванов И.И.", LocalDate.of(1990, 1, 1), "Dev", "IT"));

        mvc.perform(get("/api/employees/{id}", emp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Иванов И.И."))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"));
    }

    @Test
    void getEmployee_nonExisting_returns404() throws Exception {
        mvc.perform(get("/api/employees/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEmployee_validData_returnsOk() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Сидоров А.А.",
                "birthDate", "1992-07-20",
                "position", "Аналитик",
                "department", "Аналитика"
        );

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Сидоров А.А."))
                .andExpect(jsonPath("$.birthDate").value("1992-07-20"))
                .andExpect(jsonPath("$.id").isNumber());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void createEmployee_missingName_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "birthDate", "1992-07-20"
        );

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployee_duplicate_returns400() throws Exception {
        repository.save(new Employee("Иванов И.И.", LocalDate.of(1990, 1, 1), "", ""));

        Map<String, Object> body = Map.of(
                "fullName", "Иванов И.И.",
                "birthDate", "1990-01-01"
        );

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("уже существует")));
    }

    @Test
    void updateEmployee_validData_returnsUpdated() throws Exception {
        Employee emp = repository.save(
                new Employee("Старое ФИО", LocalDate.of(1990, 1, 1), "Dev", "IT"));

        Map<String, Object> body = Map.of(
                "fullName", "Новое ФИО",
                "birthDate", "1990-01-01",
                "position", "Senior Dev",
                "department", "IT"
        );

        mvc.perform(put("/api/employees/{id}", emp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Новое ФИО"))
                .andExpect(jsonPath("$.position").value("Senior Dev"));
    }

    @Test
    void updateEmployee_nonExisting_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "ФИО",
                "birthDate", "1990-01-01"
        );

        mvc.perform(put("/api/employees/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("не найден")));
    }

    @Test
    void deleteEmployee_existing_returnsOk() throws Exception {
        Employee emp = repository.save(
                new Employee("Удалить М.М.", LocalDate.of(1990, 1, 1), "", ""));

        mvc.perform(delete("/api/employees/{id}", emp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Удалено"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void deleteEmployee_nonExisting_returns400() throws Exception {
        mvc.perform(delete("/api/employees/{id}", 999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("не найден")));
    }

    @Test
    void uploadCsv_validFile_importsEmployees() throws Exception {
        String csvContent = "ФИО;Дата рождения;Должность;Подразделение\n"
                + "Козлова М.Д.;28.03.1988;Тестировщик;QA\n"
                + "Морозов А.В.;10.01.1991;PM;PMO\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "employees.csv", "text/csv", csvContent.getBytes());

        mvc.perform(multipart("/api/employees/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.duplicatesSkipped").value(0))
                .andExpect(jsonPath("$.errorsSkipped").value(0));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void uploadCsv_withDuplicates_skipsDuplicates() throws Exception {
        repository.save(new Employee("Козлова М.Д.", LocalDate.of(1988, 3, 28), "", ""));

        String csvContent = "ФИО;Дата рождения;Должность;Подразделение\n"
                + "Козлова М.Д.;28.03.1988;Тестировщик;QA\n"
                + "Новый Сотрудник;10.01.1991;PM;PMO\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "employees.csv", "text/csv", csvContent.getBytes());

        mvc.perform(multipart("/api/employees/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.duplicatesSkipped").value(1));
    }

    @Test
    void uploadCsv_invalidFormat_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.txt", "text/plain", "hello".getBytes());

        mvc.perform(multipart("/api/employees/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}
