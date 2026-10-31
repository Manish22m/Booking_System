package booking.controller;

import booking.dtos.BookingResponseDto;
import booking.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;

    @PostMapping("/{id}/cancel")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long id) {
        BookingResponseDto response = bookingService.cancelAdminBooking(id);
        return ResponseEntity.ok(response);
    }
}
