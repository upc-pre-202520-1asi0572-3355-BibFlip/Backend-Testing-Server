package pe.upc.edu.bibflipbackend.booking.domain.model.valueobjects;

import lombok.Getter;

@Getter
public enum CubicleZone {
    MAIN_HALL("pabellón principal"),
    HALL_B("pabellón B"),
    HALL_C("pabellón C");

    private final String name;

    CubicleZone(String name) {
        this.name = name;
    }

    // Para convertir de String a enum
    public static CubicleZone fromString(String text) {
        for (CubicleZone zone : CubicleZone.values()) {
            if (zone.name.equalsIgnoreCase(text)) {
                return zone;
            }
        }
        return MAIN_HALL; // valor por defecto
    }
} 