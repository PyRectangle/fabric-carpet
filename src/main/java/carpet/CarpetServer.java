package carpet;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import carpet.commands.*;
import carpet.network.ServerNetworkHandler;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.network.PluginChannelManager;
import carpet.network.channels.StructureChannel;
import carpet.script.CarpetScriptServer;
import carpet.settings.SettingsManager;
import carpet.logging.HUDController;
import carpet.utils.MobAI;
import carpet.utils.ServerStatus;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import static carpet.CarpetSettings.LOG;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static final Random rand = new Random();
    public static MinecraftServer minecraft_server;
    private static CommandDispatcher<ServerCommandSource> currentCommandDispatcher;
    public static CarpetScriptServer scriptServer;
    public static SettingsManager settingsManager;
    public static PluginChannelManager pluginChannelManager;
    public static ServerStatus status;
    public static final List<CarpetExtension> extensions = new ArrayList<>();

    // Separate from onServerLoaded, because a server can be loaded multiple times in singleplayer
    public static void manageExtension(CarpetExtension extension)
    {
        extensions.add(extension);
        // for extensions that come late to the party, after server is created / loaded
        // we will handle them now.
        // that would handle all extensions, even these that add themselves really late to the party
        if (currentCommandDispatcher != null)
        {
            extension.registerCommands(currentCommandDispatcher);
        }
    }

    public static void onGameStarted()
    {
        settingsManager = new SettingsManager(CarpetSettings.carpetVersion, "carpet", "Carpet Mod");
        settingsManager.parseSettingsClass(CarpetSettings.class);
        extensions.forEach(CarpetExtension::onGameStarted);
    }

    public static void onServerLoaded(MinecraftServer server) {
        pluginChannelManager = new PluginChannelManager(server);
        pluginChannelManager.register(new StructureChannel());
        extensions.add(pluginChannelManager);

        CarpetServer.minecraft_server = server;
        settingsManager.attachServer(server);
        extensions.forEach(e -> {
            SettingsManager sm = e.customSettingsManager();
            if (sm != null) sm.attachServer(server);
            e.onServerLoaded(server);
        });
        scriptServer = new CarpetScriptServer(server);
        MobAI.resetTrackers();
        LoggerRegistry.initLoggers();
        //TickSpeed.reset();

        if (CarpetSettings.serverStatusOn) {
            try {
                status = new ServerStatus(CarpetSettings.serverStatusPort, server);
            } catch (Exception ex) {
                LOG.error("Failed to start Status server:\n" + ex.getMessage());
            }
        }
    }

    public static void onServerLoadedWorlds(MinecraftServer minecraftServer)
    {
        extensions.forEach(e -> e.onServerLoadedWorlds(minecraftServer));
        scriptServer.loadAllWorldScripts();
    }

    public static void tick(MinecraftServer server)
    {
        TickSpeed.tick(server);
        HUDController.update_hud(server);
        scriptServer.tick();
        StructureChannel.instance.tick();

        //in case something happens
        CarpetSettings.impendingFillSkipUpdates = false;
        CarpetSettings.currentTelepotingEntityBox = null;
        CarpetSettings.fixedPosition = null;

        extensions.forEach(e -> e.onTick(server));
    }

    public static void registerCarpetCommands(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        TickCommand.register(dispatcher);
        ProfileCommand.register(dispatcher);
        CounterCommand.register(dispatcher);
        LogCommand.register(dispatcher);
        SpawnCommand.register(dispatcher);
        PlayerCommand.register(dispatcher);
        CameraModeCommand.register(dispatcher);
        InfoCommand.register(dispatcher);
        DistanceCommand.register(dispatcher);
        PerimeterInfoCommand.register(dispatcher);
        DrawCommand.register(dispatcher);
        ScriptCommand.register(dispatcher);
        MobAICommand.register(dispatcher);
        BeaconGridCommand.register(dispatcher);
        StatsCommand.register(dispatcher);
        LoadedCommand.register(dispatcher);
        // registering command of extensions that has registered before either server is created
        // for all other, they will have them registered when they add themselves
        extensions.forEach(e -> e.registerCommands(dispatcher));
        currentCommandDispatcher = dispatcher;

        if (FabricLoader.getInstance().isDevelopmentEnvironment())
            TestCommand.register(dispatcher);
    }

    public static void onPlayerLoggedIn(ServerPlayerEntity player)
    {
        ServerNetworkHandler.onPlayerJoin(player);
        LoggerRegistry.playerConnected(player);
        extensions.forEach(e -> e.onPlayerLoggedIn(player));

    }

    public static void onPlayerLoggedOut(ServerPlayerEntity player)
    {
        ServerNetworkHandler.onPlayerLoggedOut(player);
        LoggerRegistry.playerDisconnected(player);
        extensions.forEach(e -> e.onPlayerLoggedOut(player));
    }

    public static void onServerClosed(MinecraftServer server)
    {
        ServerNetworkHandler.close();
        currentCommandDispatcher = null;
        if (scriptServer != null) scriptServer.onClose();

        LoggerRegistry.stopLoggers();
        extensions.forEach(e -> e.onServerClosed(server));
        minecraft_server = null;
        disconnect();
    }

    public static void registerExtensionLoggers()
    {
        extensions.forEach(CarpetExtension::registerLoggers);
    }

    public static void disconnect()
    {
        // this for whatever reason gets called multiple times even when joining;
        TickSpeed.reset();
        settingsManager.detachServer();
    }
}

