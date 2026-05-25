package com.challenge.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.api.config.SecurityConfig;
import com.challenge.api.dto.CreateEmployeeRequest;
import com.challenge.api.exception.GlobalExceptionHandler;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import com.challenge.api.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"app.security.username=erus", "app.security.password=changeme"})
class EmployeeControllerTest {

    private static final Instant HIRE_DATE = Instant.parse("2020-01-15T09:00:00Z");
    private static final Instant TERMINATION_DATE = Instant.parse("2024-06-30T17:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    @WithMockUser(roles = "USER")
    void getAllEmployeesReturns200List() throws Exception {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(employeeService.getAllEmployees()).thenReturn(List.of(adaEmployee(uuid)));

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].uuid").value(uuid.toString()))
                .andExpect(jsonPath("$[0].firstName").value("Ada"))
                .andExpect(jsonPath("$[0].lastName").value("Lovelace"))
                .andExpect(jsonPath("$[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$[0].salary").value(125_000))
                .andExpect(jsonPath("$[0].age").value(36))
                .andExpect(jsonPath("$[0].jobTitle").value("Principal Engineer"))
                .andExpect(jsonPath("$[0].email").value("ada.lovelace@example.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getEmployeeByUuidReturns200Employee() throws Exception {
        UUID uuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(employeeService.findByUuid(uuid)).thenReturn(Optional.of(adaEmployee(uuid)));

        mockMvc.perform(get("/api/v1/employee/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.salary").value(125_000))
                .andExpect(jsonPath("$.age").value(36))
                .andExpect(jsonPath("$.jobTitle").value("Principal Engineer"))
                .andExpect(jsonPath("$.email").value("ada.lovelace@example.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createEmployeeReturns201WithLocationHeader() throws Exception {
        UUID uuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(employeeService.create(any(CreateEmployeeRequest.class))).thenReturn(adaEmployee(uuid));

        String requestBody = objectMapper.writeValueAsString(adaRequest());

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/employee/" + uuid)))
                .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.salary").value(125_000))
                .andExpect(jsonPath("$.age").value(36))
                .andExpect(jsonPath("$.jobTitle").value("Principal Engineer"))
                .andExpect(jsonPath("$.email").value("ada.lovelace@example.com"));

        verify(employeeService).create(any(CreateEmployeeRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void postValidationFailureReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        verify(employeeService, never()).create(any(CreateEmployeeRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMalformedUuidReturns400ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/employee/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void postMalformedInstantReturns400ErrorResponse() throws Exception {
        ObjectNode body = objectMapper.valueToTree(adaRequest());
        body.put("contractHireDate", "not-a-date");

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(employeeService, never()).create(any(CreateEmployeeRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUnknownUuidReturns404ErrorResponse() throws Exception {
        UUID uuid = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(employeeService.findByUuid(uuid)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/employee/{uuid}", uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Employee not found:")))
                .andExpect(jsonPath("$.message", not(containsString("null"))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void postEmptyBodyReturns400ErrorResponse() throws Exception {
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(employeeService, never()).create(any(CreateEmployeeRequest.class));
    }

    private static Employee adaEmployee(UUID uuid) {
        return EmployeeImpl.builder()
                .uuid(uuid)
                .firstName("Ada")
                .lastName("Lovelace")
                .salary(125_000)
                .age(36)
                .jobTitle("Principal Engineer")
                .email("ada.lovelace@example.com")
                .contractHireDate(HIRE_DATE)
                .contractTerminationDate(TERMINATION_DATE)
                .build();
    }

    private static CreateEmployeeRequest adaRequest() {
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
}
