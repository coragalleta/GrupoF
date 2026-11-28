package ec.edu.ug.galleta.excepcion;

/** Notifica una contraseña inválida antes de alcanzar el límite. */
public class ContrasenaIncorrectaException extends Exception {
    public ContrasenaIncorrectaException() {
        super("Contraseña incorrecta.");
    }
}
