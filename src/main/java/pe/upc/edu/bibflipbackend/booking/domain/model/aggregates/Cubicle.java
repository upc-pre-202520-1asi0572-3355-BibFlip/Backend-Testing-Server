package pe.upc.edu.bibflipbackend.booking.domain.model.aggregates;

import lombok.Setter;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.AvailabilitySlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.CubicleZone;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.HeadquarterId;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.CubicleDetails;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.CubicleStatus;
import pe.upc.edu.bibflipbackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Setter
public class Cubicle extends AuditableAbstractAggregateRoot<Cubicle> {

    @Embedded
    private CubicleDetails cubicleDetails;

    // Referencia a la sede (headquarters) de la cafetería a la que pertenece la mesa
    @Column(nullable = false)
    @Embedded
    private HeadquarterId headquarterId;

    @Setter
    @Enumerated(EnumType.STRING)
    private CubicleStatus status;

    // Nueva propiedad para la zona de la mesa
    @Setter
    @Enumerated(EnumType.STRING)
    private CubicleZone zone;

    // Cada mesa posee un conjunto de ScheduleSlot generados para cada día
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cubicle_id")
    private Set<AvailabilitySlot> availabilitySlots;

    protected Cubicle() {
        // Constructor para JPA
    }

    public Cubicle(CubicleDetails cubicleDetails, HeadquarterId headquarterId, CubicleZone zone) {
        this.cubicleDetails = cubicleDetails;
        this.headquarterId = headquarterId;
        this.zone = zone;
        this.availabilitySlots = new HashSet<>();
        this.status = CubicleStatus.AVAILABLE;
    }

    public Cubicle(CubicleDetails cubicleDetails, HeadquarterId headquarterId) {
        this(cubicleDetails, headquarterId, CubicleZone.MAIN_HALL); // Por defecto, sala principal
    }

}
