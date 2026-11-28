package ec.edu.ug.galleta.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Pedido(int id, Cliente cliente, LocalDateTime fecha,
                     EstadoPedido estado, List<DetallePedido> detalles) {
    public Pedido {
        detalles = List.copyOf(detalles);
    }

    // El total se calcula a partir de los detalles guardados.
    public BigDecimal calcularTotal() {
        BigDecimal total = new BigDecimal("0.00");
        for (DetallePedido detalle : detalles) {
            total = total.add(detalle.calcularSubtotal());
        }
        return total;
    }
}
