package ec.edu.ug.galleta.servicio;

import ec.edu.ug.galleta.datos.ClienteDAO;
import ec.edu.ug.galleta.modelo.Cliente;
import java.sql.SQLException;
import java.util.List;

public class ClienteServicio {
    private final ClienteDAO dao;
    public ClienteServicio(ClienteDAO dao) { this.dao = dao; }

    public void guardar(int id, String nombre, String telefono) throws SQLException {
        if (nombre.isBlank() || telefono.isBlank()) throw new IllegalArgumentException("Complete el nombre y el teléfono.");
        dao.guardar(id,nombre.trim(),telefono.trim());
    }
    public List<Cliente> listar() throws SQLException { return dao.listar(); }
}
