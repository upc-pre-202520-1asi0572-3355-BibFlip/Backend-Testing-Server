package pe.upc.edu.bibflipbackend.booking.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.upc.edu.bibflipbackend.booking.application.internal.outboundedservices.acl.ExternalHeadquarterService;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Cubicle;
import pe.upc.edu.bibflipbackend.booking.domain.model.commands.CreateCubicleCommand;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.CubicleStatus;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.HeadquarterId;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.CubicleRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceAlreadyException;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubicleCommandService - US012: Agregar nuevos cubículos")
class CubicleCommandServiceImplTest {

    @Mock
    private CubicleRepository cubicleRepository;

    @Mock
    private ExternalHeadquarterService externalHeadquarterService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CubicleCommandServiceImpl cubicleCommandService;

    @Test
    @DisplayName("Debe crear cubículo exitosamente")
    void testCreateCubicleSuccess() {
        // Arrange
        Long headquarterId = 1L;
        Integer cubicleNumber = 101;
        Integer seats = 4;
        String zone = "ZONE_A";

        CreateCubicleCommand command = new CreateCubicleCommand(cubicleNumber,seats, headquarterId, zone);

        when(externalHeadquarterService.existsHeadquarter(headquarterId)).thenReturn(true);
        when(cubicleRepository.existsByHeadquarterIdAndCubicleDetails_CubicleNumberAndStatusNot(
                any(HeadquarterId.class), eq(cubicleNumber), eq(CubicleStatus.DELETED)
        )).thenReturn(false);
        when(cubicleRepository.save(any(Cubicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Cubicle> result = cubicleCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(cubicleNumber, result.get().getCubicleDetails().cubicleNumber());
        assertEquals(seats, result.get().getCubicleDetails().seats());

        verify(externalHeadquarterService, times(1)).existsHeadquarter(headquarterId);
        verify(cubicleRepository, times(1)).save(any(Cubicle.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la sede no existe")
    void testCreateCubicle_HeadquarterNotFound() {
        // Arrange
        Long headquarterId = 999L;
        CreateCubicleCommand command = new CreateCubicleCommand( 101, 4,headquarterId, "ZONE_A");

        when(externalHeadquarterService.existsHeadquarter(headquarterId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cubicleCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("Headquarter with ID"));
        verify(cubicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el cubículo ya existe")
    void testCreateCubicle_AlreadyExists() {
        // Arrange
        Long headquarterId = 1L;
        Integer cubicleNumber = 101;
        CreateCubicleCommand command = new CreateCubicleCommand( cubicleNumber, 4, headquarterId,"ZONE_A");

        when(externalHeadquarterService.existsHeadquarter(headquarterId)).thenReturn(true);
        when(cubicleRepository.existsByHeadquarterIdAndCubicleDetails_CubicleNumberAndStatusNot(
                any(HeadquarterId.class), eq(cubicleNumber), eq(CubicleStatus.DELETED)
        )).thenReturn(true);

        // Act & Assert
        ResourceAlreadyException exception = assertThrows(
                ResourceAlreadyException.class,
                () -> cubicleCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(cubicleRepository, never()).save(any());
    }
}
