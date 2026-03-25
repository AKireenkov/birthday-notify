package com.birthday.notify.service;

import com.birthday.notify.dto.EmployeeDto;
import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private EmployeeDto dto;

    @BeforeEach
    void setUp() {
        dto = new EmployeeDto();
        dto.setFullName("Иванов Иван Иванович");
        dto.setBirthDate(LocalDate.of(1990, 5, 15));
        dto.setPosition("Разработчик");
        dto.setDepartment("IT");
    }

    @Test
    void findAll_returnsAllEmployees() {
        Employee emp = new Employee("Петров П.П.", LocalDate.of(1985, 3, 25), "Менеджер", "HR");
        when(repository.findAllByOrderByFullNameAsc()).thenReturn(List.of(emp));

        List<Employee> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Петров П.П.");
        verify(repository).findAllByOrderByFullNameAsc();
    }

    @Test
    void findById_existing_returnsEmployee() {
        Employee emp = new Employee("Петров П.П.", LocalDate.of(1985, 3, 25), "", "");
        emp.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(emp));

        Optional<Employee> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Петров П.П.");
    }

    @Test
    void findById_nonExisting_returnsEmpty() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<Employee> result = service.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void create_newEmployee_success() {
        when(repository.findByFullNameAndBirthDate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        Employee result = service.create(dto);

        assertThat(result.getFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.getPosition()).isEqualTo("Разработчик");
        assertThat(result.getDepartment()).isEqualTo("IT");
        verify(repository).save(any(Employee.class));
    }

    @Test
    void create_trimsWhitespace() {
        dto.setFullName("  Иванов И.И.  ");
        dto.setPosition("  Dev  ");
        dto.setDepartment("  IT  ");
        when(repository.findByFullNameAndBirthDate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.create(dto);

        assertThat(result.getFullName()).isEqualTo("Иванов И.И.");
        assertThat(result.getPosition()).isEqualTo("Dev");
        assertThat(result.getDepartment()).isEqualTo("IT");
    }

    @Test
    void create_duplicate_throwsException() {
        Employee existing = new Employee("Иванов Иван Иванович", LocalDate.of(1990, 5, 15), "", "");
        when(repository.findByFullNameAndBirthDate("Иванов Иван Иванович", LocalDate.of(1990, 5, 15)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void create_nullPosition_setsEmptyString() {
        dto.setPosition(null);
        dto.setDepartment(null);
        when(repository.findByFullNameAndBirthDate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.create(dto);

        assertThat(result.getPosition()).isEmpty();
        assertThat(result.getDepartment()).isEmpty();
    }

    @Test
    void update_existing_success() {
        Employee existing = new Employee("Старое ФИО", LocalDate.of(1990, 1, 1), "", "");
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.update(1L, dto);

        assertThat(result.getFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    void update_nonExisting_throwsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void delete_existing_success() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_nonExisting_throwsException() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");
    }
}
