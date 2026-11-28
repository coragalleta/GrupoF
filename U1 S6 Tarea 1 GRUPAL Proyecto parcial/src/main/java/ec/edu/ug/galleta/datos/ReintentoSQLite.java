package ec.edu.ug.galleta.datos;

import java.sql.SQLException;

/** Repite una operación únicamente cuando SQLite informa un bloqueo transitorio. */
public final class ReintentoSQLite {
    private static final int MAX_INTENTOS = 3;

    private ReintentoSQLite() {}

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar() throws SQLException;
    }

    public static <T> T ejecutar(Operacion<T> operacion) throws SQLException {
        for (int intento = 1; ; intento++) {
            try {
                return operacion.ejecutar();
            } catch (SQLException e) {
                // Los códigos extendidos conservan el código primario en el byte inferior.
                int codigo = e.getErrorCode() & 0xFF;
                if (codigo != 5 && codigo != 6) throw e;
                if (intento == MAX_INTENTOS) {
                    throw new SQLException("La base de datos está ocupada. Intente guardar nuevamente.",
                            e.getSQLState(), e.getErrorCode(), e);
                }
                try {
                    Thread.sleep(120L * intento);
                } catch (InterruptedException interrumpida) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Se interrumpió la espera para guardar el pedido.", interrumpida);
                }
            }
        }
    }
}
