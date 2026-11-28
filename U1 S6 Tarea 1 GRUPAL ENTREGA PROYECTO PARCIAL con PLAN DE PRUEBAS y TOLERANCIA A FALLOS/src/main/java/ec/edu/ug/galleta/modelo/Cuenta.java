package ec.edu.ug.galleta.modelo;

/** Datos persistentes de la cuenta de acceso local. */
public record Cuenta(String usuario, String sal, String hash, int intentosFallidos) {
    public Cuenta conIntentos(int intentos) {
        return new Cuenta(usuario, sal, hash, intentos);
    }
}
