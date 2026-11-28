package ec.edu.ug.galleta.excepcion;

/** Identifica un usuario que no corresponde a la cuenta local. */
public class UsuarioNoExisteException extends Exception {
    public UsuarioNoExisteException() {
        super("El usuario no existe.");
    }
}
