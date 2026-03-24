package com.birthday.notify.service;

import com.birthday.notify.dto.EmployeeDto;
import com.birthday.notify.model.Employee;
import com.birthday.notify.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public List<Employee> findAll() {
        return repository.findAllByOrderByFullNameAsc();
    }

    public Optional<Employee> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Employee create(EmployeeDto dto) {
        // Check for duplicate
        Optional<Employee> existing = repository.findByFullNameAndBirthDate(
                dto.getFullName().trim(), dto.getBirthDate());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Сотрудник с таким ФИО и датой рождения уже существует");
        }

        Employee employee = new Employee(
                dto.getFullName().trim(),
                dto.getBirthDate(),
                dto.getPosition() != null ? dto.getPosition().trim() : "",
                dto.getDepartment() != null ? dto.getDepartment().trim() : ""
        );
        return repository.save(employee);
    }

    @Transactional
    public Employee update(Long id, EmployeeDto dto) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден: " + id));

        employee.setFullName(dto.getFullName().trim());
        employee.setBirthDate(dto.getBirthDate());
        employee.setPosition(dto.getPosition() != null ? dto.getPosition().trim() : "");
        employee.setDepartment(dto.getDepartment() != null ? dto.getDepartment().trim() : "");
        return repository.save(employee);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Сотрудник не найден: " + id);
        }
        repository.deleteById(id);
    }
}
