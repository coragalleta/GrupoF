package ec.edu.ug.galleta.modelo;

import java.math.BigDecimal;

// Se conserva el precio del momento en que se registra el pedido.
public record DetallePedido(int productoId, String nombreProducto, int cantidad,
                            BigDecimal precioUnitario) {
    public DetallePedido {
        if (cantidad <= 0 || precioUnitario.signum() <= 0) {
            throw new IllegalArgumentException("Cantidad y precio deben ser positivos.");
        }
    }

    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
