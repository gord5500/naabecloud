package net.runelite.client.plugins.autonaabecloud;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.net.*;
import java.util.ArrayList;

public class SocketClient {

    final static int port = 55555;

    private NaabeCloudPlugin plugin;
    private NaabeCloudConfig config;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private Socket s;

    private Thread readMessage = new Thread(new Runnable() {
        @Override
        public void run() {

            while (ois != null) {
                 try {

                    JsonReader jsonReader = Json.createReader(new StringReader(ois.readObject().toString().replaceAll("\r?\n", "")));
                    JsonObject object = jsonReader.readObject();
                    jsonReader.close();

                     System.out.println(object.toString());
                    if (object.containsKey("plugins")) {

                        plugin.setLicensePlugins(object);

                        SwingUtilities.invokeLater(() -> {
                            plugin.getPanel().init();
                            plugin.getPanel().revalidate();
                            plugin.getPanel().repaint();
                        });
                    }

                    if (object.containsKey("actions")) {
                        JsonArray actions = object.getJsonArray("actions");

                        if (actions != null) {

                            plugin.setActions(actions);
                        }
                    }

                    if (object.containsKey("console")) {
                        JsonArray console = object.getJsonArray("console");

                        if (console != null) {

                            plugin.setNetworkConsole(console);
                        }
                    }

                    if (object.containsKey("required-varplayers")) {

                        JsonArray varps = object.getJsonArray("required-varplayers");
                        ArrayList<Integer> listVarps = new ArrayList<>();

                        for (int i = 0; i < varps.size(); i++) {
                            listVarps.add(varps.getInt(i));
                        }

                        plugin.setRequiredVarplayers(listVarps);
                    }

                    if (object.containsKey("required-objects")) {
                        JsonArray ids = object.getJsonArray("required-objects");
                        ArrayList<Integer> listIds = new ArrayList<>();

                        for (int i = 0; i < ids.size(); i++) {

                            listIds.add(ids.getInt(i));
                        }

                        plugin.setRequiredObjects(listIds);
                    }

                } catch (JsonParsingException e) {
                    System.out.println(e);
                } catch (IOException | ClassNotFoundException e) {

                    ois = null;
                }
            }
        }
    });

    public boolean isConnected() {

        return s != null && oos != null;
    }

    public void sendMessage(String message) {

        if (s != null && oos != null) {
            try {

                oos.writeObject(message);
            } catch (SocketException e){

                s = null;
                oos = null;
                plugin.setRetryAttempts(3);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public SocketClient(NaabeCloudPlugin plugin, NaabeCloudConfig config) {

        this.config = config;
        this.plugin = plugin;
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("naabe.cloud");
            s = new Socket(ip, port);

            oos = new ObjectOutputStream(s.getOutputStream());
            ois = new ObjectInputStream(s.getInputStream());

            sendMessage(Json.createObjectBuilder().add("request-connection", config.authCode()).build().toString());
            readMessage.start();
        } catch (ConnectException e) {

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void shutdown() {

        readMessage.interrupt();
        try {
            oos.close();
            ois.close();
            ois = null;
            oos = null;
            s = null;
        } catch (IOException e) {
            System.out.println("Socket was already dead when we tried to kill");
        } catch (NullPointerException e) {
            System.out.println("Socket was already dead when we tried to kill");
        }
    }
}



