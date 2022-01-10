package net.runelite.client.plugins.autonaabecloud;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
        name = ".☁. Naabe Cloud .☁.",
        enabledByDefault = true
)
public class NaabeCloudPlugin extends Plugin {

    private PacketBuilder packetBuilder;
    private SocketClient socketClient;

    @Getter
    private ArrayList<String> messages = new ArrayList<String>();

    @Getter
    private ArrayList<GameObject> addedObjects = new ArrayList<>();

    @Getter
    private ArrayList<GameObject> removedObjects = new ArrayList<>();

    @Setter
    private JsonArray actions;

    @Getter
    @Setter
    private JsonObject licensePlugins;

    @Setter
    private JsonArray networkConsole;

    @Getter
    private final String AUTH_CODE = "12345";

    @Getter @Setter
    private String module = "";

    @Inject
    private Client client;

    @Inject
    private NaabeCloudConfig config;

    @Inject
    private ClientToolbar clientToolbar;
    private NavigationButton navButton;

    private int connectDelay = 10;

    @Getter
    private NaabeCloudPanel panel;

    @Getter
    @Setter
    private ArrayList<Integer> requiredObjects;

    @Getter
    @Setter
    private ArrayList<Integer> requiredVarplayers;

    @Setter
    private int retryAttempts = -1;

    private int retryCooldown = 0;

    @Provides
    NaabeCloudConfig provideConfig (ConfigManager configManager) {
        return configManager.getConfig(NaabeCloudConfig.class);
    }

    @Override
    public void startUp() throws IOException {

        panel = injector.getInstance(NaabeCloudPanel.class);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "cloud.png");
        navButton = NavigationButton.builder()
                .tooltip("Naabe Cloud")
                .icon(icon)
                .priority(1)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);


        packetBuilder = new PacketBuilder(client, this, config);
        socketClient = new SocketClient(this, config);
        connectDelay = 10;
    }

    @Override
    public void shutDown() {

        if (socketClient != null)
            socketClient.shutdown();

        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onClientTick(ClientTick tick) {

        if (module.equals("")) {
            if (socketClient != null) {
                socketClient.shutdown();
                socketClient = null;
            }
        } else if (socketClient == null || (socketClient != null && !socketClient.isConnected())) {

            System.out.println(retryAttempts + " " + retryCooldown);
            if (retryAttempts > 0) {

                client.addChatMessage(ChatMessageType.BROADCAST, "", "Connection Lost! Retrying. Attempts left: " + retryAttempts, "");
                if (retryCooldown == 0) {

                    socketClient = new SocketClient(this, config);
                    retryAttempts--;
                    retryCooldown += 600;
                } else {

                    retryCooldown--;
                }
            } else if (retryAttempts == -1) {

                client.addChatMessage(ChatMessageType.BROADCAST, "", "Connecting to Naabe Cloud...", "");
                socketClient = new SocketClient(this, config);
                retryAttempts = -2;
            }
        }

        if (client.getGameState().equals(GameState.LOGGED_IN)) {

            if (actions != null) {

                for (int i = 0; i < actions.size(); i++) {

                    JsonObject action = actions.get(i).asJsonObject();
                    if (action.getInt("val0") == 0) {

                        client.invokeMenuAction(action.getString("val1"),
                                action.getString("val2"),
                                action.getInt("val3"),
                                action.getInt("val4"),
                                action.getInt("val5"),
                                action.getInt("val6"));
                    } else if (action.getInt("val0") == 1) {
                        client.setSelectedSceneTileX(action.getInt("val1") - client.getBaseX());
                        client.setSelectedSceneTileY(action.getInt("val2") - client.getBaseY());
                        client.setViewportWalking(true);
                        client.setCheckClick(false);
                    } else if (action.getInt("val0") == 2) {

                        client.doDialogue(action.getInt("val1"), action.getInt("val2"));
                    }
                }
                actions = null;
            }

            if (networkConsole != null) {

                for (int i = 0; i < networkConsole.size(); i++) {

                    String message = networkConsole.getString(i);
                    if (message.contains("terminated")) {

                        setModule("");
                        SwingUtilities.invokeLater(() -> {
                            getPanel().init();
                            getPanel().revalidate();
                            getPanel().repaint();
                        });
                        socketClient.shutdown();
                    }
                    client.addChatMessage(ChatMessageType.BROADCAST, "", message, "");
                }
                networkConsole = null;
            }

        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage message) {

        messages.add(message.getMessage());
    }

    @Subscribe
    public void onGameTick(GameTick event) {

        if (socketClient != null) {
            System.out.println(socketClient.isConnected());
        }

        if (socketClient != null && socketClient.isConnected()) {

            String message = packetBuilder.buildPacket().toString();
            System.out.println(message);
            socketClient.sendMessage(message);
        }

        client.setKeyboardIdleTicks(0);
        client.setMouseIdleTicks(0);
        messages.clear();
    }

}
