package com.birthday.notify.service;

import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayServiceTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private BirthdayService service;

    private Employee petrov;
    private Employee ivanov;

    @BeforeEach
    void setUp() {
        petrov = new Employee("Петров П.П.", LocalDate.of(1990, 3, 25), "Dev", "IT");
        ivanov = new Employee("Иванов И.И.", LocalDate.of(1985, 3, 26), "QA", "IT");
    }

    @Test
    void findBirthdaysOn_matchingDate_returnsEmployees() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        when(repository.findByBirthMonthAndDay(3, 25)).thenReturn(List.of(petrov));

        List<Employee> result = service.findBirthdaysOn(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Петров П.П.");
    }

    @Test
    void findBirthdaysOn_noMatch_returnsEmpty() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(repository.findByBirthMonthAndDay(7, 1)).thenReturn(Collections.emptyList());

        List<Employee> result = service.findBirthdaysOn(date);

        assertThat(result).isEmpty();
    }

    @Test
    void findBirthdaysOn_feb29InNonLeapYear_showsOnMar1() {
        Employee leapBirthday = new Employee("Прыгунов Ф.Ф.", LocalDate.of(1988, 2, 29), "", "");
        // 2027 is not a leap year
        LocalDate mar1 = LocalDate.of(2027, 3, 1);
        when(repository.findByBirthMonthAndDay(3, 1)).thenReturn(Collections.emptyList());
        when(repository.findByBirthMonthAndDay(2, 29)).thenReturn(List.of(leapBirthday));

        List<Employee> result = service.findBirthdaysOn(mar1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Прыгунов Ф.Ф.");
    }

    @Test
    void findBirthdaysOn_feb29InLeapYear_showsOnFeb29() {
        Employee leapBirthday = new Employee("Прыгунов Ф.Ф.", LocalDate.of(1988, 2, 29), "", "");
        // 2028 is a leap year
        LocalDate feb29 = LocalDate.of(2028, 2, 29);
        when(repository.findByBirthMonthAndDay(2, 29)).thenReturn(List.of(leapBirthday));

        List<Employee> result = service.findBirthdaysOn(feb29);

        assertThat(result).hasSize(1);
    }

    @Test
    void findBirthdaysOn_mar1InLeapYear_doesNotIncludeFeb29() {
        // In a leap year, Mar 1 should NOT include Feb 29 birthdays
        LocalDate mar1 = LocalDate.of(2028, 3, 1);
        when(repository.findByBirthMonthAndDay(3, 1)).thenReturn(Collections.emptyList());
        // Should NOT query Feb 29 because it's a leap year

        List<Employee> result = service.findBirthdaysOn(mar1);

        assertThat(result).isEmpty();
        verify(repository, never()).findByBirthMonthAndDay(2, 29);
    }

    @Test
    void getNotificationWindowBirthdays_usesCalendarWindow() {
        LocalDate today = LocalDate.of(2026, 3, 25);
        LocalDate tomorrow = LocalDate.of(2026, 3, 26);
        when(calendarService.calculateNotificationWindow(today)).thenReturn(List.of(today, tomorrow));
        when(repository.findByBirthMonthAndDay(3, 25)).thenReturn(List.of(petrov));
        when(repository.findByBirthMonthAndDay(3, 26)).thenReturn(List.of(ivanov));

        Map<LocalDate, List<Employee>> result = service.getNotificationWindowBirthdays(today);

        assertThat(result).hasSize(2);
        assertThat(result.get(today)).containsExactly(petrov);
        assertThat(result.get(tomorrow)).containsExactly(ivanov);
    }

    @Test
    void getNotificationWindowBirthdays_emptyBirthdays_returnsEmptyLists() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        LocalDate tomorrow = LocalDate.of(2026, 7, 2);
        when(calendarService.calculateNotificationWindow(today)).thenReturn(List.of(today, tomorrow));
        when(repository.findByBirthMonthAndDay(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        Map<LocalDate, List<Employee>> result = service.getNotificationWindowBirthdays(today);

        assertThat(result).hasSize(2);
        assertThat(result.get(today)).isEmpty();
        assertThat(result.get(tomorrow)).isEmpty();
    }
}
