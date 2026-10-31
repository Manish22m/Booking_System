package booking.controller;

import booking.dtos.CreateBookingRequest;
import booking.entity.Booking;
import booking.entity.Slot;
import booking.enums.BookingStatus;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

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
                .startTime(LocalDateTime.of(2026, 11, 3, 10, 0))
                .endTime(LocalDateTime.of(2026, 11, 3, 11, 0))
                .status(SlotStatus.AVAILABLE)
                .build());
    }

    @Test
    void testBookSlot_asUser_shouldReturnCreated() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        mockMvc.perform(post("/bookings")
                        .with(httpBasic("user1", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.slotId").value(testSlot.getId()));
    }

    @Test
    void testBookSlot_alreadyBooked_shouldReturnConflict() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        // First booking succeeds
        mockMvc.perform(post("/bookings")
                        .with(httpBasic("user1", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second booking on the same slot must fail with 409 Conflict
        mockMvc.perform(post("/bookings")
                        .with(httpBasic("user2", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Slot is already booked")));
    }

    @Test
    void testBookSlot_asAdmin_shouldReturnForbidden() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        mockMvc.perform(post("/bookings")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBookSlot_unauthenticated_shouldReturnUnauthorized() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .slotId(testSlot.getId())
                .build();

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCancelBooking_asOwner_shouldReturnOk() throws Exception {
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

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel")
                        .with(httpBasic("user1", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testCancelBooking_byOtherUser_shouldReturnForbidden() throws Exception {
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

        // User2 tries to cancel User1's booking
        mockMvc.perform(post("/bookings/" + bookingId + "/cancel")
                        .with(httpBasic("user2", "user123")))
                .andExpect(status().isForbidden());
    }
}
