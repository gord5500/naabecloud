package net.runelite.client.plugins.autonaabecloud;

import net.runelite.api.*;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.GroundObjectQuery;
import net.runelite.api.queries.PlayerQuery;
import net.runelite.api.widgets.Widget;

import javax.json.*;

public class PacketBuilder {

    private Client client;
    private NaabeCloudPlugin plugin;
    private NaabeCloudConfig config;

    private final int[] WIDGETS = new int [] {786445, // BANK CONTAINER
            15269889, 10616885, 12648448, // Level up
            10485784 // RUN ENERGY TEXT
    };

    private final String[] SKILLS = new String[] {"Attack", "Defence", "Strength",
        "Hitpoints", "Ranged", "Prayer", "Magic", "Cooking", "Woodcutting", "Fletching", "Fishing",
        "Firemaking", "Crafting", "Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecrafting",
        "Construction"};

    public PacketBuilder(Client client, NaabeCloudPlugin plugin, NaabeCloudConfig config) {

        this.plugin = plugin;
        this.client = client;
        this.config = config;
    }

    public JsonObject buildPacket() {

        JsonObjectBuilder builder =  Json.createObjectBuilder();

        builder.add("auth", Json.createObjectBuilder().add("code", config.authCode()).add("module", plugin.getModule())
                .add("rsn", client.getGameState().equals(GameState.LOGGED_IN) ? client.getLocalPlayer().getName() : "null"));

        builder.add("client", getClient());

        builder.add("local-player", Json.createObjectBuilder().add("world", client.getGameState().equals(GameState.LOGGED_IN) ? client.getWorld() : -1)
                .add("rsn", client.getGameState().equals(GameState.LOGGED_IN) ? client.getLocalPlayer().getName() : "null")
                .add("animation-id", client.getLocalPlayer().getAnimation())
                .add("interacting-name", client.getLocalPlayer().getInteracting() != null ? client.getLocalPlayer().getInteracting().getName() : "null")
                .add("interacting-index", (client.getLocalPlayer().getInteracting() != null
                        && client.getLocalPlayer().getInteracting() instanceof NPC) ?
                        ((NPC) client.getLocalPlayer().getInteracting()).getIndex() : -1)
                .add("scene-pos", Json.createObjectBuilder()
                        .add("x", client.getLocalPlayer().getLocalLocation().getSceneX())
                        .add("y", client.getLocalPlayer().getLocalLocation().getSceneY()))
                .add("world-pos", Json.createObjectBuilder()
                        .add("x", client.getLocalPlayer().getWorldLocation().getX())
                        .add("y", client.getLocalPlayer().getWorldLocation().getY()))
                .add("scene-dest-pos", Json.createObjectBuilder()
                        .add("x", client.getLocalDestinationLocation() != null ? client.getLocalDestinationLocation().getSceneX() : -1)
                        .add("y", client.getLocalDestinationLocation() != null ? client.getLocalDestinationLocation().getSceneY() : -1))
                .add("world-dest-pos", Json.createObjectBuilder()
                        .add("x", client.getLocalDestinationLocation() != null ? client.getLocalDestinationLocation().getX() + client.getBaseX() : -1)
                        .add("y", client.getLocalDestinationLocation() != null ? client.getLocalDestinationLocation().getY() + client.getBaseY() : -1))
                .add("graphic", client.getLocalPlayer().getGraphic()));

        builder.add("inventory", getInventory());
        builder.add("players", getPlayers());
        builder.add("npcs", getNpcs());
        builder.add("spawned-objects", getSpawnedObjects());
        builder.add("chat-messages", getChatMessages());
        builder.add("widgets", getWidgets());
        builder.add("skills", getSkills());
        builder.add("varps", getVarps());

        return builder.build();
    }

    private JsonObject getVarps() {

        JsonObjectBuilder varpsBuilder = Json.createObjectBuilder();

        if (plugin.getRequiredVarplayers() != null) {

            for (int varp : plugin.getRequiredVarplayers()) {

                varpsBuilder.add("" + varp, client.getVarpValue(varp));
            }
        }

        return varpsBuilder.build();
    }

    private JsonArray getSpawnedObjects() {

        JsonArrayBuilder objectBuilder = Json.createArrayBuilder();

        if (plugin.getRequiredObjects() != null) {
            for (GameObject object : new GameObjectQuery().idEquals(plugin.getRequiredObjects()).result(client)) {

                final ObjectComposition def = client.getObjectDefinition(object.getId());

                JsonObject gameObjectBuilder = Json.createObjectBuilder()
                        .add("id", def.getId())
                        .add("name", def.getName())
                        .add("sceneX", object.getSceneMinLocation().getX())
                        .add("sceneY", object.getSceneMinLocation().getY())
                        .add("worldX", object.getWorldLocation().getX())
                        .add("worldY", object.getWorldLocation().getY())
                        .build();

                objectBuilder.add(gameObjectBuilder);
            }

            for (GroundObject object : new GroundObjectQuery().idEquals(plugin.getRequiredObjects()).result(client)) {

                final ObjectComposition def = client.getObjectDefinition(object.getId());

                JsonObject gameObjectBuilder = Json.createObjectBuilder()
                        .add("id", def.getId())
                        .add("name", def.getName())
                        .add("sceneX", object.getLocalLocation().getSceneX())
                        .add("sceneY", object.getLocalLocation().getSceneY())
                        .add("worldX", object.getWorldLocation().getX())
                        .add("worldY", object.getWorldLocation().getY())
                        .build();

                objectBuilder.add(gameObjectBuilder);
            }

        }

        return objectBuilder.build();
    }

