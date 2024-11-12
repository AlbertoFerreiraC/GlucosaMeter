package Alternativa;

import org.hid4java.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Alberto
 */
public class GlucosaMeterGrafico extends javax.swing.JFrame {

    private static final byte[] COMANDO_INICIO = {0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] SOLICITAR_DATOS = {0x51, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int TAMANO_PAQUETE = 64;

    private DefaultTableModel modeloTabla;
    private HidServices serviciosHid;
    private HidDevice dispositivoHid;

    /**
     * Creates new form GlucosaMeterGrafico
     */
    public GlucosaMeterGrafico() {
        initComponents();
        configurarTabla();
        inicializarHID();
    }

    private void configurarTabla() {
        String[] nombresColumnas = {"Id", "Fecha de Lectura", "Nivel de Glucosa (mg/dL)"};
        modeloTabla = new DefaultTableModel(nombresColumnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        jTable1.setModel(modeloTabla);
    }

    private void inicializarHID() {
        SwingWorker<Void, Void> trabajador = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                serviciosHid = HidManager.getHidServices();
                serviciosHid.start();

                int idVendedor = 0x1a79;
                int idProducto = 0x7410;
                dispositivoHid = serviciosHid.getHidDevice(idVendedor, idProducto, null);
                return null;
            }

            @Override
            protected void done() {
                if (dispositivoHid != null) {
                    label1.setText("Conectado");
                    jButton1.setEnabled(true);
                } else {
                    label1.setText("Desconectado");
                    jButton1.setEnabled(false);
                }
            }
        };
        trabajador.execute();
    }

    private void leerDatosGlucosa() {
        jButton1.setEnabled(false);
        label1.setText("Leyendo...");

        SwingWorker<List<LecturaGlucosa>, Void> trabajador = new SwingWorker<List<LecturaGlucosa>, Void>() {
            @Override
            protected List<LecturaGlucosa> doInBackground() throws Exception {
                List<LecturaGlucosa> lecturas = new ArrayList<>();
                if (dispositivoHid != null && dispositivoHid.open()) {
                    try {
                        enviarComando(COMANDO_INICIO);
                        Thread.sleep(1000);
                        enviarComando(SOLICITAR_DATOS);

                        byte[] respuesta = leerRespuesta();
                        if (respuesta != null) {
                            lecturas = procesarDatos(respuesta);
                        }
                    } finally {
                        dispositivoHid.close();
                    }
                }
                return lecturas;
            }

            @Override
            protected void done() {
                try {
                    List<LecturaGlucosa> lecturas = get();
                    actualizarTabla(lecturas);
                    label1.setText("Conectado");
                } catch (Exception e) {
                    label1.setText("Error");
                    e.printStackTrace();
                }
                jButton1.setEnabled(true);
            }
        };
        trabajador.execute();
    }

    private boolean enviarComando(byte[] comando) throws Exception {
        byte[] paquete = new byte[TAMANO_PAQUETE];
        System.arraycopy(comando, 0, paquete, 0, comando.length);
        return dispositivoHid.write(paquete, paquete.length, (byte) 0x00) >= 0;
    }

    private byte[] leerRespuesta() {
        byte[] respuesta = new byte[TAMANO_PAQUETE];
        return dispositivoHid.read(respuesta, 2000) > 0 ? respuesta : null;
    }

    private List<LecturaGlucosa> procesarDatos(byte[] datos) {
        List<LecturaGlucosa> lecturas = new ArrayList<>();
        if (datos.length >= 3) {
            int conteoRegistros = datos[2] & 0xFF;
            int desplazamiento = 3;
            for (int i = 0; i < conteoRegistros && desplazamiento + 2 < datos.length; i++) {
                int valorGlucosa = ((datos[desplazamiento] & 0xFF) << 8) | (datos[desplazamiento + 1] & 0xFF);
                lecturas.add(new LecturaGlucosa(i + 1, new Date(), valorGlucosa));
                desplazamiento += 3;
            }
        }
        return lecturas;
    }

    private void actualizarTabla(List<LecturaGlucosa> lecturas) {
        modeloTabla.setRowCount(0);
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for (LecturaGlucosa lectura : lecturas) {
            Object[] fila = {
                lectura.id,
                formatoFecha.format(lectura.marca_tiempo),
                lectura.valor
            };
            modeloTabla.addRow(fila);
        }
    }

    private static class LecturaGlucosa {

        int id;
        Date marca_tiempo;
        int valor;

        LecturaGlucosa(int id, Date marca_tiempo, int valor) {
            this.id = id;
            this.marca_tiempo = marca_tiempo;
            this.valor = valor;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        label1 = new java.awt.Label();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton1.setText("Iniciar Lectura");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel1.setText("Estado:");

        label1.setAlignment(java.awt.Label.CENTER);
        label1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        label1.setEnabled(false);
        label1.setText("label1");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 3)); // NOI18N
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Utiles/LOGOO.jpg"))); // NOI18N
        jLabel2.setText("jLabel2");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(label1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        leerDatosGlucosa();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GlucosaMeterGrafico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new GlucosaMeterGrafico().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error al iniciar la aplicación: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        java.awt.EventQueue.invokeLater(() -> {
            new GlucosaMeterGrafico().setVisible(true);
        });
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GlucosaMeterGrafico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GlucosaMeterGrafico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GlucosaMeterGrafico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GlucosaMeterGrafico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GlucosaMeterGrafico().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    private java.awt.Label label1;
    // End of variables declaration//GEN-END:variables
}
