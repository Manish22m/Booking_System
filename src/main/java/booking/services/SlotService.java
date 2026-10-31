package booking.services;

import booking.dtos.CreateSlotRequest;
import booking.dtos.SlotResponseDto;
import booking.entity.Slot;
import booking.enums.SlotStatus;
import booking.exception.ResourceNotFoundException;
import booking.repo.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;

    @Transactional
    public SlotResponseDto createSlot(CreateSlotRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Slot slot = Slot.builder()
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(SlotStatus.AVAILABLE)
                .build();

        Slot savedSlot = slotRepository.save(slot);
        return mapToDto(savedSlot);
    }

    @Transactional(readOnly = true)
    public List<SlotResponseDto> getAllSlots(SlotStatus status) {
        List<Slot> slots;
        if (status != null) {
            slots = slotRepository.findByStatus(status);
        } else {
            slots = slotRepository.findAll();
        }
        return slots.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SlotResponseDto getSlotById(Long id) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + id));
        return mapToDto(slot);
    }

    private SlotResponseDto mapToDto(Slot slot) {
        return SlotResponseDto.builder()
                .id(slot.getId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .build();
    }
}
