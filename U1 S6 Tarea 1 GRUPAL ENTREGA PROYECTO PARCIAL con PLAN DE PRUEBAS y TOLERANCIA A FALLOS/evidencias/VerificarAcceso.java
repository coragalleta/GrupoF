import ec.edu.ug.galleta.datos.CuentaDAO;
import ec.edu.ug.galleta.servicio.AutenticacionServicio;
import ec.edu.ug.galleta.excepcion.*;
import java.nio.file.*;

public class VerificarAcceso {
    interface Accion { void ejecutar() throws Exception; }
    static void comprobar(boolean condicion) { if (!condicion) throw new AssertionError(); }
    static void esperar(Class<? extends Exception> tipo, Accion accion) throws Exception {
        try { accion.ejecutar(); } catch (Exception e) { if (tipo.isInstance(e)) return; throw e; }
        throw new AssertionError("No se lanzó " + tipo.getSimpleName());
    }
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("galleta-acceso-");
        Path archivo = carpeta.resolve("acceso.properties");
        try {
            var dao = new CuentaDAO(archivo);
            var acceso = new AutenticacionServicio(dao);
            char[] buena = "PruebaLocal123".toCharArray(), mala = "Incorrecta123".toCharArray();
            comprobar(!acceso.existeCuenta());
            acceso.crearCuenta("admin", buena);
            acceso.autenticar("admin", buena);
            comprobar(dao.leer().intentosFallidos() == 0 && !Files.readString(archivo).contains(new String(buena)));
            System.out.println("AUT-01: Acceso válido y contraseña no almacenada en texto plano: OK");
            esperar(UsuarioNoExisteException.class, () -> acceso.autenticar("otro", buena));
            comprobar(dao.leer().intentosFallidos() == 0);
            System.out.println("AUT-02: Usuario inexistente: OK");
            esperar(ContrasenaIncorrectaException.class, () -> acceso.autenticar("admin", mala));
            comprobar(dao.leer().intentosFallidos() == 1);
            System.out.println("AUT-03: Contraseña incorrecta: OK");
            acceso.autenticar("admin", buena);
            comprobar(dao.leer().intentosFallidos() == 0);
            esperar(ContrasenaIncorrectaException.class, () -> acceso.autenticar("admin", mala));
            comprobar(dao.leer().intentosFallidos() == 1);
            acceso.autenticar("admin", buena);
            System.out.println("AUT-05: Recuperación y reinicio del contador: OK");
            esperar(IllegalStateException.class, () -> acceso.crearCuenta("otro", mala));
            acceso.autenticar("admin", buena);
            System.out.println("AUT-06: No sobrescribir cuenta existente: OK");
            for (int i = 0; i < 2; i++) esperar(ContrasenaIncorrectaException.class, () -> acceso.autenticar("admin", mala));
            esperar(CuentaBloqueadaException.class, () -> acceso.autenticar("admin", mala));
            comprobar(dao.leer().intentosFallidos() == 3);
            var reapertura = new AutenticacionServicio(new CuentaDAO(archivo));
            esperar(CuentaBloqueadaException.class, () -> reapertura.autenticar("admin", buena));
            System.out.println("AUT-04: Bloqueo al tercer intento, persistente al reabrir: OK");
            Path inicial = carpeta.resolve("inicial.properties");
            try {
                var daoInicial = new CuentaDAO(inicial);
                var servicio = new AutenticacionServicio(daoInicial);
                servicio.inicializarCuenta();
                servicio.autenticar("admin", "Galleta2026!".toCharArray());
                esperar(ContrasenaIncorrectaException.class, () -> servicio.autenticar("admin", mala));
                servicio.inicializarCuenta();
                comprobar(daoInicial.leer().intentosFallidos() == 1);
                System.out.println("AUT-07: Cuenta inicial y conservación de intentos: OK");
            } finally { Files.deleteIfExists(inicial); }
            System.out.println("7 comprobaciones satisfactorias.");
        } finally { Files.deleteIfExists(archivo); Files.deleteIfExists(carpeta); }
    }
}
