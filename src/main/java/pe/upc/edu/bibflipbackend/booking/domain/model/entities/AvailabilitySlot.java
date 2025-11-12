package pe.upc.edu.bibflipbackend.booking.domain.model.entities;

import lombok.Setter;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.ScheduleSlotStatus;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.TimeSlot;
import pe.upc.edu.bibflipbackend.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Setter
public class AvailabilitySlot extends AuditableModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fecha del slot
    @Column(nullable = false)
    private LocalDate dateOfSlot;

    // El slot se define mediante el TimeSlot que ya tienes embebido
    @Embedded
    private TimeSlot timeInterval;

    // Estado: DISPONIBLE, RESERVADO, OCUPADO
    @Enumerated(EnumType.STRING)
    private ScheduleSlotStatus status;

    protected AvailabilitySlot() {
        // Constructor para JPA
    }

    public AvailabilitySlot(LocalDate dateOfSlot, TimeSlot timeInterval) {
        this.dateOfSlot = dateOfSlot;
        this.timeInterval = timeInterval;
        this.status = ScheduleSlotStatus.AVAILABLE;
    }

    public void updateStatus(ScheduleSlotStatus status) {
        this.status = status;
    }

}