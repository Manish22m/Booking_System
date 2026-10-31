package booking.services;

import booking.dtos.BookingResponseDto;
import booking.dtos.CreateBookingRequest;
import booking.entity.Booking;
import booking.entity.Slot;
import booking.entity.User;
import booking.enums.BookingStatus;
import booking.enums.SlotStatus;
import booking.exception.ResourceNotFoundException;
import booking.exception.SlotAlreadyBookedException;
import booking.exception.UnauthorizedActionException;
import booking.repo.BookingRepository;
import booking.repo.SlotRepository;
import booking.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDto bookSlot(CreateBookingRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Acquire pessimistic write lock at database level
        Slot slot = slotRepository.findByIdWithLock(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + request.getSlotId()));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotAlreadyBookedException("Slot is already booked");
        }

        // Update slot status
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // Create booking record
        Booking booking = Booking.builder()
                .slot(slot)
                .user(user)
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return mapToDto(savedBooking);
    }

    @Transactional
    public BookingResponseDto cancelUserBooking(Long bookingId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You can cancel only your own booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        
        Slot slot = slotRepository.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated slot not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        slot.setStatus(SlotStatus.AVAILABLE);

        slotRepository.save(slot);
        Booking updatedBooking = bookingRepository.save(booking);

        return mapToDto(updatedBooking);
    }

    @Transactional
    public BookingResponseDto cancelAdminBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        
        Slot slot = slotRepository.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated slot not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        slot.setStatus(SlotStatus.AVAILABLE);

        slotRepository.save(slot);
        Booking updatedBooking = bookingRepository.save(booking);

        return mapToDto(updatedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return mapToDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private BookingResponseDto mapToDto(Booking booking) {
        return BookingResponseDto.builder()
                .id(booking.getId())
                .slotId(booking.getSlot().getId())
                .userId(booking.getUser().getId())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
