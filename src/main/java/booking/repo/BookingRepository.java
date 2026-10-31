package booking.repo;

import booking.entity.Booking;
import booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findBySlotId(Long slotId);
    Optional<Booking> findBySlotIdAndStatus(Long slotId, BookingStatus status);
}
