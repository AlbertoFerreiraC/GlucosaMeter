package Alternativa;

import Conexion.Conexion;
import org.hid4java.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GlucoseMeterReader extends JFrame {

    private static final byte[] INIT_COMMAND = {0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] REQUEST_DATA = {0x51, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int PACKET_SIZE = 64;

    private JLabel statusLabel;
    private JButton readButton;
    private JButton insertButton;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private HidServices hidServices;
    private HidDevice hidDevice;

    public GlucoseMeterReader() {
        setTitle("Medidor de Glucosa");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.WHITE);

        // Panel principal con borde
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Panel superior
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        // Agregar logo
        /*JLabel logoLabel = new JLabel(new ImageIcon("/src/Utiles/LOGO.jpg"));
        topPanel.add(logoLabel, BorderLayout.WEST);*/

        // Botón Leer
        readButton = new JButton("Leer");
        readButton.setPreferredSize(new Dimension(100, 30));
        readButton.setEnabled(false);

        // Botón Insertar
        insertButton = new JButton("Insertar Seleccionados");
        insertButton.setPreferredSize(new Dimension(200, 30));
        insertButton.setEnabled(false);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(readButton);
        buttonPanel.add(insertButton);

        // Panel de estado
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(Color.WHITE);
        JLabel estadoLabel = new JLabel("Estado: ");
        statusLabel = new JLabel("Desconectado");
        statusPanel.add(estadoLabel);
        statusPanel.add(statusLabel);

        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // Tabla
        String[] columnNames = {"Seleccionar", "Id", "Fecha de Lectura", "Nivel de Glucosa (mg/dL)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setGridColor(Color.LIGHT_GRAY);
        dataTable.setShowGrid(true);
        dataTable.setIntercellSpacing(new Dimension(1, 1));

        // Configurar el ancho de las columnas
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Checkbox
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(10);  // Id
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Fecha
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Nivel

        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Agregar componentes al panel principal
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Agregar panel principal al frame
        add(mainPanel);

        // Inicializar HID y configurar eventos
        initializeHID();
        setupEventHandlers();
    }

    private void initializeHID() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                hidServices = HidManager.getHidServices();
                hidServices.start();

                int vendorId = 0x1a79;
                int productId = 0x7410;
                hidDevice = hidServices.getHidDevice(vendorId, productId, null);
                return null;
            }

            @Override
            protected void done() {
                if (hidDevice != null) {
                    statusLabel.setText("Conectado");
                    readButton.setEnabled(true);
                }
            }
        }; /*Prueba para push*/
        worker.execute();
    }

    private void setupEventHandlers() {
        readButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readGlucoseData();
            }
        });

        insertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertSelectedReadings();
            }
        });
    }

    private void readGlucoseData() {
        readButton.setEnabled(false);
        statusLabel.setText("Leyendo...");

        SwingWorker<List<GlucoseReading>, Void> worker = new SwingWorker<List<GlucoseReading>, Void>() {
            @Override
            protected List<GlucoseReading> doInBackground() throws Exception {
                List<GlucoseReading> readings = new ArrayList<>();
                if (hidDevice != null && hidDevice.open()) {
                    try {
                        // Enviar comandos y leer datos
                        sendCommand(INIT_COMMAND);
                        Thread.sleep(1000);
                        sendCommand(REQUEST_DATA);

                        byte[] response = readResponse();
                        if (response != null) {
                            readings = processData(response);
                        }
                    } finally {
                        hidDevice.close();
                    }
                }
                return readings;
            }

            @Override
            protected void done() {
                try {
                    List<GlucoseReading> readings = get();
                    updateTable(readings);
                    statusLabel.setText("Conectado");
                    insertButton.setEnabled(true);
                } catch (Exception e) {
                    statusLabel.setText("Error");
                    e.printStackTrace();
                }
                readButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private boolean sendCommand(byte[] command) throws Exception {
        byte[] packet = new byte[PACKET_SIZE];
        System.arraycopy(command, 0, packet, 0, command.length);
        return hidDevice.write(packet, packet.length, (byte) 0x00) >= 0;
    }

    private byte[] readResponse() {
        byte[] response = new byte[PACKET_SIZE];
        return hidDevice.read(response, 2000) > 0 ? response : null;
    }

    private List<GlucoseReading> processData(byte[] data) {
        List<GlucoseReading> readings = new ArrayList<>();
        if (data.length >= 3) {
            int recordCount = data[2] & 0xFF;
            int offset = 3;
            for (int i = 0; i < recordCount && offset + 2 < data.length; i++) {
                int glucoseValue = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                readings.add(new GlucoseReading(i + 1, new Date(), glucoseValue));
                offset += 3;
            }
        }
        return readings;
    }

    private void updateTable(List<GlucoseReading> readings) {
        tableModel.setRowCount(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for (GlucoseReading reading : readings) {
            Object[] row = {
                false,
                reading.id,
                dateFormat.format(reading.timestamp),
                reading.value
            };
            tableModel.addRow(row);
        }
    }

    // Previous constants and fields remain the same...
    private void insertSelectedReadings() {
        List<GlucoseReading> selectedReadings = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected) {
                int id = (int) tableModel.getValueAt(i, 1);
                Date timestamp = null;
                try {
                    timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse((String) tableModel.getValueAt(i, 2));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int value = (int) tableModel.getValueAt(i, 3);
                selectedReadings.add(new GlucoseReading(id, timestamp, value));
            }
        }

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        PreparedStatement insertLectorStmt = null;
        int insertedCount = 0;

        try {
            conn = Conexion.getConnection();
            conn.setAutoCommit(false);

            // Verificar si el registro ya existe
            String checkSql = "SELECT COUNT(*) FROM registro_glucosa WHERE nro_registro = ? AND lectura_glucosa = ?";
            checkStmt = conn.prepareStatement(checkSql);

            // Insertar en registro_glucosa
            String insertSql = "INSERT INTO registro_glucosa (nro_registro, lectura_glucosa) VALUES (?, ?)";
            insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

            // Insertar en registro_de_lector
            String insertLectorSql = "INSERT INTO registro_de_lector (nro_registro_de_lector, cantidad_lector_lectura, cantidad_despues_de_lectura) VALUES (?, ?, ?)";
            insertLectorStmt = conn.prepareStatement(insertLectorSql);

            for (GlucoseReading reading : selectedReadings) {
                // Verificar si el registro ya existe
                checkStmt.setInt(1, reading.id);
                checkStmt.setString(2, String.valueOf(reading.value));
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count == 0) {
                    // Insertar en registro_glucosa
                    insertStmt.setInt(1, reading.id);
                    insertStmt.setString(2, String.valueOf(reading.value));
                    insertStmt.executeUpdate();

                    // Insertar en registro_de_lector
                    insertLectorStmt.setInt(1, reading.id);
                    insertLectorStmt.setInt(2, 1); // cantidad_lector_lectura
                    insertLectorStmt.setInt(3, 0); // cantidad_despues_de_lectura
                    insertLectorStmt.executeUpdate();

                    insertedCount++;
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this,
                    "Se insertaron " + insertedCount + " nuevos registros en la base de datos.",
                    "Inserción Exitosa",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(this,
                    "Error al insertar registros: " + e.getMessage(),
                    "Error de Inserción",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try {
                if (checkStmt != null) {
                    checkStmt.close();
                }
                if (insertStmt != null) {
                    insertStmt.close();
                }
                if (insertLectorStmt != null) {
                    insertLectorStmt.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static class GlucoseReading {

        int id;
        Date timestamp;
        int value;

        GlucoseReading(int id, Date timestamp, int value) {
            this.id = id;
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GlucoseMeterReader frame = new GlucoseMeterReader();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
