package ledance.dto.stock;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Stock;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class StockMapperImpl implements StockMapper {

    @Override
    public StockResponse toDTO(Stock stock) {
        if ( stock == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        Double precio = null;
        Integer stock1 = null;
        Boolean requiereControlDeStock = null;
        Boolean activo = null;
        LocalDate fechaIngreso = null;
        LocalDate fechaEgreso = null;

        id = stock.getId();
        nombre = stock.getNombre();
        precio = stock.getPrecio();
        stock1 = stock.getStock();
        requiereControlDeStock = stock.getRequiereControlDeStock();
        activo = stock.getActivo();
        fechaIngreso = stock.getFechaIngreso();
        fechaEgreso = stock.getFechaEgreso();

        StockResponse stockResponse = new StockResponse( id, nombre, precio, stock1, requiereControlDeStock, activo, fechaIngreso, fechaEgreso );

        return stockResponse;
    }

    @Override
    public Stock toEntity(StockRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Stock stock = new Stock();

        stock.setNombre( request.nombre() );
        stock.setPrecio( request.precio() );
        stock.setStock( request.stock() );
        stock.setRequiereControlDeStock( request.requiereControlDeStock() );
        stock.setFechaIngreso( request.fechaIngreso() );
        stock.setFechaEgreso( request.fechaEgreso() );

        stock.setActivo( true );

        return stock;
    }

    @Override
    public void updateEntityFromRequest(StockRegistroRequest request, Stock stock) {
        if ( request == null ) {
            return;
        }

        stock.setNombre( request.nombre() );
        stock.setPrecio( request.precio() );
        stock.setStock( request.stock() );
        stock.setRequiereControlDeStock( request.requiereControlDeStock() );
        stock.setActivo( request.activo() );
        stock.setFechaIngreso( request.fechaIngreso() );
        stock.setFechaEgreso( request.fechaEgreso() );
    }
}
