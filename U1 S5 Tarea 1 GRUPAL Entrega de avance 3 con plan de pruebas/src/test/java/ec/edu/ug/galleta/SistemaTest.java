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
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
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
    @Test @DisplayName("CP-01 Precio incorrecto") void precioIncorrecto() {
        assertThrows(IllegalArgumentException.class,()->productos.guardar(0,"Galleta","-1"));
        assertThrows(IllegalArgumentException.class,()->productos.guardar(0,"Galleta","2.555"));
    }
    @Test @DisplayName("CP-02 Edición de cliente y producto") void editarClienteYProducto() throws Exception {
        clientes.guardar(clienteId,"Cliente actualizado","0980000000");
        productos.guardar(galleta.id(),"Galleta de chocolate","2.75");
        assertEquals("Cliente actualizado",clientes.listar().get(0).nombre());
        assertEquals(new BigDecimal("2.75"),pdao.buscar(galleta.id()).precio());
    }
    @Test @DisplayName("CP-08 Pedido vacío") void pedidoVacio() { assertThrows(IllegalArgumentException.class,()->pedidos.registrar(clienteId,List.of())); }
    @Test @DisplayName("CP-09 Precio histórico y total") void precioHistoricoYTotal() throws Exception {
        int id=pedidos.registrar(clienteId,List.of(item(galleta,2),item(pastel,1)));
        productos.guardar(galleta.id(),"Galleta","5.00");
        Pedido guardado=pedidos.listar().get(0);
        assertEquals(id,guardado.id()); assertEquals(new BigDecimal("8.00"),guardado.calcularTotal());
        assertEquals(2,guardado.detalles().size());
    }
    @Test @DisplayName("CP-03 Producto inactivo") void productoInactivo() throws Exception {
        productos.desactivar(galleta.id());
        assertEquals(1,productos.listar(true).size());
        assertThrows(IllegalArgumentException.class,()->pedidos.registrar(clienteId,List.of(item(galleta,1))));
    }
    @Test @DisplayName("CP-10 Estados permitidos") void estadosPermitidos() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        Pedido p=pedidos.listar().get(0); assertThrows(IllegalArgumentException.class,()->pedidos.cambiarEstado(p,EstadoPedido.ENTREGADO));
        pedidos.cambiarEstado(p,EstadoPedido.EN_PREPARACION); Pedido preparado=pedidos.listar().get(0);
        pedidos.cambiarEstado(preparado,EstadoPedido.ENTREGADO); Pedido finalizado=pedidos.listar().get(0);
        assertEquals(EstadoPedido.ENTREGADO,finalizado.estado());
        assertThrows(IllegalArgumentException.class,()->pedidos.cambiarEstado(finalizado,EstadoPedido.PENDIENTE));
    }
    @Test @DisplayName("CP-17 Persistencia de pedido") void persistencia() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        Conexion otra=new Conexion(temporal.resolve("prueba.db")); otra.inicializar();
        assertEquals(1,new PedidoDAO(otra).listar().size());
    }
    @Test @DisplayName("CP-18 Transacción completa") void transaccionCompleta() throws Exception {
        Pedido invalido=new Pedido(0,cdao.buscar(clienteId),java.time.LocalDateTime.now(),EstadoPedido.PENDIENTE,
                List.of(item(galleta,1),new DetallePedido(-1,"No existe",1,new BigDecimal("1.00"))));
        assertThrows(java.sql.SQLException.class,()->new PedidoDAO(conexion).registrar(invalido));
        assertTrue(pedidos.listar().isEmpty());
    }
    @Test @DisplayName("CP-19 Construcción de paneles") void construirPaneles() throws Exception {
        // Comprueba que las tres pantallas se pueden construir y cargar.
        SwingUtilities.invokeAndWait(()->{
            try { new ProductosPanel(productos).cargar(); new ClientesPanel(clientes).cargar(); new PedidosPanel(pedidos,productos,clientes).cargar(); }
            catch(Exception e){ throw new RuntimeException(e); }
        });
    }

    // Busca los elementos reales de los formularios para probar su comportamiento.
    private <T extends Component> List<T> componentes(Container raiz, Class<T> tipo) {
        List<T> resultado = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c)) resultado.add(tipo.cast(c));
            if (c instanceof Container cont) resultado.addAll(componentes(cont,tipo));
        }
        return resultado;
    }
    private JButton boton(Container raiz, String texto) {
        return componentes(raiz,JButton.class).stream()
                .filter(b -> texto.equals(b.getText())).findFirst().orElseThrow();
    }
    @Test @DisplayName("CP-04 Búsqueda y edición de producto") void buscarYEditarProductoEnPantalla() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ProductosPanel panel = new ProductosPanel(productos); panel.cargar();
                List<JTextField> campos = componentes(panel,JTextField.class);
                JTable tabla = componentes(panel,JTable.class).get(0);
                campos.get(2).setText("PASTEL"); boton(panel,"Buscar").doClick();
                assertEquals(1,tabla.getRowCount());
                assertEquals("Pastel",tabla.getValueAt(0,1));
                tabla.setRowSelectionInterval(0,0);
                campos.get(1).setText("3.50"); boton(panel,"Guardar").doClick();
                assertEquals(new BigDecimal("3.50"),pdao.buscar(pastel.id()).precio());
                productos.desactivar(pastel.id()); panel.cargar();
                JComboBox<?> filtro = componentes(panel,JComboBox.class).get(0);
                filtro.setSelectedIndex(1); assertEquals(0,tabla.getRowCount());
                filtro.setSelectedIndex(2); assertEquals(1,tabla.getRowCount());
                assertEquals("No",tabla.getValueAt(0,3));
                boton(panel,"Limpiar búsqueda").doClick(); assertEquals(2,tabla.getRowCount());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test @DisplayName("CP-05 Búsqueda y edición de cliente") void buscarYEditarClienteEnPantalla() throws Exception {
        clientes.guardar(0,"Ana","0981111111");
        SwingUtilities.invokeAndWait(() -> {
            try {
                ClientesPanel panel = new ClientesPanel(clientes); panel.cargar();
                List<JTextField> campos = componentes(panel,JTextField.class);
                JTable tabla = componentes(panel,JTable.class).get(0);
                campos.get(2).setText("098111"); boton(panel,"Buscar").doClick();
                assertEquals(1,tabla.getRowCount()); assertEquals("Ana",tabla.getValueAt(0,1));
                tabla.setRowSelectionInterval(0,0);
                campos.get(1).setText("0982222222"); boton(panel,"Guardar").doClick();
                assertEquals("0982222222",clientes.listar().stream().filter(c->c.nombre().equals("Ana")).findFirst().orElseThrow().telefono());
                campos.get(2).setText("ANA"); campos.get(2).postActionEvent();
                assertEquals(1,tabla.getRowCount());
                boton(panel,"Limpiar búsqueda").doClick(); assertEquals(2,tabla.getRowCount());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test @DisplayName("CP-11 Filtro de pedidos") void filtrarPedidosPorNumeroYEstado() throws Exception {
        int id = pedidos.registrar(clienteId,List.of(item(galleta,2)));
        pedidos.registrar(clienteId,List.of(item(pastel,1)));
        Pedido primero = pedidos.listar().stream().filter(p->p.id()==id).findFirst().orElseThrow();
        pedidos.cambiarEstado(primero,EstadoPedido.EN_PREPARACION);
        pedidos.cambiarEstado(pedidos.listar().stream().filter(p->p.id()==id).findFirst().orElseThrow(),EstadoPedido.ENTREGADO);
        SwingUtilities.invokeAndWait(() -> {
            try {
                PedidosPanel panel = new PedidosPanel(pedidos,productos,clientes); panel.cargar();
                JTable historial = componentes(panel,JTable.class).get(1);
                JTextField buscar = componentes(panel,JTextField.class).stream().filter(f->!(f instanceof JFormattedTextField)).findFirst().orElseThrow();
                JComboBox<?> estado = componentes(panel,JComboBox.class).get(2);
                buscar.setText(Integer.toString(id)); boton(panel,"Buscar").doClick();
                assertEquals(1,historial.getRowCount()); assertEquals(id,historial.getValueAt(0,0));
                estado.setSelectedItem(EstadoPedido.PENDIENTE); assertEquals(0,historial.getRowCount());
                estado.setSelectedItem(EstadoPedido.ENTREGADO); assertEquals(1,historial.getRowCount());
                boton(panel,"Limpiar").doClick(); assertEquals(2,historial.getRowCount());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test @DisplayName("CP-20 Reapertura con datos actualizados") void reabrirConDatosActualizados() throws Exception {
        int id = pedidos.registrar(clienteId,List.of(item(galleta,2)));
        clientes.guardar(clienteId,"Cliente actualizado","0980000000");
        productos.desactivar(galleta.id());
        Conexion reabierta = new Conexion(temporal.resolve("prueba.db")); reabierta.inicializar();
        Pedido p = new PedidoDAO(reabierta).listar().get(0);
        assertEquals(id,p.id()); assertEquals("Cliente actualizado",p.cliente().nombre());
        assertEquals(new BigDecimal("5.00"),p.calcularTotal());
        assertFalse(new ProductoDAO(reabierta).buscar(galleta.id()).activo());
    }

    // Tercer avance: entradas inválidas, valores límite y cambios en el historial.
    @Test @DisplayName("CP-06 Campos obligatorios") void camposObligatorios() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> productos.guardar(0," ","2.00"));
        assertThrows(IllegalArgumentException.class, () -> clientes.guardar(0," ","0990000000"));
        assertThrows(IllegalArgumentException.class, () -> clientes.guardar(0,"Ana"," "));
        assertEquals(2, productos.listar(false).size());
        assertEquals(1, clientes.listar().size());
    }
    @Test @DisplayName("CP-07 Límites de precio y coma decimal") void limitesPrecioYComaDecimal() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> productos.guardar(0,"Cero","0"));
        assertThrows(IllegalArgumentException.class, () -> productos.guardar(0,"Exceso","1000000"));
        productos.guardar(galleta.id(),"Galleta","0,01");
        productos.guardar(pastel.id(),"Pastel","999999.99");
        assertEquals(new BigDecimal("0.01"), pdao.buscar(galleta.id()).precio());
        assertEquals(new BigDecimal("999999.99"), pdao.buscar(pastel.id()).precio());
    }
    @Test @DisplayName("CP-12 Cliente inexistente") void clienteInexistente() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> pedidos.registrar(-1,List.of(item(galleta,1))));
        assertTrue(pedidos.listar().isEmpty());
    }
    @Test @DisplayName("CP-13 Precio desactualizado") void precioDeCestaDesactualizado() throws Exception {
        DetallePedido anterior = item(galleta,2);
        productos.guardar(galleta.id(),"Galleta","3.00");
        assertThrows(IllegalArgumentException.class, () -> pedidos.registrar(clienteId,List.of(anterior)));
        assertTrue(pedidos.listar().isEmpty());
        pedidos.registrar(clienteId,List.of(item(pdao.buscar(galleta.id()),2)));
        assertEquals(new BigDecimal("6.00"), pedidos.listar().get(0).calcularTotal());
    }
    @Test @DisplayName("CP-14 Cantidad límite") void cantidadLimite() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> item(galleta,0));
        assertThrows(IllegalArgumentException.class, () -> pedidos.registrar(clienteId,List.of(item(galleta,1000))));
        assertThrows(IllegalArgumentException.class, () -> pedidos.registrar(clienteId,List.of(item(galleta,600),item(galleta,400))));
        assertTrue(pedidos.listar().isEmpty());
        pedidos.registrar(clienteId,List.of(item(galleta,999)));
        assertEquals(new BigDecimal("2497.50"), pedidos.listar().get(0).calcularTotal());
    }
    @Test @DisplayName("CP-15 Cancelación de pedido") void cancelarPedido() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        pedidos.cambiarEstado(pedidos.listar().get(0),EstadoPedido.CANCELADO);
        Pedido cancelado = pedidos.listar().get(0);
        assertEquals(EstadoPedido.CANCELADO,cancelado.estado());
        assertEquals(new BigDecimal("2.50"),cancelado.calcularTotal());
        assertThrows(IllegalArgumentException.class, () -> pedidos.cambiarEstado(cancelado,EstadoPedido.EN_PREPARACION));
    }
    @Test @DisplayName("CP-16 Cambio de estado con datos anteriores") void cambioEstadoConDatosAnteriores() throws Exception {
        pedidos.registrar(clienteId,List.of(item(galleta,1)));
        Pedido anterior = pedidos.listar().get(0);
        pedidos.cambiarEstado(anterior,EstadoPedido.EN_PREPARACION);
        assertThrows(java.sql.SQLException.class, () -> pedidos.cambiarEstado(anterior,EstadoPedido.CANCELADO));
        assertEquals(EstadoPedido.EN_PREPARACION,pedidos.listar().get(0).estado());
    }
    @Test @DisplayName("CP-21 Agregar y quitar de la cesta") void agregarYQuitarCestaEnPantalla() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                PedidosPanel panel = new PedidosPanel(pedidos,productos,clientes); panel.cargar();
                JTable cesta = componentes(panel,JTable.class).get(0);
                JSpinner cantidad = componentes(panel,JSpinner.class).get(0);
                cantidad.setValue(2); boton(panel,"Agregar").doClick();
                cantidad.setValue(3); boton(panel,"Agregar").doClick();
                assertEquals(1,cesta.getRowCount()); assertEquals(5,cesta.getValueAt(0,1));
                assertEquals("$12.50",cesta.getValueAt(0,3));
                cesta.setRowSelectionInterval(0,0); boton(panel,"Quitar").doClick();
                assertEquals(0,cesta.getRowCount());
                assertTrue(componentes(panel,JLabel.class).stream().anyMatch(l -> "Total: $0.00".equals(l.getText())));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
