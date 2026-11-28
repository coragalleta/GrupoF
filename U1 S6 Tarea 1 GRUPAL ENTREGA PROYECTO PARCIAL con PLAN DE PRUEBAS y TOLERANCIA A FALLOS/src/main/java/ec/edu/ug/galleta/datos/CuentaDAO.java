package ec.edu.ug.galleta.datos;

import ec.edu.ug.galleta.modelo.Cuenta;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.Properties;

/** Almacena la cuenta local sin modificar la base de datos de pedidos. */
public class CuentaDAO {
    private final Path archivo;

    public CuentaDAO(Path archivo) {
        this.archivo = archivo.toAbsolutePath();
    }

    public Cuenta leer() throws IOException {
        Properties datos = new Properties();
        try (var entrada = Files.newInputStream(archivo)) {
            datos.load(entrada);
        } catch (NoSuchFileException e) {
            return null;
        } catch (IllegalArgumentException e) {
            throw new IOException("No se pudo leer la cuenta local.", e);
        }
        try {
            String usuario = datos.getProperty("usuario");
            String sal = datos.getProperty("sal");
            String hash = datos.getProperty("hash");
            int intentos = Integer.parseInt(datos.getProperty("intentosFallidos"));
            if (usuario == null || usuario.isBlank() || sal == null || hash == null
                    || Base64.getDecoder().decode(sal).length != 16
                    || Base64.getDecoder().decode(hash).length != 32
                    || intentos < 0 || intentos > 3) {
                throw new IllegalArgumentException("Datos de acceso inválidos.");
            }
            return new Cuenta(usuario, sal, hash, intentos);
        } catch (IllegalArgumentException e) {
            throw new IOException("No se pudo leer la cuenta local.", e);
        }
    }

    public void guardar(Cuenta cuenta) throws IOException {
        Properties datos = new Properties();
        datos.setProperty("usuario", cuenta.usuario());
        datos.setProperty("sal", cuenta.sal());
        datos.setProperty("hash", cuenta.hash());
        datos.setProperty("intentosFallidos", Integer.toString(cuenta.intentosFallidos()));
        // Escribir primero en un archivo temporal para evitar registros parciales.
        Path temporal = Files.createTempFile(archivo.getParent(), "acceso-", ".tmp");
        try {
            try (var salida = Files.newOutputStream(temporal)) {
                datos.store(salida, "Cuenta local - La ultima galleta");
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
