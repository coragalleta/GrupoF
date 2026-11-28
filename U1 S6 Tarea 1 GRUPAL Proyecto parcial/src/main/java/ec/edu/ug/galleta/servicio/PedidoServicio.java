package ec.edu.ug.galleta.servicio;

import ec.edu.ug.galleta.datos.*;
import ec.edu.ug.galleta.modelo.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Valida los datos del pedido antes de solicitar su registro. */
public class PedidoServicio {
    private final PedidoDAO pedidos;
    private final ClienteDAO clientes;
    private final ProductoDAO productos;

    public PedidoServicio(PedidoDAO pedidos, ClienteDAO clientes, ProductoDAO productos) {
        this.pedidos = pedidos; this.clientes = clientes; this.productos = productos;
    }

    public int registrar(int clienteId, List<DetallePedido> seleccion) throws SQLException {
        Cliente cliente = clientes.buscar(clienteId);
        if (cliente == null) throw new IllegalArgumentException("Seleccione un cliente válido.");
        if (seleccion.isEmpty()) throw new IllegalArgumentException("Agregue al menos un producto.");
        List<DetallePedido> detalles = new ArrayList<>();
        Map<Integer, Integer> cantidades = new HashMap<>();
        for (DetallePedido item : seleccion) {
            Producto producto = productos.buscar(item.productoId());
            if (producto == null || !producto.activo()) throw new IllegalArgumentException("Un producto ya no está disponible.");
            // El límite de la pantalla también se comprueba antes de guardar.
            if (item.cantidad() > 999 || cantidades.merge(producto.id(), item.cantidad(), Integer::sum) > 999)
                throw new IllegalArgumentException("Máximo 999 unidades por producto.");
            // Se verifica que el precio mostrado coincida con el precio actual.
            if (producto.precio().compareTo(item.precioUnitario()) != 0)
                throw new IllegalArgumentException("El precio cambió. Quite y agregue nuevamente el producto.");
            detalles.add(new DetallePedido(producto.id(),producto.nombre(),item.cantidad(),producto.precio()));
        }
        return pedidos.registrar(new Pedido(0,cliente,LocalDateTime.now(),EstadoPedido.PENDIENTE,detalles));
    }

    public List<Pedido> listar() throws SQLException { return pedidos.listar(); }

    public void cambiarEstado(Pedido pedido, EstadoPedido nuevo) throws SQLException {
        if (!pedido.estado().permite(nuevo)) throw new IllegalArgumentException("No se permite ese cambio de estado.");
        pedidos.cambiarEstado(pedido.id(),pedido.estado(),nuevo);
    }
}
