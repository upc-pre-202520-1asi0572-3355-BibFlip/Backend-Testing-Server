package pe.upc.edu.bibflipbackend.booking.application.internal.queryservices;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Cubicle;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.AvailabilitySlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetAllCubicleByHeadquarterIdQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetAllCubiclesQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetCubicleByIdQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.queries.GetCubicleScheduleByIdAndDateQuery;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.*;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.CubicleRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubicleQueryService - Visualizar cubículos y disponibilidad")
class CubicleQueryServiceImplTest {

    @Mock
    private CubicleRepository cubicleRepository;

    @InjectMocks
    private CubicleQueryServiceImpl cubicleQueryService;

    @Test
    @DisplayName("Debe retornar cubículo por ID exitosamente")
    void testGetCubicleById_Success() {
        // Arrange
        Long cubicleId = 1L;
        Cubicle mockCubicle = mock(Cubicle.class);
        when(mockCubicle.getStatus()).thenReturn(CubicleStatus.AVAILABLE);
        when(cubicleRepository.findById(cubicleId)).thenReturn(Optional.of(mockCubicle));

        GetCubicleByIdQuery query = new GetCubicleByIdQuery(cubicleId);

        // Act
        Optional<Cubicle> result = cubicleQueryService.handle(query);

        // Assert
        assertTrue(result.isPresent());
        verify(cubicleRepository, times(1)).findById(cubicleId);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando cubículo está DELETED")
    void testGetCubicleById_Deleted() {
        // Arrange
        Long cubicleId = 1L;
        Cubicle mockCubicle = mock(Cubicle.class);
        when(mockCubicle.getStatus()).thenReturn(CubicleStatus.DELETED);
        when(cubicleRepository.findById(cubicleId)).thenReturn(Optional.of(mockCubicle));

        GetCubicleByIdQuery query = new GetCubicleByIdQuery(cubicleId);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cubicleQueryService.handle(query)
        );

        assertTrue(exception.getMessage().contains("No cubiclefound with ID"));
    }

    @Test
    @DisplayName("Debe retornar slots de disponibilidad por fecha")
    void testGetCubicleScheduleByIdAndDate_Success() {
        // Arrange
        Long cubicleId = 1L;
        LocalDate date = LocalDate.now();

        List<AvailabilitySlot> mockSlots = new ArrayList<>(List.of(
                new AvailabilitySlot(date, new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0))),
                new AvailabilitySlot(date, new TimeSlot(LocalTime.of(11, 0), LocalTime.of(12, 0)))
        ));

        when(cubicleRepository.existsById(cubicleId)).thenReturn(true);
        when(cubicleRepository.findAvailabilitySlotsByCubicleIdAndDate(cubicleId, date)).thenReturn(mockSlots);

        GetCubicleScheduleByIdAndDateQuery query = new GetCubicleScheduleByIdAndDateQuery(cubicleId, date);

        // Act
        List<AvailabilitySlot> result = cubicleQueryService.handle(query);

        // Assert
        assertEquals(2, result.size());
        verify(cubicleRepository, times(1)).findAvailabilitySlotsByCubicleIdAndDate(cubicleId, date);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando cubículo no existe al buscar schedule")
    void testGetCubicleScheduleByIdAndDate_CubicleNotFound() {
        // Arrange
        Long cubicleId = 999L;
        LocalDate date = LocalDate.now();

        when(cubicleRepository.existsById(cubicleId)).thenReturn(false);

        GetCubicleScheduleByIdAndDateQuery query = new GetCubicleScheduleByIdAndDateQuery(cubicleId, date);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cubicleQueryService.handle(query)
        );

        assertTrue(exception.getMessage().contains("No cubiclefound with ID"));
        verify(cubicleRepository, never()).findAvailabilitySlotsByCubicleIdAndDate(any(), any());
    }

    @Test
    @DisplayName("Debe retornar cubículos por headquarter ID")
    void testGetAllCubiclesByHeadquarterId_Success() {
        // Arrange
        Long headquarterId = 1L;
        List<Cubicle> mockCubicles = List.of(mock(Cubicle.class), mock(Cubicle.class));

        when(cubicleRepository.findByHeadquarterIdAndStatusNot(any(HeadquarterId.class), eq(CubicleStatus.DELETED)))
                .thenReturn(mockCubicles);

        GetAllCubicleByHeadquarterIdQuery query = new GetAllCubicleByHeadquarterIdQuery(headquarterId);

        // Act
        List<Cubicle> result = cubicleQueryService.handle(query);

        // Assert
        assertEquals(2, result.size());
        verify(cubicleRepository, times(1)).findByHeadquarterIdAndStatusNot(any(HeadquarterId.class), eq(CubicleStatus.DELETED));
    }
}
