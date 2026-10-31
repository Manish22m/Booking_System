package booking.dtos;

import booking.enums.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {
    private Long id;
    private Long slotId;
    private Long userId;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
