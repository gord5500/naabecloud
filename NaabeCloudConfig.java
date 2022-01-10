package net.runelite.client.plugins.autonaabecloud;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("NaabeCloud")
public interface NaabeCloudConfig extends Config {

    @ConfigItem(
            position = 1,
            keyName = "authCode",
            name = "Auth Code",
            description = "The auth code for your plugin license"
    ) default String authCode() {return "";}

}
