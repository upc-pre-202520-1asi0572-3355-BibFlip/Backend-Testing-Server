package pe.upc.edu.bibflipbackend.booking.application.internal.commandservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.upc.edu.bibflipbackend.booking.application.internal.outboundedservices.acl.ExternalUserService;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Booking;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Cubicle;
import pe.upc.edu.bibflipbackend.booking.domain.model.commands.CreateBookingCommand;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.AvailabilitySlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.*;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.BookingRepository;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.CubicleRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.InvalidValueException;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingCommandService - US008: Reservar un cubículo")
class BookingCommandServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CubicleRepository cubicleRepository;

    @Mock
    private ExternalUserService externalUserService;

    @InjectMocks
    private BookingCommandServiceImpl bookingCommandService;

    private Cubicle mockCubicle;
    private List<Long> slotIds;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        bookingDate = LocalDate.now();
        slotIds = List.of(1L, 2L);

        mockCubicle = new Cubicle(
                new CubicleDetails(101, 4),
                new HeadquarterId(1L),
                CubicleZone.HALL_B
        );
        ReflectionTestUtils.setField(mockCubicle, "id", 1L); // Set the ID of the mock cubicle

        AvailabilitySlot slot1 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        AvailabilitySlot slot2 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(11, 0), LocalTime.of(12, 0)));

        slot1.setId(1L);
        slot2.setId(2L);

        Set<AvailabilitySlot> slots = new HashSet<>();
        slots.add(slot1);
        slots.add(slot2);
        mockCubicle.setAvailabilitySlots(slots);
    }

    @Test
    @DisplayName("Debe crear reserva exitosamente con slots consecutivos y disponibles")
    void testCreateBookingSuccess() {
        // Arrange
        Long clientId = 1L;
        Long cubicleId = 1L;
        CreateBookingCommand command = new CreateBookingCommand(clientId, cubicleId, bookingDate, slotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(true);
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.of(mockCubicle));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            try {
                booking.setId(1L);
            } catch (Exception e) {
                ReflectionTestUtils.setField(booking, "id", 1L);
            }
            return booking;
        });

        Optional<Booking> result = bookingCommandService.handle(command);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(clientId, result.get().getUserId().clientId());
        assertEquals(cubicleId, result.get().getCubicleId().getId());
        assertEquals(2, result.get().getBookingSlots().size());

        mockCubicle.getAvailabilitySlots().forEach(slot ->
                assertEquals(ScheduleSlotStatus.RESERVED, slot.getStatus())
        );

        verify(externalUserService, times(1)).existUserById(clientId);
        verify(cubicleRepository, times(1)).findByIdWithSlotsForUpdate(cubicleId);
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(cubicleRepository, times(1)).save(mockCubicle);
    }


    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario no existe")
    void testCreateBooking_UserDoesNotExist() {
        Long clientId = 100L;
        CreateBookingCommand command = new CreateBookingCommand(clientId, 1L, bookingDate, slotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingCommandService.handle(command)
        );

        assertEquals("User does not exist", exception.getMessage());
        verify(externalUserService, times(1)).existUserById(clientId);
        verify(cubicleRepository, never()).findByIdWithSlotsForUpdate(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el cubículo no existe")
    void testCreateBooking_CubicleNotFound() {
        Long clientId = 100L;
        Long cubicleId = 999L;
        CreateBookingCommand command = new CreateBookingCommand(clientId, cubicleId, bookingDate, slotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(true);
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("Cubicle not found"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando los slots no son consecutivos")
    void testCreateBooking_NonConsecutiveSlots() {
        Long clientId = 100L;
        Long cubicleId = 1L;

        AvailabilitySlot slot1 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        AvailabilitySlot slot2 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(12, 0), LocalTime.of(13, 0))); // GAP
        slot1.setId(1L);
        slot2.setId(2L);

        Set<AvailabilitySlot> slots = new HashSet<>();
        slots.add(slot1);
        slots.add(slot2);
        mockCubicle.setAvailabilitySlots(slots);

        CreateBookingCommand command = new CreateBookingCommand(clientId, cubicleId, bookingDate, slotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(true);
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.of(mockCubicle));

        InvalidValueException exception = assertThrows(
                InvalidValueException.class,
                () -> bookingCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("consecutive"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la duración excede 120 minutos")
    void testCreateBooking_ExceedsMaxDuration() {
        Long clientId = 100L;
        Long cubicleId = 1L;

        AvailabilitySlot slot1 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        AvailabilitySlot slot2 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(11, 0), LocalTime.of(12, 0)));
        AvailabilitySlot slot3 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(12, 0), LocalTime.of(13, 0)));
        slot1.setId(1L);
        slot2.setId(2L);
        slot3.setId(3L);

        Set<AvailabilitySlot> slots = new HashSet<>();
        slots.add(slot1);
        slots.add(slot2);
        slots.add(slot3);
        mockCubicle.setAvailabilitySlots(slots);

        List<Long> longSlotIds = List.of(1L, 2L, 3L);
        CreateBookingCommand command = new CreateBookingCommand(clientId, cubicleId, bookingDate, longSlotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(true);
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.of(mockCubicle));

        InvalidValueException exception = assertThrows(
                InvalidValueException.class,
                () -> bookingCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("Cannot book more than 2 hours"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando algún slot no está disponible")
    void testCreateBooking_SlotNotAvailable() {
        Long clientId = 100L;
        Long cubicleId = 1L;

        AvailabilitySlot slot1 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        AvailabilitySlot slot2 = new AvailabilitySlot(bookingDate, new TimeSlot(LocalTime.of(11, 0), LocalTime.of(12, 0)));
        slot1.setId(1L);
        slot2.setId(2L);

        slot2.updateStatus(ScheduleSlotStatus.RESERVED);

        Set<AvailabilitySlot> slots = new HashSet<>();
        slots.add(slot1);
        slots.add(slot2);
        mockCubicle.setAvailabilitySlots(slots);

        CreateBookingCommand command = new CreateBookingCommand(clientId, cubicleId, bookingDate, slotIds);

        when(externalUserService.existUserById(clientId)).thenReturn(true);
        when(cubicleRepository.findByIdWithSlotsForUpdate(cubicleId)).thenReturn(Optional.of(mockCubicle));

        InvalidValueException exception = assertThrows(
                InvalidValueException.class,
                () -> bookingCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("not available for booking"));
        verify(bookingRepository, never()).save(any());
    }

}
