package ec.edu.ug.galleta.datos;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {
    private final String url;

    public Conexion(Path archivo) {
        url = "jdbc:sqlite:" + archivo.toAbsolutePath();
    }

    public Connection abrir() throws SQLException {
        Connection cn = DriverManager.getConnection(url);
        try (Statement st = cn.createStatement()) {
            // SQLite requiere activar las claves foráneas en cada conexión.
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            cn.close();
            throw e;
        }
        return cn;
    }

    // La base de datos y las tablas se crean en la primera ejecución.
    public void inicializar() throws SQLException {
        try (Connection cn = abrir(); Statement st = cn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS producto (
                    id INTEGER PRIMARY KEY,
                    nombre TEXT NOT NULL CHECK(length(trim(nombre)) > 0),
                    precio_centavos INTEGER NOT NULL CHECK(precio_centavos > 0),
                    activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1))
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS cliente (
                    id INTEGER PRIMARY KEY,
                    nombre TEXT NOT NULL CHECK(length(trim(nombre)) > 0),
                    telefono TEXT NOT NULL CHECK(length(trim(telefono)) > 0)
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS pedido (
                    id INTEGER PRIMARY KEY,
                    cliente_id INTEGER NOT NULL REFERENCES cliente(id),
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL CHECK(estado IN ('PENDIENTE','EN_PREPARACION','ENTREGADO','CANCELADO'))
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS detalle_pedido (
                    id INTEGER PRIMARY KEY,
                    pedido_id INTEGER NOT NULL REFERENCES pedido(id),
                    producto_id INTEGER NOT NULL REFERENCES producto(id),
                    cantidad INTEGER NOT NULL CHECK(cantidad > 0),
                    precio_unitario_centavos INTEGER NOT NULL CHECK(precio_unitario_centavos > 0)
                )
                """);
        }
    }
}
