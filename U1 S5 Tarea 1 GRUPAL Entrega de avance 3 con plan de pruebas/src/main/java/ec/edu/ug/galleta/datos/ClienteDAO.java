package ec.edu.ug.galleta.datos;

import ec.edu.ug.galleta.modelo.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    private final Conexion conexion;
    public ClienteDAO(Conexion conexion) { this.conexion = conexion; }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement("SELECT * FROM cliente ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) clientes.add(leer(rs));
        }
        return clientes;
    }

    public Cliente buscar(int id) throws SQLException {
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement("SELECT * FROM cliente WHERE id=?")) {
            ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? leer(rs) : null; }
        }
    }

    private Cliente leer(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"),rs.getString("nombre"),rs.getString("telefono"));
    }

    // El mismo método permite registrar o editar según el identificador.
    public void guardar(int id, String nombre, String telefono) throws SQLException {
        String sql = id == 0 ? "INSERT INTO cliente(nombre,telefono) VALUES(?,?)"
                : "UPDATE cliente SET nombre=?,telefono=? WHERE id=?";
        try (Connection cn = conexion.abrir(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1,nombre); ps.setString(2,telefono);
            if (id != 0) ps.setInt(3,id);
            if (ps.executeUpdate() != 1) throw new SQLException("Cliente no encontrado.");
        }
    }
}
