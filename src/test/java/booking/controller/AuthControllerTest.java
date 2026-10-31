package booking.controller;

import booking.dtos.CreateBookingRequest;
import booking.dtos.CreateSlotRequest;
import booking.dtos.LoginRequest;
import booking.entity.Slot;
import booking.enums.SlotStatus;
import booking.repo.BookingRepository;
import booking.repo.SlotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        slotRepository.deleteAll();
    }

    @Test
    void testLogin_user_success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("user1")
                .password("user123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void testLogin_admin_success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void testLogin_badCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("user1")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testJwtAuthorization_adminCanCreateSlot_userCannot() throws Exception {
        // 1. Login as user1 -> get token
        LoginRequest userLogin = LoginRequest.builder().username("user1").password("user123").build();
        String userTokenJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String userToken = objectMapper.readTree(userTokenJson).get("token").asText();

        // 2. Login as admin -> get token
        LoginRequest adminLogin = LoginRequest.builder().username("admin").password("admin123").build();
        String adminTokenJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(adminTokenJson).get("token").asText();

        CreateSlotRequest slotRequest = CreateSlotRequest.builder()
                .startTime(LocalDateTime.of(2026, 11, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 11, 10, 11, 0))
                .build();

        // 3. User tries to create slot with JWT -> 403 Forbidden
        mockMvc.perform(post("/slots")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isForbidden());

        // 4. Admin creates slot with JWT -> 201 Created
        mockMvc.perform(post("/slots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testJwtAuthorization_userCanBookSlot_adminCannot() throws Exception {
        // 1. Create slot
        Slot slot = slotRepository.save(Slot.builder()
                .startTime(LocalDateTime.of(2026, 11, 11, 14, 0))
                .endTime(LocalDateTime.of(2026, 11, 11, 15, 0))
                .status(SlotStatus.AVAILABLE)
                .build());

        // 2. Login as user1 and admin
        String userTokenJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user1", "user123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String userToken = objectMapper.readTree(userTokenJson).get("token").asText();

        String adminTokenJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(adminTokenJson).get("token").asText();

        CreateBookingRequest bookingRequest = CreateBookingRequest.builder().slotId(slot.getId()).build();

        // 3. Admin tries to book slot with JWT -> 403 Forbidden
        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isForbidden());

        // 4. User books slot with JWT -> 201 Created
        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testLogout_invalidatesToken() throws Exception {
        // 1. Login as user1
        String userTokenJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user1", "user123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(userTokenJson).get("token").asText();

        // 2. Verify token works on protected endpoint
        mockMvc.perform(get("/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Logout with token
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Logged out successfully")));

        // 4. Subsequent access with logged out token must be rejected with 401 Unauthorized
        mockMvc.perform(get("/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
