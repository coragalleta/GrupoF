package ec.edu.ug.galleta.vista;

import ec.edu.ug.galleta.modelo.Cliente;
import ec.edu.ug.galleta.servicio.ClienteServicio;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ClientesPanel extends JPanel {
    private final ClienteServicio servicio;
    private final JTextField nombre = new JTextField(22), telefono = new JTextField(14);
    private final DefaultTableModel modelo = Estilo.modelo("ID","Nombre","Teléfono");
    private final JTable tabla = Estilo.tabla(modelo);
    private final JLabel modo = new JLabel("Nuevo cliente");
    private int id;
    private List<Cliente> clientes = List.of();

    public ClientesPanel(ClienteServicio servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(10,12)); setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(18,20,18,20));
        JPanel superior = Estilo.panel(new BorderLayout(3,5));
        superior.add(Estilo.titulo("Registro de clientes"),BorderLayout.NORTH);
        JPanel f = Estilo.fila(); JButton guardar = Estilo.boton("Guardar",Estilo.MENTA), nuevo = Estilo.boton("Nuevo",Estilo.LILA);
        f.add(new JLabel("Nombre:")); f.add(nombre); f.add(new JLabel("Teléfono:")); f.add(telefono); f.add(guardar); f.add(nuevo);
        superior.add(f,BorderLayout.CENTER); superior.add(modo,BorderLayout.SOUTH);
        add(superior,BorderLayout.NORTH); add(new JScrollPane(tabla),BorderLayout.CENTER);
        add(new JLabel("Seleccione un cliente para modificar sus datos."),BorderLayout.SOUTH);
        guardar.addActionListener(e -> guardar()); nuevo.addActionListener(e -> limpiar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow()>=0) {
                Cliente c = clientes.get(tabla.getSelectedRow()); id=c.id(); nombre.setText(c.nombre()); telefono.setText(c.telefono());
                modo.setText("Editando cliente #"+id);
            }
        });
    }
    public void cargar() throws SQLException {
        clientes=servicio.listar(); modelo.setRowCount(0);
        for (Cliente c:clientes) modelo.addRow(new Object[]{c.id(),c.nombre(),c.telefono()});
    }
    private void limpiar() { id=0; nombre.setText(""); telefono.setText(""); tabla.clearSelection(); modo.setText("Nuevo cliente"); }
    private void guardar() {
        try { servicio.guardar(id,nombre.getText(),telefono.getText()); limpiar(); cargar(); }
        catch (SQLException | IllegalArgumentException e) { Estilo.error(this,e); }
    }
}
