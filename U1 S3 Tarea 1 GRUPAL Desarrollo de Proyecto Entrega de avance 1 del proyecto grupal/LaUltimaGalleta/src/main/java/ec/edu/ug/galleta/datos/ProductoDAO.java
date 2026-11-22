package ec.edu.ug.galleta.datos;

import ec.edu.ug.galleta.modelo.Producto;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {
    private final Conexion conexion;
    public ProductoDAO(Conexion conexion) { this.conexion = conexion; }

    public List<Producto> listar(boolean soloActivos) throws SQLException {
        String sql = "SELECT * FROM producto" + (soloActivos ? " WHERE activo=1" : "") + " ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) productos.add(leer(rs));
        }
        return productos;
    }

    public Producto buscar(int id) throws SQLException {
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement("SELECT * FROM producto WHERE id=?")) {
            ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? leer(rs) : null; }
        }
    }

    private Producto leer(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("id"), rs.getString("nombre"),
                BigDecimal.valueOf(rs.getLong("precio_centavos"),2), rs.getBoolean("activo"));
    }

    public void guardar(int id, String nombre, BigDecimal precio) throws SQLException {
        // El precio se almacena en centavos para evitar errores de redondeo.
        String sql = id == 0 ? "INSERT INTO producto(nombre,precio_centavos) VALUES(?,?)"
                : "UPDATE producto SET nombre=?,precio_centavos=? WHERE id=?";
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1,nombre); ps.setLong(2,precio.movePointRight(2).longValueExact());
            if (id != 0) ps.setInt(3,id);
            if (ps.executeUpdate() != 1) throw new SQLException("Producto no encontrado.");
        }
    }

    // No se elimina el producto, porque puede estar en pedidos anteriores.
    public void desactivar(int id) throws SQLException {
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement("UPDATE producto SET activo=0 WHERE id=?")) {
            ps.setInt(1,id);
            if (ps.executeUpdate() != 1) throw new SQLException("Producto no encontrado.");
        }
    }
}
