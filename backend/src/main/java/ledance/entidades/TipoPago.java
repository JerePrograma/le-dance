package ledance.entidades;

public enum TipoPago {
    SUBSCRIPTION,  // Pago asociado a una inscripcion (cuotas, mensualidades, matricula)
    GENERAL,       // Pago independiente (facturacion general, conceptos o stocks)
    RESUMEN        // Pago generado a partir de una actualizacion historica, se usa para conservar los abonos ya aplicados
}
