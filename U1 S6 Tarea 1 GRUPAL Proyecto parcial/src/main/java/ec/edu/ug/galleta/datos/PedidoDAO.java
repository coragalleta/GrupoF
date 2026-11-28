package ec.edu.ug.galleta.datos;

import ec.edu.ug.galleta.modelo.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Guarda y consulta los pedidos y sus detalles en SQLite. */
public class PedidoDAO {
    private final Conexion conexion;
    public PedidoDAO(Conexion conexion) { this.conexion = conexion; }

    public int registrar(Pedido pedido) throws SQLException {
        return ReintentoSQLite.ejecutar(() -> registrarUnaVez(pedido));
    }

    private int registrarUnaVez(Pedido pedido) throws SQLException {
        // Cabecera y detalles se guardan juntos. Si algo falla, se deshacen ambos.
        int confirmado = 0;
        try (Connection cn = conexion.abrir()) {
            // Acota la espera de escritura de cada intento.
            try (Statement st = cn.createStatement()) {
                st.execute("PRAGMA busy_timeout = 150");
            }
            cn.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO pedido(cliente_id,fecha,estado) VALUES(?,?,?)")) {
                    ps.setInt(1,pedido.cliente().id()); ps.setString(2,pedido.fecha().toString());
                    ps.setString(3,pedido.estado().name()); ps.executeUpdate();
                }
                try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    if (!rs.next()) throw new SQLException("No se obtuvo el número del pedido.");
                    id = rs.getInt(1);
                }
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO detalle_pedido(pedido_id,producto_id,cantidad,precio_unitario_centavos) VALUES(?,?,?,?)")) {
                    for (DetallePedido detalle : pedido.detalles()) {
                        ps.setInt(1,id); ps.setInt(2,detalle.productoId()); ps.setInt(3,detalle.cantidad());
                        ps.setLong(4,detalle.precioUnitario().movePointRight(2).longValueExact());
                        ps.executeUpdate();
                    }
                }
                cn.commit();
                confirmado = id;
                return id;
            } catch (SQLException | RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException rollback) {
                    // No repetir si no se pudo confirmar la reversión.
                    SQLException incierto = new SQLException("No se pudo revertir el pedido. Revise el historial antes de volver a guardar.", e);
                    incierto.addSuppressed(rollback);
                    throw incierto;
                }
                throw e;
            }
        } catch (SQLException e) {
            // Un error al cerrar la conexión no debe duplicar un pedido ya confirmado.
            if (confirmado != 0) return confirmado;
            throw e;
        }
    }

    public List<Pedido> listar() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        // Los pedidos más recientes aparecen primero en el historial.
        String sql = "SELECT p.*,c.nombre,c.telefono FROM pedido p JOIN cliente c ON c.id=p.cliente_id ORDER BY p.id DESC";
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Cliente cliente = new Cliente(rs.getInt("cliente_id"),rs.getString("nombre"),rs.getString("telefono"));
                pedidos.add(new Pedido(id,cliente,LocalDateTime.parse(rs.getString("fecha")),
                        EstadoPedido.valueOf(rs.getString("estado")),detalles(cn,id)));
            }
        }
        return pedidos;
    }

    private List<DetallePedido> detalles(Connection cn, int id) throws SQLException {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT d.*,p.nombre FROM detalle_pedido d JOIN producto p ON p.id=d.producto_id WHERE d.pedido_id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) detalles.add(new DetallePedido(rs.getInt("producto_id"),rs.getString("nombre"),
                        rs.getInt("cantidad"),BigDecimal.valueOf(rs.getLong("precio_unitario_centavos"),2)));
            }
        }
        return detalles;
    }

    public void cambiarEstado(int id, EstadoPedido actual, EstadoPedido nuevo) throws SQLException {
        // Comparar el estado anterior evita sobrescribir un cambio reciente.
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement("UPDATE pedido SET estado=? WHERE id=? AND estado=?")) {
            ps.setString(1,nuevo.name()); ps.setInt(2,id); ps.setString(3,actual.name());
            if (ps.executeUpdate() != 1) throw new SQLException("El pedido cambió. Actualice la lista.");
        }
    }
}
