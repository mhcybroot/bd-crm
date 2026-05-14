package com.bdcrm.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdcrm.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationOnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void platformAdminCanBootstrapOrganizationAndFirstAdmin() throws Exception {
        mockMvc.perform(post("/api/organizations/bootstrap")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organization": {
                                    "slug": "acme-bd",
                                    "name": "Acme BD",
                                    "status": "ACTIVE",
                                    "timezone": "Asia/Dhaka",
                                    "locale": "en-BD",
                                    "contactEmail": "ops@acme.test",
                                    "planCode": "standard",
                                    "dataRetentionDays": 365
                                  },
                                  "adminUser": {
                                    "username": "acme-admin",
                                    "password": "secret123",
                                    "fullName": "Acme Admin",
                                    "email": "admin@acme.test"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.slug").value("acme-bd"))
                .andExpect(jsonPath("$.firstAdminUser.username").value("acme-admin"));

        Organization organization = organizationRepository.findBySlugIgnoreCase("acme-bd").orElseThrow();
        var user = userRepository.findByUsernameIgnoreCaseAndOrganizationId("acme-admin", organization.getId()).orElseThrow();
        assertThat(user.getRoles()).anyMatch(role -> role.getName().name().equals("ORG_ADMIN"));
    }

    @Test
    void duplicateOrganizationSlugDoesNotCreateUserOrOrganization() throws Exception {
        long organizationCount = organizationRepository.count();
        long userCount = userRepository.count();

        mockMvc.perform(post("/api/organizations/bootstrap")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organization": {
                                    "slug": "default",
                                    "name": "Duplicate Default",
                                    "status": "ACTIVE",
                                    "timezone": "UTC",
                                    "locale": "en-US",
                                    "contactEmail": "ops@duplicate.test",
                                    "planCode": "standard",
                                    "dataRetentionDays": 365
                                  },
                                  "adminUser": {
                                    "username": "duplicate-admin",
                                    "password": "secret123",
                                    "fullName": "Duplicate Admin",
                                    "email": "duplicate@acme.test"
                                  }
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(organizationRepository.count()).isEqualTo(organizationCount);
        assertThat(userRepository.count()).isEqualTo(userCount);
    }

    @Test
    void nonPlatformUserCannotAccessOrganizationEndpoints() throws Exception {
        mockMvc.perform(get("/api/organizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer("manager", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void orgAdminCannotCreateUsersInAnotherOrganization() throws Exception {
        mockMvc.perform(post("/api/organizations/bootstrap")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organization": {
                                    "slug": "orbit",
                                    "name": "Orbit Labs",
                                    "status": "ACTIVE",
                                    "timezone": "UTC",
                                    "locale": "en-US",
                                    "contactEmail": "ops@orbit.test",
                                    "planCode": "standard",
                                    "dataRetentionDays": 365
                                  },
                                  "adminUser": {
                                    "username": "orbit-admin",
                                    "password": "secret123",
                                    "fullName": "Orbit Admin",
                                    "email": "admin@orbit.test"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        Long defaultOrganizationId = organizationRepository.findBySlugIgnoreCase("default").orElseThrow().getId();

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer("orbit-admin", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "cross-tenant-rep",
                                  "password": "secret123",
                                  "fullName": "Cross Tenant Rep",
                                  "email": "rep@default.test",
                                  "managerId": null,
                                  "organizationId": %s,
                                  "roles": ["ORG_REP"]
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedOrganizationBootstrapIsRejected() throws Exception {
        mockMvc.perform(post("/api/organizations/bootstrap")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organization": {
                                    "slug": "paused-org",
                                    "name": "Paused Org",
                                    "status": "SUSPENDED",
                                    "timezone": "UTC",
                                    "locale": "en-US",
                                    "contactEmail": "ops@paused.test",
                                    "planCode": "standard",
                                    "dataRetentionDays": 365
                                  },
                                  "adminUser": {
                                    "username": "paused-admin",
                                    "password": "secret123",
                                    "fullName": "Paused Admin",
                                    "email": "admin@paused.test"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(organizationRepository.findBySlugIgnoreCase("paused-org")).isEmpty();
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + loginAndExtractToken(username, password);
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.get("token").asText();
    }
}
