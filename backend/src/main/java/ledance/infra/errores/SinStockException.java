package ledance.infra.errores;

public class SinStockException extends RuntimeException {
    public SinStockException(String mensaje) {
        super(mensaje);
    }
}
