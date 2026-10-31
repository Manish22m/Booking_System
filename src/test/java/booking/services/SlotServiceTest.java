package booking.services;

import booking.dtos.CreateSlotRequest;
import booking.dtos.SlotResponseDto;
import booking.entity.Slot;
import booking.enums.SlotStatus;
import booking.exception.ResourceNotFoundException;
import booking.repo.SlotRepository;
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
class SlotServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private SlotService slotService;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2026, 11, 1, 10, 0);
        endTime = LocalDateTime.of(2026, 11, 1, 11, 0);
    }

    @Test
    void testCreateSlot_success() {
        CreateSlotRequest request = CreateSlotRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Slot savedSlot = Slot.builder()
                .id(1L)
                .startTime(startTime)
                .endTime(endTime)
                .status(SlotStatus.AVAILABLE)
                .build();

        when(slotRepository.save(any(Slot.class))).thenReturn(savedSlot);

        SlotResponseDto response = slotService.createSlot(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(SlotStatus.AVAILABLE, response.getStatus());
        verify(slotRepository, times(1)).save(any(Slot.class));
    }

    @Test
    void testCreateSlot_invalidTimes_throwsException() {
        CreateSlotRequest request = CreateSlotRequest.builder()
                .startTime(endTime)
                .endTime(startTime)
                .build();

        assertThrows(IllegalArgumentException.class, () -> slotService.createSlot(request));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void testGetAllSlots_noFilter() {
        Slot slot1 = Slot.builder().id(1L).startTime(startTime).endTime(endTime).status(SlotStatus.AVAILABLE).build();
        Slot slot2 = Slot.builder().id(2L).startTime(startTime.plusHours(1)).endTime(endTime.plusHours(1)).status(SlotStatus.BOOKED).build();

        when(slotRepository.findAll()).thenReturn(List.of(slot1, slot2));

        List<SlotResponseDto> result = slotService.getAllSlots(null);

        assertEquals(2, result.size());
        verify(slotRepository, times(1)).findAll();
    }

    @Test
    void testGetAllSlots_withFilter() {
        Slot slot1 = Slot.builder().id(1L).startTime(startTime).endTime(endTime).status(SlotStatus.AVAILABLE).build();

        when(slotRepository.findByStatus(SlotStatus.AVAILABLE)).thenReturn(List.of(slot1));

        List<SlotResponseDto> result = slotService.getAllSlots(SlotStatus.AVAILABLE);

        assertEquals(1, result.size());
        assertEquals(SlotStatus.AVAILABLE, result.get(0).getStatus());
        verify(slotRepository, times(1)).findByStatus(SlotStatus.AVAILABLE);
    }

    @Test
    void testGetSlotById_success() {
        Slot slot = Slot.builder().id(1L).startTime(startTime).endTime(endTime).status(SlotStatus.AVAILABLE).build();
        when(slotRepository.findById(1L)).thenReturn(Optional.of(slot));

        SlotResponseDto response = slotService.getSlotById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void testGetSlotById_notFound() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> slotService.getSlotById(99L));
    }
}
