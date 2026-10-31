package booking.controller;

import booking.dtos.CreateSlotRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateSlot_asAdmin_shouldReturnCreated() throws Exception {
        CreateSlotRequest request = CreateSlotRequest.builder()
                .startTime(LocalDateTime.of(2026, 11, 2, 9, 0))
                .endTime(LocalDateTime.of(2026, 11, 2, 10, 0))
                .build();

        mockMvc.perform(post("/slots")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void testCreateSlot_asUser_shouldReturnForbidden() throws Exception {
        CreateSlotRequest request = CreateSlotRequest.builder()
                .startTime(LocalDateTime.of(2026, 11, 2, 11, 0))
                .endTime(LocalDateTime.of(2026, 11, 2, 12, 0))
                .build();

        mockMvc.perform(post("/slots")
                        .with(httpBasic("user1", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateSlot_unauthenticated_shouldReturnUnauthorized() throws Exception {
        CreateSlotRequest request = CreateSlotRequest.builder()
                .startTime(LocalDateTime.of(2026, 11, 2, 13, 0))
                .endTime(LocalDateTime.of(2026, 11, 2, 14, 0))
                .build();

        mockMvc.perform(post("/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllSlots_asUser_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/slots")
                        .with(httpBasic("user1", "user123")))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllSlots_asAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/slots")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllSlots_unauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/slots"))
                .andExpect(status().isUnauthorized());
    }
}
