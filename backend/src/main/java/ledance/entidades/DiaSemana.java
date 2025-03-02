package ledance.entidades;

public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

    public java.time.DayOfWeek toDayOfWeek() {
        return switch (this) {
            case LUNES -> java.time.DayOfWeek.MONDAY;
            case MARTES -> java.time.DayOfWeek.TUESDAY;
            case MIERCOLES -> java.time.DayOfWeek.WEDNESDAY;
            case JUEVES -> java.time.DayOfWeek.THURSDAY;
            case VIERNES -> java.time.DayOfWeek.FRIDAY;
            case SABADO -> java.time.DayOfWeek.SATURDAY;
            case DOMINGO -> java.time.DayOfWeek.SUNDAY;
        };
    }
}
