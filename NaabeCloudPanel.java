package net.runelite.client.plugins.autonaabecloud;

import com.google.inject.Inject;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.json.JsonArray;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;

public class NaabeCloudPanel extends PluginPanel {

    private NaabeCloudPlugin plugin;
    private NaabeCloudConfig config;

    @Inject
    public NaabeCloudPanel(NaabeCloudPlugin plugin, NaabeCloudConfig config) {

        super(true);
        this.plugin = plugin;
        this.config = config;

        init();
        revalidate();
        repaint();
    }

    public void init() {

        removeAll();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel title = new JLabel();
        title.setText("Naabe Cloud");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(title.getFont().getFontName(), Font.BOLD, 20));
        titlePanel.add(title, "West");
        titlePanel.add(new JLabel("V1.0"), "East");

        add(titlePanel, "North");

        if (plugin.getLicensePlugins() != null) {
            JsonArray licensedPlugins = plugin.getLicensePlugins().getJsonArray("plugins");

            JPanel pluginPanels = new JPanel();
            GridLayout pluginLayout = new GridLayout(licensedPlugins.size(), 1);
            pluginLayout.setVgap(10);
            pluginPanels.setLayout(pluginLayout);

            for (int i = 0; i < licensedPlugins.size(); i++) {

                String module = licensedPlugins.get(i).asJsonObject().getString("module");
                String imageUrl = licensedPlugins.get(i).asJsonObject().getString("image-url");

                pluginPanels.add(new CloudPluginPanel(plugin, this, module, imageUrl));
            }

            add(pluginPanels, "Center");
        } else {
            add(new JLabel("Error connecting to server."), "Center");
        }
    }
}

class CloudPluginPanel extends JPanel {

    NaabeCloudPlugin plugin;
    NaabeCloudPanel panel;

    public void init(String moduleName, String imageUrl) {

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JPanel nameWrapper = new JPanel(new BorderLayout());
        JLabel moduleLabel = new JLabel(moduleName);
        nameWrapper.add(moduleLabel);
        nameWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        nameWrapper.setBorder(NAME_BOTTOM_BORDER);
        nameWrapper.setFont(new Font(nameWrapper.getFont().getFontName(), Font.BOLD, 30));

        if (plugin.getModule().equals(moduleName)) {

            moduleLabel.setForeground(Color.GREEN);
        }

        add(nameWrapper, "Center");
        try {
            JLabel pluginImage = new JLabel("");
            pluginImage.setIcon(new ImageIcon(ImageIO.read(new URL(imageUrl)).getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
            add(pluginImage, "West");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel controlPanel = new JPanel();

        GridLayout controlLayout = new GridLayout(1, 3);
        controlLayout.setHgap(5);

        controlPanel.setLayout(controlLayout);

        JLabel playButton = new JLabel("");

        playButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                plugin.setModule(moduleName);
                plugin.setRetryAttempts(-1);
                panel.init();
                panel.revalidate();
                panel.repaint();
            }
        });

        JLabel settingButton = new JLabel("");
        JLabel stopButton = new JLabel("");

        stopButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                plugin.setModule("");
                panel.init();
                panel.revalidate();
                panel.repaint();
            }
        });

        playButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(NaabeCloudPanel.class, "play-button.png").getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
        settingButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(NaabeCloudPanel.class, "settings.png").getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
        stopButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(NaabeCloudPanel.class, "error.png").getScaledInstance(20, 20, Image.SCALE_SMOOTH)));

        controlPanel.setBorder(NAME_BOTTOM_BORDER);
        controlPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controlPanel.add(settingButton);
        controlPanel.add(playButton);
        controlPanel.add(stopButton);

        add(controlPanel, "East");
    }

    public CloudPluginPanel(NaabeCloudPlugin plugin, NaabeCloudPanel panel, String moduleName, String imageUrl) {

        super(true);
        this.panel = panel;
        this.plugin = plugin;
        init(moduleName, imageUrl);

    }

    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR), BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

}

