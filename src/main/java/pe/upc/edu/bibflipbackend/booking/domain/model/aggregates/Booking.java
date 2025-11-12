package pe.upc.edu.bibflipbackend.booking.domain.model.aggregates;

import lombok.Setter;
import pe.upc.edu.bibflipbackend.booking.domain.model.entities.BookingSlot;
import pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects.UserId;
import pe.upc.edu.bibflipbackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Entity
@Setter
public class Booking extends AuditableAbstractAggregateRoot<Booking> {

    // Identificador del cliente (Value Object)
    @Column(name = "client_id", nullable = false)
    @Embedded
    private UserId userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cubicle_id", nullable = false)
    private Cubicle cubicleId;

    // Fecha de la reserva (para agrupar y filtrar reservas por día)
    @Column(nullable = false)
    private LocalDate bookingDate;

    // Los intervalos reservados (BookingSlot es una entidad de detalle)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Set<BookingSlot> bookingSlots;

    protected Booking() {
        // Constructor para JPA
    }

    public Booking(UserId userId, Cubicle cubicle, LocalDate bookingDate, Set<BookingSlot> bookingSlots) {
        this.userId = userId;
        this.cubicleId = cubicle;
        this.bookingDate = bookingDate;
        this.bookingSlots = bookingSlots;
    }

    // Aquí se pueden agregar reglas de dominio adicionales, por ejemplo,
    // la validación de que la reserva no supere las 2 horas totales.

}
