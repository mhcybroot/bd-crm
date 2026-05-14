package com.bdcrm.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest(properties = "bdcrm.features.organization-management-enabled=true")
@AutoConfigureMockMvc
class TemplateUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FollowupTemplateRepository templateRepository;

    @Test
    void updatingTemplateCanReplaceStepsWithoutUniqueConstraintCollision() throws Exception {
        Long templateId = templateRepository.findByNameIgnoreCase("Standard 7 Touch")
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/followup-templates/{templateId}", templateId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Standard 7 Touch",
                                  "description": "Updated template",
                                  "isDefault": true,
                                  "active": true,
                                  "steps": [
                                    { "stepNumber": 1, "dayOffset": 0, "channel": "CALL", "instructions": "Call immediately" },
                                    { "stepNumber": 2, "dayOffset": 2, "channel": "EMAIL", "instructions": "Send overview" },
                                    { "stepNumber": 3, "dayOffset": 5, "channel": "WHATSAPP", "instructions": "Follow up on WhatsApp" }
                                  ],
                                  "stages": [
                                    { "name": "Prospecting", "stageOrder": 1, "slaHours": 48, "exitAutomation": "START_FIRST_TOUCH" },
                                    { "name": "Qualified", "stageOrder": 2, "slaHours": 72, "exitAutomation": "FOLLOWUP_REMINDER" },
                                    { "name": "Proposal", "stageOrder": 3, "slaHours": 96, "exitAutomation": "PROPOSAL_TRACKING" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.steps[0].stepNumber").value(1))
                .andExpect(jsonPath("$.steps[0].instructions").value("Call immediately"));

        FollowupTemplate template = templateRepository.findById(templateId).orElseThrow();
        assertThat(template.getSteps())
                .hasSize(3)
                .extracting(FollowupTemplateStep::getStepNumber)
                .containsExactly(1, 2, 3);
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
