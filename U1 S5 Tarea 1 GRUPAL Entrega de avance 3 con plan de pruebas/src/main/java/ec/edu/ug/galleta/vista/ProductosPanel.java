package ec.edu.ug.galleta.vista;

import ec.edu.ug.galleta.modelo.Producto;
import ec.edu.ug.galleta.servicio.ProductoServicio;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

// Administración del catálogo y búsqueda de productos.
public class ProductosPanel extends JPanel {
    private final ProductoServicio servicio;
    private final JTextField nombre = new JTextField(22), precio = new JTextField(9);
    private final DefaultTableModel modelo = Estilo.modelo("ID","Producto","Precio","Disponible");
    private final JTable tabla = Estilo.tabla(modelo);
    private final JLabel modo = new JLabel("Nuevo producto");
    private final JTextField buscar = new JTextField(20);
    private final JComboBox<String> disponibilidad = new JComboBox<>(new String[]{"Todos","Disponibles","No disponibles"});
    private final JLabel cantidad = new JLabel();
    private int id = 0;
    private List<Producto> productos = List.of();

    public ProductosPanel(ProductoServicio servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(10,12)); setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(18,20,18,20));
        JPanel superior = Estilo.panel(new BorderLayout(3,5));
        superior.add(Estilo.titulo("Catálogo de productos"),BorderLayout.NORTH);
        JPanel formulario = Estilo.fila();
        JButton guardar = Estilo.boton("Guardar",Estilo.MENTA);
        JButton nuevo = Estilo.boton("Nuevo",Estilo.LILA);
        JButton desactivar = Estilo.boton("Desactivar",Estilo.ROSA);
        formulario.add(new JLabel("Nombre:")); formulario.add(nombre);
        formulario.add(new JLabel("Precio ($):")); formulario.add(precio);
        formulario.add(guardar); formulario.add(nuevo); formulario.add(desactivar);
        superior.add(formulario,BorderLayout.CENTER); superior.add(modo,BorderLayout.SOUTH);
        add(superior,BorderLayout.NORTH); add(new JScrollPane(tabla),BorderLayout.CENTER);
        JPanel filtros = Estilo.fila();
        JButton consultar = Estilo.boton("Buscar",Estilo.LILA), borrar = Estilo.boton("Limpiar búsqueda",Estilo.LILA);
        filtros.add(new JLabel("Buscar producto:")); filtros.add(buscar); filtros.add(disponibilidad);
        filtros.add(consultar); filtros.add(borrar); filtros.add(cantidad);
        add(filtros,BorderLayout.SOUTH);
        consultar.addActionListener(e -> consultar());
        buscar.addActionListener(e -> consultar());
        disponibilidad.addActionListener(e -> consultar());
        borrar.addActionListener(e -> { buscar.setText(""); disponibilidad.setSelectedIndex(0); consultar(); });
        guardar.addActionListener(e -> guardar()); nuevo.addActionListener(e -> limpiar());
        desactivar.addActionListener(e -> desactivar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                Producto p = productos.get(tabla.getSelectedRow());
                id = p.id(); nombre.setText(p.nombre()); precio.setText(p.precio().toPlainString());
                modo.setText("Editando producto #" + id);
            }
        });
    }
    public void cargar() throws SQLException {
        // La lista visible y la tabla deben tener el mismo orden para editar el registro correcto.
        String texto = buscar.getText().trim().toLowerCase(Locale.ROOT);
        int opcion = disponibilidad.getSelectedIndex();
        productos = servicio.listar(false).stream()
                .filter(p -> p.nombre().toLowerCase(Locale.ROOT).contains(texto))
                .filter(p -> opcion == 0 || p.activo() == (opcion == 1)).toList();
        limpiar();
        modelo.setRowCount(0);
        for (Producto p : productos) modelo.addRow(new Object[]{p.id(),p.nombre(),Estilo.dinero(p.precio()),p.activo()?"Sí":"No"});
        cantidad.setText(productos.size()+" producto(s)");
    }
    private void consultar() {
        try { cargar(); } catch (SQLException e) { Estilo.error(this,e); }
    }
    private void limpiar() { id=0; nombre.setText(""); precio.setText(""); tabla.clearSelection(); modo.setText("Nuevo producto"); }
    private void guardar() {
        try { servicio.guardar(id,nombre.getText(),precio.getText()); limpiar(); cargar(); }
        catch (SQLException | IllegalArgumentException e) { Estilo.error(this,e); }
    }
    private void desactivar() {
        if (id==0) { Estilo.error(this,new IllegalArgumentException("Seleccione un producto.")); return; }
        if (JOptionPane.showConfirmDialog(this,"¿Desactivar este producto?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try { servicio.desactivar(id); limpiar(); cargar(); }
        catch (SQLException e) { Estilo.error(this,e); }
    }
}
