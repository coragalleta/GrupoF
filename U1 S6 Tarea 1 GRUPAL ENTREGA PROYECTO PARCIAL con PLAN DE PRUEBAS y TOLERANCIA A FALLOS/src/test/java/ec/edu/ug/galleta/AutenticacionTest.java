package ec.edu.ug.galleta;

import ec.edu.ug.galleta.datos.CuentaDAO;
import ec.edu.ug.galleta.excepcion.*;
import ec.edu.ug.galleta.servicio.AutenticacionServicio;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacionTest {
    @TempDir Path temporal;
    private CuentaDAO cuentas;
    private AutenticacionServicio acceso;
    private final String usuario = "admin";
    private final char[] correcta = "PruebaLocal123".toCharArray();
    private final char[] incorrecta = "Incorrecta123".toCharArray();

    @BeforeEach void preparar() throws Exception {
        cuentas = new CuentaDAO(temporal.resolve("acceso.properties"));
        acceso = new AutenticacionServicio(cuentas);
        acceso.crearCuenta(usuario, correcta);
    }

    @Test @DisplayName("AUT-01 Acceso válido") void accesoValido() throws Exception {
        assertDoesNotThrow(() -> acceso.autenticar(usuario, correcta));
        assertEquals(0, cuentas.leer().intentosFallidos());
        assertNotEquals(new String(correcta), cuentas.leer().hash());
    }

    @Test @DisplayName("AUT-02 Usuario inexistente") void usuarioInexistente() throws Exception {
        assertThrows(UsuarioNoExisteException.class, () -> acceso.autenticar("otro", correcta));
        assertEquals(0, cuentas.leer().intentosFallidos());
    }

    @Test @DisplayName("AUT-03 Contraseña incorrecta") void contrasenaIncorrecta() throws Exception {
        assertThrows(ContrasenaIncorrectaException.class, () -> acceso.autenticar(usuario, incorrecta));
        assertEquals(1, cuentas.leer().intentosFallidos());
    }

    @Test @DisplayName("AUT-04 Bloqueo al tercer intento y persistencia") void bloqueo() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertThrows(ContrasenaIncorrectaException.class, () -> acceso.autenticar(usuario, incorrecta));
        }
        assertThrows(CuentaBloqueadaException.class, () -> acceso.autenticar(usuario, incorrecta));
        assertEquals(3, cuentas.leer().intentosFallidos());
        AutenticacionServicio reapertura = new AutenticacionServicio(
                new CuentaDAO(temporal.resolve("acceso.properties")));
        assertThrows(CuentaBloqueadaException.class, () -> reapertura.autenticar(usuario, correcta));
    }

    @Test @DisplayName("AUT-05 Recuperación con credenciales correctas") void recuperacion() throws Exception {
        assertThrows(ContrasenaIncorrectaException.class, () -> acceso.autenticar(usuario, incorrecta));
        assertDoesNotThrow(() -> acceso.autenticar(usuario, correcta));
        assertEquals(0, cuentas.leer().intentosFallidos());
        assertThrows(ContrasenaIncorrectaException.class, () -> acceso.autenticar(usuario, incorrecta));
        assertEquals(1, cuentas.leer().intentosFallidos());
    }

    @Test @DisplayName("AUT-06 No sobrescribir cuenta existente") void noSobrescribir() throws Exception {
        assertThrows(IllegalStateException.class, () -> acceso.crearCuenta("otro", incorrecta));
        assertDoesNotThrow(() -> acceso.autenticar(usuario, correcta));
    }
    @Test @DisplayName("AUT-07 Cuenta inicial y conservación de intentos") void cuentaInicial() throws Exception {
        CuentaDAO inicial = new CuentaDAO(temporal.resolve("inicial.properties"));
        AutenticacionServicio servicio = new AutenticacionServicio(inicial);
        servicio.inicializarCuenta();
        assertDoesNotThrow(() -> servicio.autenticar("admin", "Galleta2026!".toCharArray()));
        assertThrows(ContrasenaIncorrectaException.class,
                () -> servicio.autenticar("admin", incorrecta));
        servicio.inicializarCuenta();
        assertEquals(1, inicial.leer().intentosFallidos());
    }
}
