package pe.upc.edu.bibflipbackend.booking.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Booking;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Cubicle;
import pe.upc.edu.bibflipbackend.booking.domain.model.commands.DeleteBookingCommand;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.AvailabilitySlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.BookingSlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.*;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.BookingRepository;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.CubicleRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingCommandService - Eliminar reserva")
class BookingCommandServiceImplDeleteTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CubicleRepository cubicleRepository;

    @InjectMocks
    private BookingCommandServiceImpl bookingCommandService;

    @Test
    @DisplayName("Debe eliminar reserva exitosamente y liberar slots")
    void testDeleteBookingSuccess() {
        // Arrange
        Long bookingId = 1L;
        Long cubicleId = 10L;
        LocalDate bookingDate = LocalDate.now();

        Booking mockBooking = mock(Booking.class);
        Cubicle mockCubicleEntity = mock(Cubicle.class);

        when(mockBooking.getId()).thenReturn(bookingId);
        when(mockBooking.getCubicleId()).thenReturn(mockCubicleEntity);
        when(mockCubicleEntity.getId()).thenReturn(cubicleId);
        when(mockBooking.getBookingDate()).thenReturn(bookingDate);

        Set<BookingSlot> bookingSlots = Set.of(
                new BookingSlot(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)))
        );
        when(mockBooking.getBookingSlots()).thenReturn(bookingSlots);

        Cubicle mockCubicle = new Cubicle(
                new CubicleDetails(101, 4),
                new HeadquarterId(1L),
                CubicleZone.HALL_B
        );

        AvailabilitySlot slot = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        slot.updateStatus(ScheduleSlotStatus.RESERVED);
        mockCubicle.getAvailabilitySlots().add(slot);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.of(mockCubicle));

        DeleteBookingCommand command = new DeleteBookingCommand(bookingId);

        // Act
        bookingCommandService.handle(command);

        // Assert
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(cubicleRepository, times(1)).findByIdWithSlotsForUpdate(cubicleId);
        verify(cubicleRepository, times(1)).save(mockCubicle);
        verify(bookingRepository, times(1)).delete(mockBooking);

        // Verificar que el slot volvió a AVAILABLE
        assertEquals(ScheduleSlotStatus.AVAILABLE, slot.getStatus());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la reserva no existe")
    void testDeleteBooking_NotFound() {
        // Arrange
        Long bookingId = 999L;
        DeleteBookingCommand command = new DeleteBookingCommand(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("Booking not found"));
        verify(bookingRepository, never()).delete(any());
    }
}
