package ledance.entidades;

public enum TipoPago {
    SUBSCRIPTION,  // Pago asociado a una inscripción (cuotas, mensualidades, matrícula)
    GENERAL,       // Pago independiente (facturación general, conceptos o stocks)
    RESUMEN        // Pago generado a partir de una actualización histórica, se usa para conservar los abonos ya aplicados
}
