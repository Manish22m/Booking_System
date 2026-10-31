package booking.services;

import booking.dtos.BookingResponseDto;
import booking.dtos.CreateBookingRequest;
import booking.entity.Booking;
import booking.entity.Slot;
import booking.entity.User;
import booking.enums.BookingStatus;
import booking.enums.Role;
import booking.enums.SlotStatus;
import booking.exception.ResourceNotFoundException;
import booking.exception.SlotAlreadyBookedException;
import booking.exception.UnauthorizedActionException;
import booking.repo.BookingRepository;
import booking.repo.SlotRepository;
import booking.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private User otherUser;
    private Slot availableSlot;
    private Slot bookedSlot;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@techpulse.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .status(SlotStatus.AVAILABLE)
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@techpulse.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .status(SlotStatus.AVAILABLE)
                .build();

        availableSlot = Slot.builder()
                .id(10L)
                .startTime(LocalDateTime.of(2026, 11, 1, 10, 0))
                .endTime(LocalDateTime.of(2026, 11, 1, 11, 0))
                .status(SlotStatus.AVAILABLE)
                .build();

        bookedSlot = Slot.builder()
                .id(20L)
                .startTime(LocalDateTime.of(2026, 11, 1, 12, 0))
                .endTime(LocalDateTime.of(2026, 11, 1, 13, 0))
                .status(SlotStatus.BOOKED)
                .build();
    }

    @Test
    void testBookSlot_success() {
        CreateBookingRequest request = CreateBookingRequest.builder().slotId(10L).build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(10L)).thenReturn(Optional.of(availableSlot));

        Booking savedBooking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponseDto response = bookingService.bookSlot(request, "user1");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getSlotId());
        assertEquals(1L, response.getUserId());
        assertEquals(BookingStatus.ACTIVE, response.getStatus());
        assertEquals(SlotStatus.BOOKED, availableSlot.getStatus());

        verify(slotRepository, times(1)).save(availableSlot);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testBookSlot_slotAlreadyBooked_throwsConflict() {
        CreateBookingRequest request = CreateBookingRequest.builder().slotId(20L).build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(20L)).thenReturn(Optional.of(bookedSlot));

        assertThrows(SlotAlreadyBookedException.class, () -> bookingService.bookSlot(request, "user1"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookSlot_slotNotFound() {
        CreateBookingRequest request = CreateBookingRequest.builder().slotId(99L).build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.bookSlot(request, "user1"));
    }

    @Test
    void testBookSlot_userNotFound() {
        CreateBookingRequest request = CreateBookingRequest.builder().slotId(10L).build();

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.bookSlot(request, "unknown"));
    }

    @Test
    void testCancelUserBooking_success() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(bookedSlot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(slotRepository.findByIdWithLock(bookedSlot.getId())).thenReturn(Optional.of(bookedSlot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponseDto response = bookingService.cancelUserBooking(100L, "user1");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals(SlotStatus.AVAILABLE, bookedSlot.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(slotRepository, times(1)).save(bookedSlot);
    }

    @Test
    void testCancelUserBooking_unauthorized_throwsException() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(bookedSlot)
                .user(otherUser)
                .status(BookingStatus.ACTIVE)
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedActionException.class, () -> bookingService.cancelUserBooking(100L, "user1"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testCancelUserBooking_alreadyCancelled_throwsException() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .status(BookingStatus.CANCELLED)
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelUserBooking(100L, "user1"));
    }

    @Test
    void testCancelAdminBooking_success() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(bookedSlot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(slotRepository.findByIdWithLock(bookedSlot.getId())).thenReturn(Optional.of(bookedSlot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponseDto response = bookingService.cancelAdminBooking(100L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals(SlotStatus.AVAILABLE, bookedSlot.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(slotRepository, times(1)).save(bookedSlot);
    }

    @Test
    void testCancelAdminBooking_alreadyCancelled_throwsException() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelAdminBooking(100L));
    }

    @Test
    void testCancelAdminBooking_notFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelAdminBooking(99L));
    }

    @Test
    void testCancelUserBooking_bookingNotFound() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelUserBooking(99L, "user1"));
    }

    @Test
    void testCancelUserBooking_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelUserBooking(100L, "unknown"));
    }

    @Test
    void testGetBookingById_success() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingResponseDto response = bookingService.getBookingById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    void testGetBookingById_notFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingById(99L));
    }

    @Test
    void testGetBookingsByUserId() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));

        List<BookingResponseDto> results = bookingService.getBookingsByUserId(1L);

        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getId());
    }
}
