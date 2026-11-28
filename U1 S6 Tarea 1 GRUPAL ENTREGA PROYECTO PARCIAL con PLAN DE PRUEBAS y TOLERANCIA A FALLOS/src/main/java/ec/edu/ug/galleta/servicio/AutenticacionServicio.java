package ec.edu.ug.galleta.servicio;

import ec.edu.ug.galleta.datos.CuentaDAO;
import ec.edu.ug.galleta.excepcion.*;
import ec.edu.ug.galleta.modelo.Cuenta;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Valida la cuenta local y conserva el contador de intentos fallidos. */
public class AutenticacionServicio {
    private static final int MAX_INTENTOS = 3;
    private final CuentaDAO cuentas;

    public AutenticacionServicio(CuentaDAO cuentas) {
        this.cuentas = cuentas;
    }

    public boolean existeCuenta() throws IOException {
        return cuentas.leer() != null;
    }

    public synchronized void inicializarCuenta() throws IOException {
        if (!existeCuenta()) {
            cuentas.guardar(new Cuenta("admin", "dLbl6vOJcy0KAAgSU4bIhw==",
                    "ufru19a4ueaexpNJLRm1PTM27YsbaCpId7/L/s2NCas=", 0));
        }
    }

    public synchronized void crearCuenta(String usuario, char[] contrasena) throws IOException {
        if (existeCuenta()) throw new IllegalStateException("La cuenta ya existe.");
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.length < 8) {
            throw new IllegalArgumentException("Ingrese un usuario y una contraseña de al menos 8 caracteres.");
        }
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        cuentas.guardar(new Cuenta(usuario.trim(), Base64.getEncoder().encodeToString(sal),
                Base64.getEncoder().encodeToString(derivar(contrasena, sal)), 0));
    }

    public synchronized void autenticar(String usuario, char[] contrasena)
            throws IOException, UsuarioNoExisteException,
                   ContrasenaIncorrectaException, CuentaBloqueadaException {
        Cuenta cuenta = cuentas.leer();
        if (cuenta == null || !cuenta.usuario().equals(usuario)) {
            throw new UsuarioNoExisteException();
        }
        if (cuenta.intentosFallidos() >= MAX_INTENTOS) {
            throw new CuentaBloqueadaException();
        }
        // Persistir el intento antes de notificar el fallo de autenticación.
        if (!coincide(cuenta, contrasena)) {
            int intentos = cuenta.intentosFallidos() + 1;
            cuentas.guardar(cuenta.conIntentos(intentos));
            if (intentos >= MAX_INTENTOS) {
                throw new CuentaBloqueadaException();
            }
            throw new ContrasenaIncorrectaException();
        }
        if (cuenta.intentosFallidos() != 0) {
            cuentas.guardar(cuenta.conIntentos(0));
        }
    }

    private boolean coincide(Cuenta cuenta, char[] contrasena) throws IOException {
        if (contrasena == null) return false;
        byte[] sal = Base64.getDecoder().decode(cuenta.sal());
        byte[] esperado = Base64.getDecoder().decode(cuenta.hash());
        return MessageDigest.isEqual(esperado, derivar(contrasena, sal));
    }

    private byte[] derivar(char[] contrasena, byte[] sal) throws IOException {
        // Derivación de contraseña con sal y 600 000 iteraciones.
        PBEKeySpec spec = new PBEKeySpec(contrasena, sal, 600_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo verificar la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
