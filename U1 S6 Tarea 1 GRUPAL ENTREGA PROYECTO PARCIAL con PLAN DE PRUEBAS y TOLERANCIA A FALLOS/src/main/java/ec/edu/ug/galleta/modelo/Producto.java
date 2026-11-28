package ec.edu.ug.galleta.modelo;

import java.math.BigDecimal;

// Producto del catálogo. El precio se maneja con dos decimales.
public record Producto(int id, String nombre, BigDecimal precio, boolean activo) {
    @Override
    public String toString() {
        return nombre + " - $" + precio.toPlainString();
    }
}
