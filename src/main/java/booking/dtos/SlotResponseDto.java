package booking.dtos;

import booking.enums.SlotStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponseDto {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SlotStatus status;
}
