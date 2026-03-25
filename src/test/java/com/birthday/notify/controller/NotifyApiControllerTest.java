package com.birthday.notify.controller;

import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotifyApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void todayBirthdays_returnsMap() throws Exception {
        mvc.perform(get("/api/notify/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void todayBirthdays_withBirthdayToday_containsEmployee() throws Exception {
        LocalDate today = LocalDate.now();
        // Create employee with birthday today
        Employee emp = new Employee("Тест Именинник", today, "Dev", "IT");
        repository.save(emp);

        mvc.perform(get("/api/notify/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + today.toString(), hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void previewWindow_noDate_usesToday() throws Exception {
        mvc.perform(get("/api/notify/window"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.windowDates").isArray())
                .andExpect(jsonPath("$.windowSize").isNumber());
    }

    @Test
    void previewWindow_withDate_usesSpecifiedDate() throws Exception {
        mvc.perform(get("/api/notify/window").param("date", "2026-03-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2026-03-27"))
                .andExpect(jsonPath("$.windowDates").isArray())
                // Friday March 27 -> window should include Sat+Sun
                .andExpect(jsonPath("$.windowSize").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void previewWindow_fridayBeforeWeekend_extendsWindow() throws Exception {
        // 2026-03-27 is Friday
        mvc.perform(get("/api/notify/window").param("date", "2026-03-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowSize").value(3))
                .andExpect(jsonPath("$.windowDates[0]").value("2026-03-27"))
                .andExpect(jsonPath("$.windowDates[1]").value("2026-03-28"))
                .andExpect(jsonPath("$.windowDates[2]").value("2026-03-29"));
    }
}
