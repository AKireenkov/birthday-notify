package com.birthday.notify.repository;

import com.birthday.notify.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByFullNameAndBirthDate(String fullName, LocalDate birthDate);

    @Query("SELECT e FROM Employee e WHERE " +
           "MONTH(e.birthDate) = :month AND DAY(e.birthDate) = :day")
    List<Employee> findByBirthMonthAndDay(@Param("month") int month, @Param("day") int day);

    List<Employee> findAllByOrderByFullNameAsc();
}
