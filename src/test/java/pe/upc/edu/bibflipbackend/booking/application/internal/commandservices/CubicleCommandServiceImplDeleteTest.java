package pe.upc.edu.bibflipbackend.booking.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.upc.edu.bibflipbackend.booking.domain.model.aggregates.Cubicle;

import pe.upc.edu.bibflipbackend.booking.domain.model.commands.DeleteCubicleCommand;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.*;
import pe.upc.edu.bibflipbackend.booking.infrastructure.persistence.jpa.repositories.CubicleRepository;
import pe.upc.edu.bibflipbackend.shared.application.exceptions.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubicleCommandService - US015: Eliminar cubículos")
class CubicleCommandServiceImplDeleteTest {

    @Mock
    private CubicleRepository cubicleRepository;

    @InjectMocks
    private CubicleCommandServiceImpl cubicleCommandService;

    @Test
    @DisplayName("Debe marcar cubículo como DELETED exitosamente")
    void testDeleteCubicleSuccess() {
        // Arrange
        Long cubicleId = 1L;
        Cubicle mockCubicle = mock(Cubicle.class);

        when(cubicleRepository.findById(cubicleId)).thenReturn(Optional.of(mockCubicle));

        DeleteCubicleCommand command = new DeleteCubicleCommand(cubicleId);

        // Act
        cubicleCommandService.handle(command);

        // Assert
        verify(cubicleRepository, times(1)).findById(cubicleId);
        verify(mockCubicle, times(1)).setStatus(CubicleStatus.DELETED);
        verify(cubicleRepository, times(1)).save(mockCubicle);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el cubículo no existe")
    void testDeleteCubicle_NotFound() {
        // Arrange
        Long cubicleId = 999L;
        DeleteCubicleCommand command = new DeleteCubicleCommand(cubicleId);

        when(cubicleRepository.findById(cubicleId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> cubicleCommandService.handle(command)
        );

        assertTrue(exception.getMessage().contains("Cubicle with ID"));
        verify(cubicleRepository, never()).save(any());
    }

}
