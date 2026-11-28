package ec.edu.ug.galleta;

import ec.edu.ug.galleta.datos.*;
import ec.edu.ug.galleta.servicio.*;
import ec.edu.ug.galleta.vista.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Swing debe crear sus ventanas en el hilo de eventos.
        SwingUtilities.invokeLater(() -> {
            try {
                // La ventana principal solo se abre después de autenticar la cuenta.
                AutenticacionServicio acceso = new AutenticacionServicio(new CuentaDAO(Path.of("acceso.properties")));
                if (AccesoDialogo.ingresar(acceso)) iniciar();
            }
            catch (IOException e) { JOptionPane.showMessageDialog(null,"No se pudo acceder a la cuenta local: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            catch (SQLException e) { JOptionPane.showMessageDialog(null,"No se pudo abrir la base de datos: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        });
    }
    private static void iniciar() throws SQLException {
        Conexion conexion=new Conexion(Path.of("pedidos.db")); conexion.inicializar();
        ProductoDAO productoDAO=new ProductoDAO(conexion); ClienteDAO clienteDAO=new ClienteDAO(conexion);
        ProductoServicio productos=new ProductoServicio(productoDAO); ClienteServicio clientes=new ClienteServicio(clienteDAO);
        PedidoServicio pedidos=new PedidoServicio(new PedidoDAO(conexion),clienteDAO,productoDAO);
        ProductosPanel p=new ProductosPanel(productos); ClientesPanel c=new ClientesPanel(clientes); PedidosPanel o=new PedidosPanel(pedidos,productos,clientes);
        p.cargar(); c.cargar(); o.cargar();
        JFrame ventana=new JFrame("La última galleta - Gestión de pedidos");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); ventana.setMinimumSize(new Dimension(1020,650)); ventana.setSize(1100,740);
        JPanel cabecera=new JPanel(new BorderLayout()); cabecera.setBackground(new Color(106,48,65)); cabecera.setBorder(BorderFactory.createEmptyBorder(17,22,17,22));
        JLabel marca=new JLabel("La última galleta"); marca.setFont(new Font("Serif",Font.BOLD,29)); marca.setForeground(Color.WHITE); cabecera.add(marca,BorderLayout.WEST);
        JLabel subtitulo=new JLabel("Sistema de gestión de pedidos"); subtitulo.setForeground(Color.WHITE); cabecera.add(subtitulo,BorderLayout.EAST);
        JTabbedPane pestanas=new JTabbedPane(); pestanas.addTab("Productos",p); pestanas.addTab("Clientes",c); pestanas.addTab("Pedidos",o);
        // Se actualizan los catálogos al regresar a la pantalla de pedidos.
        pestanas.addChangeListener(e->{try { if(pestanas.getSelectedComponent()==o)o.cargar(); } catch(SQLException ex){JOptionPane.showMessageDialog(ventana,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}});
        ventana.add(cabecera,BorderLayout.NORTH); ventana.add(pestanas,BorderLayout.CENTER);
        ventana.setLocationRelativeTo(null); ventana.setVisible(true);
    }
}
