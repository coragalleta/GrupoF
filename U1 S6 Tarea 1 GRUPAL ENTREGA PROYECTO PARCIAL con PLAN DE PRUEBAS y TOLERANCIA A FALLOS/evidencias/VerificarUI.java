import ec.edu.ug.galleta.datos.CuentaDAO;
import ec.edu.ug.galleta.servicio.AutenticacionServicio;
import ec.edu.ug.galleta.vista.AccesoDialogo;
import java.awt.*; import java.awt.image.*; import javax.swing.*; import javax.imageio.ImageIO; import java.nio.file.*; import java.lang.reflect.*;
public class VerificarUI {
 static Object field(Object o,String n) throws Exception {var f=o.getClass().getDeclaredField(n); f.setAccessible(true);return f.get(o);}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception {Path dir=Files.createTempDirectory("galleta-ui"); var dao=new CuentaDAO(dir.resolve("acceso.properties")); var s=new AutenticacionServicio(dao); s.inicializarCuenta(); var ctor=AccesoDialogo.class.getDeclaredConstructor(AutenticacionServicio.class); ctor.setAccessible(true); AccesoDialogo[] a=new AccesoDialogo[1]; SwingUtilities.invokeAndWait(()->{try{a[0]=ctor.newInstance(s); a[0].setModal(false);a[0].setVisible(true);}catch(Exception e){throw new RuntimeException(e);}}); var d=a[0]; var u=(JTextField)field(d,"usuario");var p=(JPasswordField)field(d,"contrasena");var b=(JButton)field(d,"entrar");var l=(JLabel)field(d,"aviso"); Robot robot=new Robot(); robot.waitForIdle(); snap(robot,d,args[0],"Acceso inicial.png");
  SwingUtilities.invokeAndWait(()->{u.setText("otro");p.setText("incorrecta");b.doClick();}); check(l.getText().contains("existe")); snap(robot,d,args[0],"Usuario inexistente.png");
  SwingUtilities.invokeAndWait(()->{u.setText("admin");p.setText("incorrecta");b.doClick();}); check(l.getText().toLowerCase().contains("incorrecta")); snap(robot,d,args[0],"Contrasena incorrecta.png");
  SwingUtilities.invokeAndWait(()->{p.setText("incorrecta");b.doClick();p.setText("incorrecta");b.doClick();}); check(!b.isEnabled()&&dao.leer().intentosFallidos()==3); snap(robot,d,args[0],"Cuenta bloqueada.png");
  SwingUtilities.invokeAndWait(d::dispose); Files.delete(dir.resolve("acceso.properties")); s.inicializarCuenta(); s.autenticar("admin","Galleta2026!".toCharArray()); try{s.autenticar("admin","incorrecta".toCharArray());throw new AssertionError();}catch(ec.edu.ug.galleta.excepcion.ContrasenaIncorrectaException e){} s.inicializarCuenta();check(dao.leer().intentosFallidos()==1);System.out.println("AUT-07: Cuenta inicial y conservación de intentos: OK");System.out.println("Interfaz: apertura, usuario inexistente, contraseña incorrecta y bloqueo al tercer intento: OK");Files.delete(dir.resolve("acceso.properties"));Files.delete(dir);System.exit(0);
 }
 static void snap(Robot r,JDialog d,String path,String name)throws Exception{r.waitForIdle();Thread.sleep(200);ImageIO.write(r.createScreenCapture(d.getBounds()),"png",Path.of(path,name).toFile());}
}