    private JsonArray getNpcs() {
        JsonArrayBuilder npcBuilder = Json.createArrayBuilder();

        for (NPC npc : client.getNpcs()) {

            JsonObject gameNpcBuilder = Json.createObjectBuilder()
                    .add("id", npc.getId())
                    .add("name", npc.getName() != null ? npc.getName() : "null")
                    .add("index", npc.getIndex())
                    .add("sceneX", npc.getLocalLocation().getSceneX())
                    .add("sceneY", npc.getLocalLocation().getSceneY())
                    .add("interacting", npc.getInteracting() != null ? npc.getInteracting().getName() : "null")
                    .add("hp", npc.getHealthRatio())
                    .add("is-dead", npc.isDead())
                    .build();

            npcBuilder.add(gameNpcBuilder);
        }

        return npcBuilder.build();
    }

    private JsonObject getSkills() {

        JsonObjectBuilder skills = Json.createObjectBuilder();

        for (int i = 0; i < client.getRealSkillLevels().length - 3; i++) {

            skills.add(SKILLS[i], Json.createObjectBuilder()
                    .add("level", client.getRealSkillLevels()[i])
                    .add("experience", client.getSkillExperiences()[i]));
        }

        return skills.build();
    }

    private JsonArray getWidgets() {

        JsonArrayBuilder widgets = Json.createArrayBuilder();

        for (int widgetId : WIDGETS) {

            Widget widget = client.getWidget(widgetId);
            JsonObjectBuilder widgetBuilder = Json.createObjectBuilder();

            widgetBuilder.add("id", widgetId);

            if (widget != null && !widget.isHidden()) {

                JsonArrayBuilder childArray = Json.createArrayBuilder();
                widgetBuilder.add("visible", true);
                widgetBuilder.add("text", widget.getText());
                widgetBuilder.add("item-id", widget.getItemId());
                widgetBuilder.add("item-quantity", widget.getItemQuantity());

                if (widget.getChildren() != null) {
                    for (Widget child : widget.getChildren()) {

                        JsonObjectBuilder childObject = Json.createObjectBuilder();

                        childObject
                                .add("index", child.getIndex())
                                .add("visible", !child.isHidden())
                                .add("text", child.getText())
                                .add("item-id", child.getItemId())
                                .add("item-quantity", child.getItemQuantity());

                        childArray.add(childObject);
                    }
                }

                widgetBuilder.add("children", childArray);
            } else {

                widgetBuilder.add("visible", false);
                widgetBuilder.add("text", "null");
                widgetBuilder.add("item-id", -1);
                widgetBuilder.add("item-quantity", -1);
            }

            widgets.add(widgetBuilder);
        }

        return widgets.build();
    }

    private JsonArray getChatMessages() {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (String message : plugin.getMessages()) {

            builder.add(message);
        }

        return builder.build();
    }

    private JsonObject getClient() {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        JsonArrayBuilder regionBuilder = Json.createArrayBuilder();
        for (int id : client.getMapRegions()) {
            regionBuilder.add(id);
        }

        builder.add("map-regions", regionBuilder);
        builder.add("current-region", client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation().getRegionID() : -1);
        builder.add("base-pos", Json.createObjectBuilder()
                .add("x", client.getBaseX())
                .add("y", client.getBaseY()));

        return builder.build();
    }

    private JsonArray getPlayers() {

        JsonArrayBuilder builder = Json.createArrayBuilder();
        LocatableQueryResults<Player> players = new PlayerQuery().result(client);

        for (Player player : players) {

            builder.add(Json.createObjectBuilder()
                    .add("rsn", player.getName())
                    .add("animation-id", player.getAnimation())
                    .add("interacting-name", player.getInteracting() != null ? player.getInteracting().getName() : "null")
                    .add("sceneX", player.getLocalLocation().getSceneX())
                    .add("sceneY", player.getLocalLocation().getSceneY())
                    .add("worldX", player.getWorldLocation().getX())
                    .add("worldY", player.getWorldLocation().getY())
                    .build());
        }

        return builder.build();
    }

    private JsonArray getInventory() {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        ItemContainer inventory = this.client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null)
            return builder.build();

        for (int i = 0; i < (inventory.getItems()).length; i++) {
            Item item = inventory.getItems()[i];

            if (item != null && item.getId() != -1) {

                ItemComposition def = client.getItemDefinition(item.getId());
                if (def != null && def.getName() != null) {
                    builder.add(Json.createObjectBuilder()
                            .add("name", def.getName())
                            .add("id", def.getId())
                            .add("position", i)
                            .add("quantity", item.getQuantity())
                            .build());
                }
            }
        }

        return builder.build();
    }
}
