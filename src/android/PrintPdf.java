package com.rits.printpdf;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
// import android.content.pm.PackageManager;
// import android.content.pm.ResolveInfo;
// import android.os.Build;
// import android.webkit.MimeTypeMap;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
// import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.CordovaResourceApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.io.File;
import java.net.*;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLSelector;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;


/**
 * This class echoes a string called from JavaScript.
 */
public class PrintPdf extends CordovaPlugin {

    private static PrintListener printListener;

    public enum PaperSize {
        A4,
        A5
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) throws JSONException {
        if (action.equals("print")) {
            // this.print(args, callback);

            // New logic
            String fileUrl = args.getJSONObject(0).getString("pdfFilePath");
            // File file = new File(args.getJSONObject(0).getString("pdfFilePath"));
            String filename =args.getJSONObject(0).getString("pdfFileName");
            String printerIP = args.getJSONObject(0).getString("printerIp");
            int printerPort = Integer.parseInt(args.getJSONObject(0).getString("printerPort"));
            int copies = Integer.parseInt(args.getJSONObject(0).getString("noOfCopy"));
            PaperSize paperSize = PaperSize.A4;

            // Get corret file path
            String filePath = "";
            try {
                CordovaResourceApi resourceApi = webView.getResourceApi();
                Uri fileUri = resourceApi.remapUri(Uri.parse(fileUrl));
                filePath = fileUri.getPath();
            } catch (Exception e) {
                filePath = fileUrl;
            }
            File file = new File(filePath);

            // FOR TESTING FILE PATH ISSUE
            /*cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    FileInputStream inputStream = null;
                    int x = 0;
                    try {
                        inputStream = new FileInputStream(file);
                        byte[] buffer = new byte[3000];

                        while (inputStream.read(buffer) != -1)
                            x = x + 1;

                    } catch (IOException e) {
                        callback.error("ERROR1 - " + e);
                    } finally {
                        callback.success("SUCCESS");
                    }
                }
            });*/

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Socket socket = null;
                    DataOutputStream out = null;
                    FileInputStream inputStream = null;
                    try {
                        socket = new Socket(printerIP, printerPort);
                        out = new DataOutputStream(socket.getOutputStream());
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        inputStream = new FileInputStream(file);
                        byte[] buffer = new byte[3000];

                        final char ESC = 0x1b;
                        final String UEL = ESC + "%-12345X";
                        final String ESC_SEQ = ESC + "%-12345\r\n";

                        out.writeBytes(UEL);
                        out.writeBytes("@PJL \r\n");
                        out.writeBytes("@PJL JOB NAME = '" + filename + "' \r\n");
                        out.writeBytes("@PJL SET PAPER=" + paperSize.name());
                        out.writeBytes("@PJL SET COPIES=" + copies);
                        out.writeBytes("@PJL ENTER LANGUAGE = PDF\r\n");
                        while (inputStream.read(buffer) != -1)
                            out.write(buffer);
                        out.writeBytes(ESC_SEQ);
                        out.writeBytes("@PJL \r\n");
                        out.writeBytes("@PJL RESET \r\n");
                        out.writeBytes("@PJL EOJ NAME = '" + filename + "'");
                        out.writeBytes(UEL);

                        out.flush();
                    } catch (IOException e) {
                        callback.error("ERROR1 - " + e);
                    } finally {
                        try {
                            if (inputStream != null)
                                inputStream.close();
                            if (out != null)
                                out.close();
                            if (socket != null)
                                socket.close();
                            if (printListener != null)
                                callback.success("SUCCESS");
                        } catch (IOException e) {
                            callback.error("ERROR2 - " + e);
                        }
                    }
                }
            });

            return true;
        }
        return false;
    }

    public void print(JSONArray args, CallbackContext callback){
        try {
            File pdfFile = new File(args.getJSONObject(0).getString("pdfFilePath"));
            String printerIp = args.getJSONObject(0).getString("printerIp");
            int printerPort = Integer.parseInt(args.getJSONObject(0).getString("printerPort"));
            int noOfCopy = Integer.parseInt(args.getJSONObject(0).getString("noOfCopy"));

            this.printPDFFile(callback, printerIp, printerPort, pdfFile, "test.pdf", PaperSize.A4, noOfCopy);
        } catch(Exception ex) {
            callback.error("ERROR - " + ex);
        }
    }

    public static void printPDFFile(CallbackContext callback, final String printerIP, final int printerPort, final File file, final String filename, final PaperSize paperSize, final int copies) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                DataOutputStream out = null;
                FileInputStream inputStream = null;
                try {
                    socket = new Socket(printerIP, printerPort);
                    out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[3000];

                    final char ESC = 0x1b;
                    final String UEL = ESC + "%-12345X";
                    final String ESC_SEQ = ESC + "%-12345\r\n";

                    out.writeBytes(UEL);
                    out.writeBytes("@PJL \r\n");
                    out.writeBytes("@PJL JOB NAME = '" + filename + "' \r\n");
                    out.writeBytes("@PJL SET PAPER=" + paperSize.name());
                    out.writeBytes("@PJL SET COPIES=" + copies);
                    out.writeBytes("@PJL ENTER LANGUAGE = PDF\r\n");
                    while (inputStream.read(buffer) != -1)
                        out.write(buffer);
                    out.writeBytes(ESC_SEQ);
                    out.writeBytes("@PJL \r\n");
                    out.writeBytes("@PJL RESET \r\n");
                    out.writeBytes("@PJL EOJ NAME = '" + filename + "'");
                    out.writeBytes(UEL);

                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (printListener != null) {
                        callback.error("ERROR -> " + e);
                        printListener.networkError();
                    }
                } finally {
                    try {
                        if (inputStream != null)
                            inputStream.close();
                        if (out != null)
                            out.close();
                        if (socket != null)
                            socket.close();
                        if (printListener != null) {
                            callback.success(filename);
                            printListener.printCompleted();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (printListener != null) {
                            callback.error("ERROR -> " + e);
                            printListener.networkError();
                        }
                    }
                }
            }
        });
        t.start();
    }

    public static void setPrintListener(PrintListener list) {
        printListener = list;
    }

    public interface PrintListener {
        void printCompleted();

        void networkError();
    }
}
