package ec.edu.ug.galleta.vista;

import ec.edu.ug.galleta.excepcion.*;
import ec.edu.ug.galleta.servicio.AutenticacionServicio;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

public final class AccesoDialogo extends JDialog {
    private static final Color VINO = Estilo.VINO;
    private static final Color CREMA = Estilo.FONDO;
    private static final Color TINTA = new Color(58, 39, 42);
    private static final Color SECUNDARIO = new Color(111, 93, 95);
    private static final Color LINEA = new Color(211, 195, 190);

    private final AutenticacionServicio servicio;
    private final JTextField usuario = new JTextField(20);
    private final JPasswordField contrasena = new JPasswordField(20);
    private final JLabel aviso = new JLabel(" ");
    private final JButton entrar = new JButton("Iniciar sesión");
    private boolean autorizado;

    private AccesoDialogo(AutenticacionServicio servicio) {
        super((Frame) null, "La última galleta — Acceso", true);
        this.servicio = servicio;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.add(marca(), BorderLayout.WEST);
        contenido.add(formulario(), BorderLayout.CENTER);
        setContentPane(contenido);
        getRootPane().setDefaultButton(entrar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                usuario.requestFocusInWindow();
            }
        });
    }

    public static boolean ingresar(AutenticacionServicio servicio) throws IOException {
        servicio.inicializarCuenta();
        AccesoDialogo ventana = new AccesoDialogo(servicio);
        ventana.setVisible(true);
        return ventana.autorizado;
    }

    private JPanel marca() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(VINO);
        panel.setPreferredSize(new Dimension(248, 430));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 25, 30, 25));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.insets = new Insets(0, 0, 20, 0);
        panel.add(new Sello(), c);
        c.gridy++; c.insets = new Insets(0, 0, 0, 0);
        panel.add(texto("La última", new Font("Serif", Font.BOLD, 34), Color.WHITE), c);
        c.gridy++; c.insets = new Insets(0, 0, 17, 0);
        panel.add(texto("galleta", new Font("Serif", Font.BOLD, 34), Color.WHITE), c);
        c.gridy++; c.insets = new Insets(0, 0, 5, 0);
        panel.add(texto("PASTELERÍA", new Font("SansSerif", Font.BOLD, 12), new Color(246, 211, 193)), c);
        c.gridy++; c.insets = new Insets(0, 0, 0, 0);
        panel.add(texto("Gestión de pedidos", new Font("SansSerif", Font.PLAIN, 14), new Color(251, 234, 229)), c);
        return panel;
    }

    private JPanel formulario() {
        JPanel panel = Estilo.panel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(410, 430));
        panel.setBorder(BorderFactory.createEmptyBorder(27, 37, 25, 37));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.insets = new Insets(0, 0, 6, 0);
        panel.add(texto("Iniciar sesión", new Font("SansSerif", Font.BOLD, 26), TINTA), c);
        c.gridy++; c.insets = new Insets(0, 0, 25, 0);
        panel.add(texto("Acceso al sistema de gestión", new Font("SansSerif", Font.PLAIN, 13), SECUNDARIO), c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        JLabel etiquetaUsuario = etiqueta("Usuario", usuario);
        panel.add(etiquetaUsuario, c);
        c.gridy++; c.insets = new Insets(0, 0, 15, 0);
        campo(usuario); panel.add(usuario, c);
        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        panel.add(etiqueta("Contraseña", contrasena), c);
        c.gridy++; c.insets = new Insets(0, 0, 7, 0);
        campo(contrasena); panel.add(contrasena, c);

        JCheckBox mostrar = new JCheckBox("Mostrar contraseña");
        mostrar.setOpaque(false); mostrar.setForeground(SECUNDARIO);
        mostrar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mostrar.setMargin(new Insets(0, 0, 0, 0));
        char oculto = contrasena.getEchoChar();
        mostrar.addActionListener(e -> contrasena.setEchoChar(mostrar.isSelected() ? (char) 0 : oculto));
        c.gridy++; c.insets = new Insets(0, 0, 9, 0); panel.add(mostrar, c);

        aviso.setFont(new Font("SansSerif", Font.PLAIN, 12));
        aviso.setForeground(VINO);
        aviso.setVerticalAlignment(SwingConstants.TOP);
        aviso.setPreferredSize(new Dimension(330, 36));
        c.gridy++; c.insets = new Insets(0, 0, 5, 0); panel.add(aviso, c);

        entrar.setUI(new BasicButtonUI());
        entrar.setBackground(VINO); entrar.setForeground(Color.WHITE);
        entrar.setFont(new Font("SansSerif", Font.BOLD, 14));
        entrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entrar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        entrar.setOpaque(true);
        entrar.addActionListener(e -> validar());
        c.gridy++; c.insets = new Insets(0, 0, 9, 0); panel.add(entrar, c);
        JButton cancelar = new JButton("Salir");
        cancelar.setUI(new BasicButtonUI()); cancelar.setContentAreaFilled(false);
        cancelar.setForeground(SECUNDARIO); cancelar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cancelar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        cancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelar.addActionListener(e -> dispose());
        c.gridy++; c.insets = new Insets(0, 0, 0, 0); panel.add(cancelar, c);
        return panel;
    }

    private static JLabel texto(String texto, Font fuente, Color color) {
        JLabel etiqueta = new JLabel(texto); etiqueta.setFont(fuente); etiqueta.setForeground(color);
        return etiqueta;
    }

    private static JLabel etiqueta(String nombre, JComponent campo) {
        JLabel etiqueta = texto(nombre, new Font("SansSerif", Font.BOLD, 13), TINTA);
        etiqueta.setLabelFor(campo);
        return etiqueta;
    }

    private static void campo(JTextField campo) {
        campo.setFont(new Font("SansSerif", Font.PLAIN, 15));
        campo.setForeground(TINTA); campo.setBackground(Color.WHITE);
        campo.setCaretColor(VINO);
        campo.setPreferredSize(new Dimension(330, 40));
        Border normal = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINEA),
                BorderFactory.createEmptyBorder(8, 11, 8, 11));
        Border activo = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(VINO),
                BorderFactory.createEmptyBorder(8, 11, 8, 11));
        campo.setBorder(normal);
        campo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { campo.setBorder(activo); }
            @Override public void focusLost(FocusEvent e) { campo.setBorder(normal); }
        });
    }

    /** Gestiona fallos de acceso sin abrir la ventana de gestión. */
    private void validar() {
        char[] clave = contrasena.getPassword();
        try {
            servicio.autenticar(usuario.getText().trim(), clave);
            autorizado = true;
            dispose();
        } catch (UsuarioNoExisteException | ContrasenaIncorrectaException e) {
            aviso.setText(e.getMessage());
            if (e instanceof UsuarioNoExisteException) usuario.requestFocusInWindow();
            else contrasena.requestFocusInWindow();
        } catch (CuentaBloqueadaException e) {
            aviso.setText("<html>Cuenta bloqueada por tres intentos fallidos.<br>Acceso denegado.</html>");
            usuario.setEnabled(false);
            contrasena.setEnabled(false);
            entrar.setEnabled(false);
            entrar.setBackground(LINEA);
        } catch (IOException e) {
            aviso.setText("<html>No se pudo verificar la cuenta.<br>Intente nuevamente.</html>");
        } finally {
            Arrays.fill(clave, '\0');
            contrasena.setText("");
        }
    }

    private static final class Sello extends JComponent {
        Sello() { setPreferredSize(new Dimension(67, 67)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D d = (Graphics2D) g.create();
            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            d.setColor(new Color(244, 205, 158)); d.fillOval(1, 1, 64, 64);
            d.setColor(new Color(218, 160, 113)); d.fillOval(6, 6, 54, 54);
            d.setColor(new Color(100, 55, 49));
            int[][] chispas = {{16, 17, 8}, {36, 12, 7}, {45, 30, 8}, {24, 34, 9}, {14, 44, 6}, {36, 48, 7}};
            for (int[] p : chispas) d.fillOval(p[0], p[1], p[2], p[2]);
            d.dispose();
        }
    }
}
