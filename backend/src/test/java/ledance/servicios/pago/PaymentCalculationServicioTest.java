package ledance.servicios.pago;

import ledance.servicios.stock.StockServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentCalculationServicioTest {

    private PaymentCalculationServicio servicio;

    @BeforeEach
    public void setUp() {
        StockServicio stockServicio = Mockito.mock(StockServicio.class);
        servicio = new PaymentCalculationServicio(stockServicio);
    }

    private int invokeParseCantidad(String value) throws Exception {
        Method m = PaymentCalculationServicio.class.getDeclaredMethod("parseCantidad", String.class);
        m.setAccessible(true);
        return (int) m.invoke(servicio, value);
    }

    @Test
    public void parseCantidadNumero() throws Exception {
        int result = invokeParseCantidad(" 3 ");
        assertEquals(3, result);
    }

    @Test
    public void parseCantidadNoNumero() throws Exception {
        int result = invokeParseCantidad("no-numero");
        assertEquals(1, result);
    }
}
