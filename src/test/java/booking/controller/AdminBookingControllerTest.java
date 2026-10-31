package booking.controller;

import booking.dtos.CreateBookingRequest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Slot testSlot;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        slotRepository.deleteAll();

        testSlot = slotRepository.save(Slot.builder()
                .startTime(LocalDateTime.of(2026, 11, 4, 10, 0))
                .endTime(LocalDateTime.of(2026, 11, 4, 11, 0))
                .status(SlotStatus.AVAILABLE)
                .build());
    }

    @Test
    void testCancelBooking_asAdmin_shouldReturnOk() throws Exception {
        // user1 books the slot
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        String responseJson = mockMvc.perform(post("/bookings")
                        .with(httpBasic("user1", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(responseJson).get("id").asLong();

        // admin cancels the booking
        mockMvc.perform(post("/admin/bookings/" + bookingId + "/cancel")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testCancelBooking_asUser_shouldReturnForbidden() throws Exception {
        // user1 books the slot
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        String responseJson = mockMvc.perform(post("/bookings")
                        .with(httpBasic("user1", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(responseJson).get("id").asLong();

        // user1 tries to hit admin cancel endpoint
        mockMvc.perform(post("/admin/bookings/" + bookingId + "/cancel")
                        .with(httpBasic("user1", "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCancelBooking_unauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/bookings/1/cancel"))
                .andExpect(status().isUnauthorized());
    }
}
