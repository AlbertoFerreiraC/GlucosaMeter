package Medidor;

import org.hid4java.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class GlucoseMeterReader {
    private static final byte[] INIT_COMMAND = {0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] REQUEST_DATA = {0x51, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int PACKET_SIZE = 64;

    public static void main(String[] args) {
        HidServices hidServices = HidManager.getHidServices();
        
        if (hidServices == null) {
            System.err.println("Error al crear los servicios HID.");
            return;
        }

        hidServices.start();

        try {
            int vendorId = 0x1a79;
            int productId = 0x7410;

            HidDevice hidDevice = hidServices.getHidDevice(vendorId, productId, null);

            if (hidDevice != null) {
                System.out.println("Dispositivo encontrado: " + hidDevice.getProduct());
                processGlucoseMeter(hidDevice);
            } else {
                System.out.println("Dispositivo no encontrado. Verificar la conexión y los IDs.");
            }
        } finally {
            hidServices.shutdown();
        }
    }

    private static void processGlucoseMeter(HidDevice device) {
        String fileName = "glucose_data_" + getCurrentDateTime() + ".txt";
        String filePath = System.getProperty("user.home") + "/Desktop/" + fileName;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("Datos del Medidor de Glucosa - " + getCurrentDateTime() + "\n\n");

            if (!device.open()) {
                writer.write("No se pudo abrir el dispositivo.\n");
                System.out.println("No se pudo abrir el dispositivo.");
                return;
            }

            try {
                writer.write("Inicializando dispositivo...\n");
                if (!sendCommand(device, INIT_COMMAND, writer)) {
                    writer.write("Error en la inicialización\n");
                    System.out.println("Error en la inicialización\n");
                    return;
                }
                Thread.sleep(1000);

                writer.write("Solicitando datos...\n");
                System.out.println("Solicitando datos...");
                if (!sendCommand(device, REQUEST_DATA, writer)) {
                    writer.write("Error al solicitar datos\n");
                    System.out.println("Error al solicitar datos");
                    return;
                }

                boolean dataReceived = false;
                int attempts = 0;
                while (!dataReceived && attempts < 10) {
                    byte[] response = readResponse(device);
                    if (response != null) {
                        processAndSaveData(response, writer);
                        dataReceived = true;
                    } else {
                        writer.write("Intento " + (attempts + 1) + ": No se recibieron datos\n");
                        System.out.println("Intento " + (attempts + 1) + ": No se recibieron datos");
                    }
                    attempts++;
                    Thread.sleep(500);
                }

                if (!dataReceived) {
                    writer.write("No se recibieron datos después de varios intentos\n");
                    System.out.println("No se recibieron datos después de varios intentos");
                }

            } finally {
                device.close();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("Proceso completado. Datos guardados en: " + filePath);
    }

    private static boolean sendCommand(HidDevice device, byte[] command, BufferedWriter writer) throws IOException {
        byte[] packet = new byte[PACKET_SIZE];
        System.arraycopy(command, 0, packet, 0, command.length);
        
        writer.write("Enviando comando: " + bytesToHex(command) + "\n");
        int result = device.write(packet, packet.length, (byte) 0x00);
        if (result < 0) {
            writer.write("Error al enviar comando: " + result + "\n");
            System.out.println("Error al enviar comando: " + result);
            return false;
        }
        return true;
    }

    private static byte[] readResponse(HidDevice device) {
        byte[] response = new byte[PACKET_SIZE];
        int bytesRead = device.read(response, 2000);
        
        if (bytesRead > 0) {
            return response;
        }
        return null;
    }

    private static void processAndSaveData(byte[] data, BufferedWriter writer) throws IOException {
        writer.write("Datos recibidos (HEX): " + bytesToHex(data) + "\n");

        writer.write("Análisis detallado del paquete:\n");

        if (data.length >= 3) {
            writer.write("Byte de inicio: 0x" + String.format("%02X", data[0]) + "\n");
        
            writer.write("Segundo byte: 0x" + String.format("%02X", data[1]) + "\n");
        
            int recordCount = data[2] & 0xFF;

            writer.write("Datos de glucosa:\n");
            System.out.println("Datos de glucosa:");
        
            int offset = 3;
            for (int i = 0; i < recordCount && offset + 2 < data.length; i++) {
                int glucoseValue = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                String hexValue = String.format("%02X%02X", data[offset], data[offset + 1]);
                writer.write("  Registro " + (i+1) + ": 0x" + hexValue + " = " + glucoseValue + " mg/dL\n");
                System.out.println("  Registro " + (i+1) + ": 0x" + hexValue + " = " + glucoseValue + " mg/dL");
                offset += 3; 
            }

            if (offset < data.length) {
                writer.write("Bytes adicionales: " + bytesToHex(Arrays.copyOfRange(data, offset, data.length)) + "\n");
            }
        } else {
            writer.write("El paquete recibido es demasiado corto para ser válido.\n");
        }
    
        writer.write("\n");
        writer.flush();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return now.format(formatter);
    }
}