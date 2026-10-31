package booking.controller;

import booking.dtos.BookingResponseDto;
import booking.dtos.CreateBookingRequest;
import booking.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/slot")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponseDto> bookSlot(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        BookingResponseDto response = bookingService.bookSlot(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        BookingResponseDto response = bookingService.cancelUserBooking(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
