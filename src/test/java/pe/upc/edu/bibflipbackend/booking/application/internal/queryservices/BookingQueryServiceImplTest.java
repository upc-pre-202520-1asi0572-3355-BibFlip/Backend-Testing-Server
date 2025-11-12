package pe.upc.edu.bibflipbackend.booking.application.internal.queryservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Booking;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetAllBookingsByIdClientQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetAllBookingsQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.UserId;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.BookingRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingQueryServiceImplTest {
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingQueryServiceImpl bookingQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void handleGetAllBookings_ShouldReturnBookings_WhenBookingsExist() {
        // Arrange
        Booking booking1 = mock(Booking.class);
        Booking booking2 = mock(Booking.class);
        when(bookingRepository.findAll()).thenReturn(List.of(booking1, booking2));

        // Act
        List<Booking> result = bookingQueryService.handle(new GetAllBookingsQuery());

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    void handleGetAllBookings_ShouldThrowException_WhenNoBookingsFound() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingQueryService.handle(new GetAllBookingsQuery())
        );

        assertEquals("No bookings found", exception.getMessage());
        verify(bookingRepository, times(1)).findAll();
    }


    @Test
    void handleGetAllBookingsByIdClient_ShouldReturnBookings_WhenClientHasBookings() {
        // Arrange
        Long clientId = 123L;
        Booking booking1 = mock(Booking.class);
        Booking booking2 = mock(Booking.class);
        when(bookingRepository.findAllByUserId(new UserId(clientId)))
                .thenReturn(List.of(booking1, booking2));

        GetAllBookingsByIdClientQuery query = new GetAllBookingsByIdClientQuery(clientId);

        // Act
        List<Booking> result = bookingQueryService.handle(query);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository, times(1)).findAllByUserId(new UserId(clientId));
    }

    @Test
    void handleGetAllBookingsByIdClient_ShouldThrowException_WhenClientHasNoBookings() {
        // Arrange
        Long clientId = 999L;
        when(bookingRepository.findAllByUserId(new UserId(clientId)))
                .thenReturn(List.of());

        GetAllBookingsByIdClientQuery query = new GetAllBookingsByIdClientQuery(clientId);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingQueryService.handle(query)
        );

        assertEquals("No bookings found for client ID: " + clientId, exception.getMessage());
        verify(bookingRepository, times(1)).findAllByUserId(new UserId(clientId));
    }
}
