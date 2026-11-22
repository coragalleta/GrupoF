package ec.edu.ug.galleta;

import ec.edu.ug.galleta.datos.*;
import ec.edu.ug.galleta.modelo.*;
import ec.edu.ug.galleta.servicio.*;
import ec.edu.ug.galleta.vista.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class SistemaTest {
    @TempDir Path temporal;
    Conexion conexion;
    ProductoDAO pdao;
    ClienteDAO cdao;
    ProductoServicio productos;
    ClienteServicio clientes;
    PedidoServicio pedidos;
    int clienteId;
    Producto galleta, pastel;

    @BeforeEach void preparar() throws Exception {
        conexion = new Conexion(temporal.resolve("prueba.db")); conexion.inicializar();
        pdao = new ProductoDAO(conexion); cdao = new ClienteDAO(conexion);
        productos = new ProductoServicio(pdao); clientes = new ClienteServicio(cdao);
        pedidos = new PedidoServicio(new PedidoDAO(conexion),cdao,pdao);
        productos.guardar(0,"Galleta","2.50"); productos.guardar(0,"Pastel","3.00");
        clientes.guardar(0,"Cliente de prueba","0990000000"); clienteId=clientes.listar().get(0).id();
        galleta=productos.listar(true).get(0); pastel=productos.listar(true).get(1);
    }
    private DetallePedido item(Producto p,int cantidad){return new DetallePedido(p.id(),p.nombre(),cantidad,p.precio());}
    @Test void precioIncorrecto() {
        assertThrows(IllegalArgumentException.class,()->productos.guardar(0,"Galleta","-1"));
        assertThrows(IllegalArgumentException.class,()->productos.guardar(0,"Galleta","2.555"));
    }
    @Test void editarClienteYProducto() throws Exception {
        clientes.guardar(clienteId,"Cliente actualizado","0980000000");
        productos.guardar(galleta.id(),"Galleta de chocolate","2.75");
        assertEquals("Cliente actualizado",clientes.listar().get(0).nombre());
        assertEquals(new BigDecimal("2.75"),pdao.buscar(galleta.id()).precio());
    }
    @Test void pedidoVacio() { assertThrows(IllegalArgumentException.class,()->pedidos.registrar(clienteId,List.of())); }
    @Test void precioHistoricoYTotal() throws Exception {
        int id=pedidos.registrar(clienteId,List.of(item(galleta,2),item(pastel,1)));
        productos.guardar(galleta.id(),"Galleta","5.00");
        Pedido guardado=pedidos.listar().get(0);
        assertEquals(id,guardado.id()); assertEquals(new BigDecimal("8.00"),guardado.calcularTotal());
        assertEquals(2,guardado.detalles().size());
    }
    @Test void productoInactivo() throws Exception {
        productos.desactivar(galleta.id());
        assertEquals(1,productos.listar(true).size());
        assertThrows(IllegalArgumentException.class,()->pedidos.registrar(clienteId,List.of(item(galleta,1))));
    }
    @Test void estadosPermitidos() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        Pedido p=pedidos.listar().get(0); assertThrows(IllegalArgumentException.class,()->pedidos.cambiarEstado(p,EstadoPedido.ENTREGADO));
        pedidos.cambiarEstado(p,EstadoPedido.EN_PREPARACION); Pedido preparado=pedidos.listar().get(0);
        pedidos.cambiarEstado(preparado,EstadoPedido.ENTREGADO); Pedido finalizado=pedidos.listar().get(0);
        assertEquals(EstadoPedido.ENTREGADO,finalizado.estado());
        assertThrows(IllegalArgumentException.class,()->pedidos.cambiarEstado(finalizado,EstadoPedido.PENDIENTE));
    }
    @Test void persistencia() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        Conexion otra=new Conexion(temporal.resolve("prueba.db")); otra.inicializar();
        assertEquals(1,new PedidoDAO(otra).listar().size());
    }
    @Test void transaccionCompleta() throws Exception {
        Pedido invalido=new Pedido(0,cdao.buscar(clienteId),java.time.LocalDateTime.now(),EstadoPedido.PENDIENTE,
                List.of(item(galleta,1),new DetallePedido(-1,"No existe",1,new BigDecimal("1.00"))));
        assertThrows(java.sql.SQLException.class,()->new PedidoDAO(conexion).registrar(invalido));
        assertTrue(pedidos.listar().isEmpty());
    }
    @Test void construirPaneles() throws Exception {
        // Comprueba que las tres pantallas se pueden construir y cargar.
        SwingUtilities.invokeAndWait(()->{
            try { new ProductosPanel(productos).cargar(); new ClientesPanel(clientes).cargar(); new PedidosPanel(pedidos,productos,clientes).cargar(); }
            catch(Exception e){ throw new RuntimeException(e); }
        });
    }
}
