package booking.controller;

import booking.dtos.CreateSlotRequest;
import booking.dtos.SlotResponseDto;
import booking.enums.SlotStatus;
import booking.services.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/slots")
public class SlotController {
	@Autowired
    private final SlotService slotService;

    @PostMapping("/add")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlotResponseDto> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        SlotResponseDto response = slotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<SlotResponseDto>> getAllSlots(
            @RequestParam(required = false) SlotStatus status) {
        List<SlotResponseDto> slots = slotService.getAllSlots(status);
        return ResponseEntity.ok(slots);
    }
}
