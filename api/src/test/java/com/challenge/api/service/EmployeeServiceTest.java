package com.challenge.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.challenge.api.dto.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private static final Instant HIRE_DATE = Instant.parse("2020-01-15T09:00:00Z");
    private static final Instant TERMINATION_DATE = Instant.parse("2024-06-30T17:00:00Z");

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService();
    }

    @Test
    void createAssignsUuidAndMapsFields() {
        CreateEmployeeRequest request = adaLovelaceRequest();

        Employee created = service.create(request);

        assertThat(created.getUuid()).isNotNull();
        assertThat(created.getFirstName()).isEqualTo("Ada");
        assertThat(created.getLastName()).isEqualTo("Lovelace");
        assertThat(created.getSalary()).isEqualTo(125_000);
        assertThat(created.getAge()).isEqualTo(36);
        assertThat(created.getJobTitle()).isEqualTo("Principal Engineer");
        assertThat(created.getEmail()).isEqualTo("ada.lovelace@example.com");
        assertThat(created.getContractHireDate()).isEqualTo(HIRE_DATE);
        assertThat(created.getContractTerminationDate()).isEqualTo(TERMINATION_DATE);
        assertThat(created.getFullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void fullNameIsDerivedAndSetFullNameIsNoOp() {
        Employee created = service.create(adaLovelaceRequest());

        String originalFullName = created.getFullName();
        String originalFirstName = created.getFirstName();
        String originalLastName = created.getLastName();

        created.setFullName("Ignored Name");

        assertThat(created.getFullName()).isEqualTo(originalFullName);
        assertThat(created.getFirstName()).isEqualTo(originalFirstName);
        assertThat(created.getLastName()).isEqualTo(originalLastName);
    }

    @Test
    void findByUuidReturnsEmptyForUnknownUuid() {
        Optional<Employee> result = service.findByUuid(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByUuidReturnsDetachedCopy() {
        Employee created = service.create(adaLovelaceRequest());
        UUID uuid = created.getUuid();

        Optional<Employee> firstLookup = service.findByUuid(uuid);

        assertThat(firstLookup).isPresent();
        Employee found = firstLookup.get();
        assertThat(found).isNotSameAs(created);

        found.setFirstName("Mutated");

        Optional<Employee> secondLookup = service.findByUuid(uuid);
        assertThat(secondLookup).isPresent();
        assertThat(secondLookup.get().getFirstName()).isEqualTo("Ada");
    }

    @Test
    void getAllEmployeesReturnsDetachedCopies() {
        Employee created = service.create(adaLovelaceRequest());
        UUID uuid = created.getUuid();

        List<Employee> firstList = service.getAllEmployees();
        assertThat(firstList).hasSize(1);

        Employee fromList = firstList.get(0);
        fromList.setFirstName("Mutated");
        fromList.setLastName("Surname");

        Employee stored = service.findByUuid(uuid).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Ada");
        assertThat(stored.getLastName()).isEqualTo("Lovelace");

        List<Employee> secondList = service.getAllEmployees();
        assertThat(secondList).hasSize(1);
        assertThat(secondList.get(0).getFirstName()).isEqualTo("Ada");
        assertThat(secondList.get(0).getLastName()).isEqualTo("Lovelace");
    }

    @Test
    void concurrentCreateProducesUniqueUuidsAndCorrectCount() throws InterruptedException {
        int initialSize = service.getAllEmployees().size();
        int workers = 50;

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(workers);
            Set<UUID> uuids = ConcurrentHashMap.newKeySet();

            for (int i = 0; i < workers; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        start.await();
                        Employee created = service.create(workerRequest(idx));
                        uuids.add(created.getUuid());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(uuids).hasSize(workers);
            assertThat(service.getAllEmployees()).hasSize(initialSize + workers);
        } finally {
            executor.shutdownNow();
        }
    }

    private static CreateEmployeeRequest adaLovelaceRequest() {
        return new CreateEmployeeRequest(
                "Ada",
                "Lovelace",
                125_000,
                36,
                "Principal Engineer",
                "ada.lovelace@example.com",
                HIRE_DATE,
                TERMINATION_DATE);
    }

    private static CreateEmployeeRequest workerRequest(int index) {
        return new CreateEmployeeRequest(
                "Worker" + index,
                "Test",
                50_000 + index,
                25,
                "Engineer",
                "worker" + index + "@example.com",
                HIRE_DATE,
                null);
    }
}
