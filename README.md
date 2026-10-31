## 1. Chosen Locking Strategy

I chose **Database-Level Pessimistic Locking (`PESSIMISTIC_WRITE`)** instead of Optimistic Locking.

### Why Pessimistic Locking?
- **No wasted retries under high traffic**: With optimistic locking (using `@Version`), if 20 users try to book the same slot at the same second, 19 of them fail with `OptimisticLockException`. To fix that, we would have to write retry logic, which wastes server CPU and database calls.
- **Clean and deterministic**: With pessimistic locking, the first user's request locks the slot row for a few milliseconds. Other requests simply wait in line. Once the first user finishes booking, the next user reads the updated status (`BOOKED`) and gets a clean `409 Conflict` error ("Slot is already booked").
- **Database-level guarantee**: It uses native database row locking (`SELECT ... FOR UPDATE`), which fulfills the requirement of not using in-memory locks like Java `synchronized`.

### Code Implementation (`SlotRepository.java`):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Slot s WHERE s.id = :id")
Optional<Slot> findByIdWithLock(@Param("id") Long id);

2. Transaction Boundaries
All booking and cancellation logic is wrapped inside Spring's @Transactional(isolation = Isolation.READ_COMMITTED) boundary in BookingService.java.
What happens inside this boundary:
-Fetch user from the database.
-Lock the slot row using findByIdWithLock(slotId).
-Validate status: Check if slot.getStatus() == SlotStatus.AVAILABLE.
-Update slot status to BOOKED.
-Create & save the Booking record.

Why this boundary is important:
Atomicity (All or Nothing): Updating the slot status and inserting the booking record happen together in one transaction. If saving fails at any point, the entire transaction rolls back automatically, so we never have a slot marked as booked without a booking record.
Lock duration: The database lock stays active for the entire duration of the method and is only released once the transaction commits or rolls back.

3. How Race Conditions are Prevented
A race condition happens when two users (User A and User B) try to book the exact same slot at the exact same millisecond.

Here is how my code prevents double booking step-by-step:

Both requests arrive: User A and User B both call POST /bookings for slotId = 1.
User A acquires the lock: User A's thread runs findByIdWithLock(1). The database grants an exclusive lock on that slot row.
User B waits: User B's thread also runs findByIdWithLock(1). Because User A holds the row lock, the database puts User B on wait.
User A books the slot: User A checks that status is AVAILABLE, changes it to BOOKED, saves the booking, and commits the transaction. The row lock is now released.
User B gets the lock: User B unblocks and reads the slot row. But now the status is already BOOKED.
User B is rejected: User B's code checks if (slot.getStatus() != SlotStatus.AVAILABLE) and throws SlotAlreadyBookedException.
Clean error response: GlobalExceptionHandler catches this exception and returns HTTP 409 Conflict with message "Slot is already booked".
Result: Only 1 booking is created in the database, and double booking is 100% prevented.
