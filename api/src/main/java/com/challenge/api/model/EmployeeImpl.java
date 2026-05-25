package com.challenge.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(of = "uuid")
public class EmployeeImpl implements Employee {

    private UUID uuid;
    private String firstName;
    private String lastName;
    private Integer salary;
    private Integer age;
    private String jobTitle;
    private String email;
    private Instant contractHireDate;
    private Instant contractTerminationDate;

    public static EmployeeImpl copyOf(Employee source) {
        Employee employee = Objects.requireNonNull(source, "source");

        return EmployeeImpl.builder()
                .uuid(employee.getUuid())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .salary(employee.getSalary())
                .age(employee.getAge())
                .jobTitle(employee.getJobTitle())
                .email(employee.getEmail())
                .contractHireDate(employee.getContractHireDate())
                .contractTerminationDate(employee.getContractTerminationDate())
                .build();
    }

    @Override
    public String getFullName() {
        return Stream.of(firstName, lastName).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    /**
     * Full name is derived from firstName and lastName, so external fullName writes are intentionally ignored.
     */
    @Override
    public void setFullName(String ignored) {}
}
