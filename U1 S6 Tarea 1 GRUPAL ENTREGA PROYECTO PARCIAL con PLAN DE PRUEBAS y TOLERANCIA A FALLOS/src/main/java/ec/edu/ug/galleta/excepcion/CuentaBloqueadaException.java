package ec.edu.ug.galleta.excepcion;

/** Impide el acceso tras tres intentos fallidos. */
public class CuentaBloqueadaException extends Exception {
    public CuentaBloqueadaException() {
        super("Cuenta bloqueada por tres intentos fallidos.");
    }
}
