package ec.edu.ug.galleta.modelo;

// Datos básicos del cliente que realiza el pedido.
public record Cliente(int id, String nombre, String telefono) {
    @Override
    public String toString() {
        return nombre + " (" + telefono + ")";
    }
}
