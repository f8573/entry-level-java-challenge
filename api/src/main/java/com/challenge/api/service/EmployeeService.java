package com.challenge.api.service;

import com.challenge.api.dto.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final Map<UUID, Employee> store = new ConcurrentHashMap<>();

    public List<Employee> getAllEmployees() {
        return store.values().stream()
                .map(EmployeeImpl::copyOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Optional<Employee> findByUuid(UUID uuid) {
        return Optional.ofNullable(store.get(uuid)).map(EmployeeImpl::copyOf);
    }

    public Employee create(CreateEmployeeRequest request) {
        EmployeeImpl employee = EmployeeImpl.builder()
                .uuid(UUID.randomUUID())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .salary(request.salary())
                .age(request.age())
                .jobTitle(request.jobTitle())
                .email(request.email())
                .contractHireDate(request.contractHireDate())
                .contractTerminationDate(request.contractTerminationDate())
                .build();

        store.put(employee.getUuid(), employee);

        return EmployeeImpl.copyOf(employee);
    }
}
