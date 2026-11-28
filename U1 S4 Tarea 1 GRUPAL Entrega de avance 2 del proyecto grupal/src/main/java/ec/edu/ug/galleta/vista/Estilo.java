package ec.edu.ug.galleta.vista;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.math.BigDecimal;

// Elementos que se repiten en las ventanas del sistema.
final class Estilo {
    static final Color FONDO = new Color(252,248,245);
    static final Color ROSA = new Color(239,211,220);
    static final Color MENTA = new Color(218,235,222);
    static final Color LILA = new Color(231,224,242);
    static final Color VINO = new Color(106,48,65);

    private Estilo() {}

    static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout); p.setBackground(FONDO); return p;
    }
    static JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color); b.setForeground(VINO); b.setFocusPainted(false);
        b.setMargin(new Insets(7,13,7,13)); return b;
    }
    static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas,0) {
            @Override public boolean isCellEditable(int row,int col) { return false; }
        };
    }
    static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo); t.setRowHeight(27);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setBackground(LILA); t.getTableHeader().setForeground(VINO);
        t.setSelectionBackground(ROSA); t.setSelectionForeground(Color.BLACK);
        t.setFillsViewportHeight(true); return t;
    }
    static JPanel fila() { return panel(new FlowLayout(FlowLayout.LEFT,8,5)); }
    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto); l.setFont(l.getFont().deriveFont(Font.BOLD,18f)); l.setForeground(VINO); return l;
    }
    static String dinero(BigDecimal valor) { return "$" + valor.toPlainString(); }
    static void error(Component c, Exception e) {
        String mensaje = e instanceof SQLException ? "No se pudo completar la operación. " + e.getMessage() : e.getMessage();
        JOptionPane.showMessageDialog(c,mensaje,"Aviso",JOptionPane.WARNING_MESSAGE);
    }
}
