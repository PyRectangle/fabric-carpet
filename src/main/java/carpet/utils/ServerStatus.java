package carpet.utils;

import carpet.patches.EntityPlayerMPFake;
import carpet.CarpetSettings;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerStatus extends NanoHTTPD {

    public static final String MIME_JSON = "application/json";
    public static final String CARPET_HEADER = "x-carpet";

    class PlayerStatus {
        public String Name;
        public String UUID;
        public double X;
        public double Y;
        public double Z;
        public float Health;
        public String Dimension;
        public boolean IsBot;
    }

    class ServerStatusResponse {
        public int Online;
        public long StartTime;
        public List<PlayerStatus> Players;
    }

    private MinecraftServer server;

    public ServerStatus(int port, MinecraftServer server) throws IOException {
        super(port);

        this.server = server;
        start();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String key = session.getHeaders().get(CARPET_HEADER);
        if (null == key || !key.equals(CarpetSettings.serverStatusSecret) || session.getMethod() != Method.GET) {
            return newFixedLengthResponse("");
        }

        List<PlayerStatus> players = new ArrayList<>();

        for (ServerPlayerEntity p :
                server.getPlayerManager().getPlayerList()) {
            PlayerStatus pl = new PlayerStatus();
            pl.Name = p.getName().getString();
            pl.UUID = p.getUuid().toString();
            pl.X = p.x;
            pl.Y = p.y;
            pl.Z = p.z;
            pl.Dimension = p.dimension.toString();
            pl.Health = p.getHealth();
            pl.IsBot = server.getPlayerManager().getPlayer(p.getName().getString()) instanceof EntityPlayerMPFake;

            players.add(pl);
        }

        ServerStatusResponse r = new ServerStatusResponse();
        r.Online = server.getPlayerManager().getCurrentPlayerCount();
        r.Players = players;

        r.StartTime = server.getServerStartTime();

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Gson().toJson(r));
    }
}
