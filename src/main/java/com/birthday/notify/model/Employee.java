package com.birthday.notify.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "employees", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"full_name", "birth_date"})
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ФИО обязательно")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotNull(message = "Дата рождения обязательна")
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "position")
    private String position;

    @Column(name = "department")
    private String department;

    public Employee() {}

    public Employee(String fullName, LocalDate birthDate, String position, String department) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.position = position;
        this.department = department;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
