package ec.edu.ug.galleta.modelo;

public enum EstadoPedido {
    PENDIENTE("Pendiente"), EN_PREPARACION("En preparación"),
    ENTREGADO("Entregado"), CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPedido(String etiqueta) { this.etiqueta = etiqueta; }

    // Un pedido entregado o cancelado ya no cambia de estado.
    public boolean permite(EstadoPedido nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == EN_PREPARACION || nuevo == CANCELADO;
            case EN_PREPARACION -> nuevo == ENTREGADO || nuevo == CANCELADO;
            default -> false;
        };
    }

    @Override public String toString() { return etiqueta; }
}
