package com.bank;

import com.bank.domain.Role;
import com.bank.domain.TransferStatus;
import com.bank.domain.User;
import com.bank.repository.IncidentRepository;
import com.bank.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AtoDetectionE2ETest {

    private static final String HOME_IP = "192.168.1.10";
    private static final String ATTACKER_IP = "203.0.113.50";
    private static final String PASSWORD = "password1";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bank")
            .withUsername("bank")
            .withPassword("bank");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.demo.seed", () -> "false");
        registry.add("app.jwt.secret", () -> "test-secret-key-at-least-thirty-two-chars!!");
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    IncidentRepository incidentRepository;

    private String customerEmail;
    private String merchantEmail;

    @BeforeEach
    void setUpUsers() {
        customerEmail = "victim-" + UUID.randomUUID() + "@test.bank";
        merchantEmail = "merchant-" + UUID.randomUUID() + "@test.bank";
    }

    @Test
    void bruteForceThenNewIpLargeTransferIsHeldAndApprovable() throws Exception {
        register(customerEmail, "Victim");
        register(merchantEmail, "Merchant");

        // Establish a known home IP before the attack window.
        String homeToken = login(customerEmail, HOME_IP);
        JsonNode customerAccounts = openAccount(homeToken, "CHECKING", "10000.00");
        long fromId = customerAccounts.get("id").asLong();

        String merchantToken = login(merchantEmail, HOME_IP);
        JsonNode merchantAccounts = openAccount(merchantToken, "CHECKING", "0");
        long toId = merchantAccounts.get("id").asLong();

        // Brute force from attacker IP (not enough to lock — max failed is 5, we need 3).
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", ATTACKER_IP)
                            .content("""
                                    {"email":"%s","password":"wrong-password"}
                                    """.formatted(customerEmail)))
                    .andExpect(status().isUnauthorized());
        }

        String attackerToken = login(customerEmail, ATTACKER_IP);

        MvcResult transferResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + attackerToken)
                        .header("Idempotency-Key", "ato-" + UUID.randomUUID())
                        .header("X-Forwarded-For", ATTACKER_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId":%d,"toAccountId":%d,"amount":5000.00}
                                """.formatted(fromId, toId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn();

        long transferId = objectMapper.readTree(transferResult.getResponse().getContentAsString())
                .get("id").asLong();
        assertTrue(incidentRepository.findByTransferId(transferId).isPresent());

        // Balances unchanged while held.
        mockMvc.perform(get("/api/v1/accounts/" + fromId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.0));

        String analystToken = createAnalystAndLogin();
        long incidentId = incidentRepository.findByTransferId(transferId).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/approve")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransferStatus.COMPLETED.name()));

        mockMvc.perform(get("/api/v1/accounts/" + fromId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000.0));
    }

    private void register(String email, String name) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"%s","role":"CUSTOMER"}
                                """.formatted(email, PASSWORD, name)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private JsonNode openAccount(String token, String type, String deposit) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountType":"%s","initialDeposit":%s}
                                """.formatted(type, deposit)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createAnalystAndLogin() throws Exception {
        String email = "analyst-" + UUID.randomUUID() + "@test.bank";
        userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "Analyst", Role.ANALYST));
        String token = login(email, "10.0.0.2");
        assertEquals("ANALYST", userRepository.findByEmailIgnoreCase(email).orElseThrow().getRole().name());
        return token;
    }
}
