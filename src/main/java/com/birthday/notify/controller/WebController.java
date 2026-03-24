package com.birthday.notify.controller;

import com.birthday.notify.model.Employee;
import com.birthday.notify.service.BirthdayService;
import com.birthday.notify.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final EmployeeService employeeService;
    private final BirthdayService birthdayService;

    public WebController(EmployeeService employeeService, BirthdayService birthdayService) {
        this.employeeService = employeeService;
        this.birthdayService = birthdayService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Employee> employees = employeeService.findAll();
        Map<LocalDate, List<Employee>> todayBirthdays = birthdayService.getTodayBirthdays();
        int birthdayCount = todayBirthdays.values().stream().mapToInt(List::size).sum();

        model.addAttribute("employees", employees);
        model.addAttribute("birthdayCount", birthdayCount);
        model.addAttribute("today", LocalDate.now());
        return "index";
    }
}
