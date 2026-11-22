package ec.edu.ug.galleta.vista;

import ec.edu.ug.galleta.modelo.*;
import ec.edu.ug.galleta.servicio.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PedidosPanel extends JPanel {
    private final PedidoServicio pedidos;
    private final ProductoServicio productos;
    private final ClienteServicio clientes;
    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JComboBox<Producto> producto = new JComboBox<>();
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1,1,999,1));
    private final JLabel total = new JLabel("Total: $0.00");
    private final List<DetallePedido> cesta = new ArrayList<>();
    private final DefaultTableModel modeloCesta = Estilo.modelo("Producto","Cantidad","Precio unitario","Subtotal");
    private final JTable tablaCesta = Estilo.tabla(modeloCesta);
    private final DefaultTableModel modeloHistorial = Estilo.modelo("N.º","Cliente","Fecha","Estado","Total");
    private final JTable tablaHistorial = Estilo.tabla(modeloHistorial);
    private final JTextField buscar = new JTextField(19);
    private final JComboBox<Object> filtro = new JComboBox<>();
    private List<Pedido> historial = List.of(), visibles = List.of();

    public PedidosPanel(PedidoServicio pedidos, ProductoServicio productos, ClienteServicio clientes) {
        this.pedidos=pedidos; this.productos=productos; this.clientes=clientes;
        setLayout(new BorderLayout(8,8)); setBackground(Estilo.FONDO); setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        JPanel arriba=Estilo.panel(new BorderLayout(4,5));
        arriba.add(Estilo.titulo("Registrar pedido"),BorderLayout.NORTH);
        JPanel f=Estilo.fila(); cliente.setPreferredSize(new Dimension(220,29)); producto.setPreferredSize(new Dimension(210,29));
        JButton agregar=Estilo.boton("Agregar",Estilo.MENTA), quitar=Estilo.boton("Quitar",Estilo.ROSA), guardar=Estilo.boton("Registrar pedido",Estilo.MENTA);
        f.add(new JLabel("Cliente:")); f.add(cliente); f.add(new JLabel("Producto:")); f.add(producto); f.add(new JLabel("Cant.:")); f.add(cantidad); f.add(agregar);
        JPanel bloque=Estilo.panel(new BorderLayout(5,5)); bloque.add(f,BorderLayout.NORTH); bloque.add(new JScrollPane(tablaCesta),BorderLayout.CENTER);
        JPanel acciones=Estilo.fila(); total.setFont(total.getFont().deriveFont(Font.BOLD,18f)); total.setForeground(Estilo.VINO);
        acciones.add(quitar); acciones.add(guardar); acciones.add(total); bloque.add(acciones,BorderLayout.SOUTH);
        arriba.add(bloque,BorderLayout.CENTER);
        JPanel abajo=Estilo.panel(new BorderLayout(4,5)); abajo.add(Estilo.titulo("Pedidos registrados"),BorderLayout.NORTH);
        JPanel filtros=Estilo.fila(); filtro.addItem("Todos"); for (EstadoPedido estado:EstadoPedido.values()) filtro.addItem(estado);
        JButton filtrar=Estilo.boton("Buscar",Estilo.LILA), detalle=Estilo.boton("Ver detalle",Estilo.LILA), estado=Estilo.boton("Cambiar estado",Estilo.ROSA);
        filtros.add(new JLabel("Cliente:")); filtros.add(buscar); filtros.add(new JLabel("Estado:")); filtros.add(filtro); filtros.add(filtrar); filtros.add(detalle); filtros.add(estado);
        JPanel listado=Estilo.panel(new BorderLayout(5,5)); listado.add(filtros,BorderLayout.NORTH); listado.add(new JScrollPane(tablaHistorial),BorderLayout.CENTER); abajo.add(listado,BorderLayout.CENTER);
        JSplitPane division=new JSplitPane(JSplitPane.VERTICAL_SPLIT,arriba,abajo); division.setResizeWeight(.48); division.setBorder(null); add(division,BorderLayout.CENTER);
        agregar.addActionListener(e->agregar()); quitar.addActionListener(e->quitar()); guardar.addActionListener(e->registrar());
        filtrar.addActionListener(e->filtrar()); filtro.addActionListener(e->filtrar()); buscar.addActionListener(e->filtrar());
        detalle.addActionListener(e->verDetalle()); estado.addActionListener(e->cambiarEstado());
    }

    public void cargar() throws SQLException {
        Cliente anterior=(Cliente)cliente.getSelectedItem(); cliente.removeAllItems();
        for(Cliente c:clientes.listar()) { cliente.addItem(c); if(anterior!=null && anterior.id()==c.id()) cliente.setSelectedItem(c); }
        producto.removeAllItems(); for(Producto p:productos.listar(true)) producto.addItem(p);
        historial=pedidos.listar(); filtrar();
    }
    private void agregar() {
        Producto p=(Producto)producto.getSelectedItem();
        if(p==null) { Estilo.error(this,new IllegalArgumentException("Registre un producto disponible primero.")); return; }
        int unidades=(Integer)cantidad.getValue();
        for(int i=0;i<cesta.size();i++) {
            DetallePedido d=cesta.get(i);
            if(d.productoId()==p.id()) {
                if (d.precioUnitario().compareTo(p.precio())!=0) { Estilo.error(this,new IllegalArgumentException("Quite el producto anterior antes de agregarlo con el nuevo precio.")); return; }
                if (d.cantidad()+unidades>999) { Estilo.error(this,new IllegalArgumentException("Máximo 999 unidades por producto.")); return; }
                cesta.set(i,new DetallePedido(p.id(),p.nombre(),d.cantidad()+unidades,p.precio())); actualizarCesta(); return;
            }
        }
        cesta.add(new DetallePedido(p.id(),p.nombre(),unidades,p.precio())); actualizarCesta();
    }
    private void quitar() { int fila=tablaCesta.getSelectedRow(); if(fila>=0){ cesta.remove(fila); actualizarCesta(); } }
    // El total se actualiza cada vez que se añade o quita un producto.
    private void actualizarCesta() {
        modeloCesta.setRowCount(0); BigDecimal suma=new BigDecimal("0.00");
        for(DetallePedido d:cesta){ modeloCesta.addRow(new Object[]{d.nombreProducto(),d.cantidad(),Estilo.dinero(d.precioUnitario()),Estilo.dinero(d.calcularSubtotal())}); suma=suma.add(d.calcularSubtotal()); }
        total.setText("Total: "+Estilo.dinero(suma));
    }
    private void registrar() {
        try {
            Cliente c=(Cliente)cliente.getSelectedItem(); if(c==null) throw new IllegalArgumentException("Seleccione un cliente.");
            int id=pedidos.registrar(c.id(),cesta); cesta.clear(); actualizarCesta(); cargar();
            JOptionPane.showMessageDialog(this,"Pedido #"+id+" registrado.");
        } catch(SQLException | IllegalArgumentException e) { Estilo.error(this,e); }
    }
    private void filtrar() {
        String texto=buscar.getText().trim().toLowerCase(Locale.ROOT); Object seleccion=filtro.getSelectedItem();
        visibles=historial.stream().filter(p->p.cliente().nombre().toLowerCase(Locale.ROOT).contains(texto))
                .filter(p->!(seleccion instanceof EstadoPedido) || p.estado()==seleccion).toList();
        modeloHistorial.setRowCount(0);
        for(Pedido p:visibles) modeloHistorial.addRow(new Object[]{p.id(),p.cliente().nombre(),p.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),p.estado(),Estilo.dinero(p.calcularTotal())});
    }
    private Pedido seleccionado() { int fila=tablaHistorial.getSelectedRow(); if(fila<0){ Estilo.error(this,new IllegalArgumentException("Seleccione un pedido de la lista.")); return null;} return visibles.get(fila); }
    private void verDetalle() {
        Pedido p=seleccionado(); if(p==null)return;
        DefaultTableModel m=Estilo.modelo("Producto","Cant.","Precio","Subtotal");
        for(DetallePedido d:p.detalles()) m.addRow(new Object[]{d.nombreProducto(),d.cantidad(),Estilo.dinero(d.precioUnitario()),Estilo.dinero(d.calcularSubtotal())});
        JScrollPane scroll=new JScrollPane(Estilo.tabla(m)); scroll.setPreferredSize(new Dimension(580,160));
        JPanel datos=Estilo.panel(new BorderLayout(5,5)); datos.add(new JLabel(p.cliente().nombre()+" - "+p.estado()),BorderLayout.NORTH); datos.add(scroll,BorderLayout.CENTER);
        datos.add(new JLabel("Total: "+Estilo.dinero(p.calcularTotal())),BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this,datos,"Pedido #"+p.id(),JOptionPane.INFORMATION_MESSAGE);
    }
    private void cambiarEstado() {
        Pedido p=seleccionado(); if(p==null)return;
        List<EstadoPedido> opciones=new ArrayList<>(); for(EstadoPedido s:EstadoPedido.values()) if(p.estado().permite(s)) opciones.add(s);
        if(opciones.isEmpty()){ Estilo.error(this,new IllegalArgumentException("Este pedido ya tiene un estado final.")); return; }
        EstadoPedido nuevo=(EstadoPedido)JOptionPane.showInputDialog(this,"Seleccione el nuevo estado:","Pedido #"+p.id(),JOptionPane.QUESTION_MESSAGE,null,opciones.toArray(),opciones.get(0));
        if(nuevo!=null) try { pedidos.cambiarEstado(p,nuevo); cargar(); } catch(SQLException | IllegalArgumentException e){ Estilo.error(this,e); }
    }
}
