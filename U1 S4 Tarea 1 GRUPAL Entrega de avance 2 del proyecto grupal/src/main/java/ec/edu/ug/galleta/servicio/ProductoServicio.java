package ec.edu.ug.galleta.servicio;

import ec.edu.ug.galleta.datos.ProductoDAO;
import ec.edu.ug.galleta.modelo.Producto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

// Reglas de los productos antes de almacenar el catálogo.
public class ProductoServicio {
    private final ProductoDAO dao;
    public ProductoServicio(ProductoDAO dao) { this.dao = dao; }

    public void guardar(int id, String nombre, String precioTexto) throws SQLException {
        if (nombre.isBlank()) throw new IllegalArgumentException("Ingrese el nombre del producto.");
        BigDecimal precio;
        // Se aceptan precios con coma o punto decimal, pero no se redondean.
        try {
            precio = new BigDecimal(precioTexto.trim().replace(',', '.')).setScale(2,RoundingMode.UNNECESSARY);
            if (precio.signum() <= 0 || precio.compareTo(new BigDecimal("999999.99")) > 0) throw new ArithmeticException();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Ingrese un precio positivo con máximo dos decimales.");
        }
        dao.guardar(id,nombre.trim(),precio);
    }
    public List<Producto> listar(boolean activos) throws SQLException { return dao.listar(activos); }
    public void desactivar(int id) throws SQLException { dao.desactivar(id); }
}
