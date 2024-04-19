package talonos.biomescanner.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelHandler;
import talonos.biomescanner.BiomeScanner;
import talonos.biomescanner.map.MapScanner;
import talonos.biomescanner.map.event.UpdateMapEvent;

@ChannelHandler.Sharable
public class BiomeScannerNetwork {

    private static final BiomeScannerNetwork INSTANCE = new BiomeScannerNetwork();
    private SimpleNetworkWrapper networkWrapper;

    public static void init() {
        INSTANCE.networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(BiomeScanner.MODID);
        INSTANCE.networkWrapper.registerMessage(UpdateClientMapHandler.class, UpdateMapPacket.class, 1, Side.CLIENT);

        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer()) {
            MapScanner.instance.bus()
                .register(INSTANCE);
        }

        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onUpdateMap(UpdateMapEvent event) {
        int minX = event.getX();
        int minY = event.getY();
        int width = event.getWidth();
        int height = event.getHeight();

        byte[] data = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[(y * width) + x] = MapScanner.instance.getRawColorByte(x + minX, y + minY);
            }
        }
        sendToAllPlayers(new UpdateMapPacket(minX, minY, width, height, data));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            MapScanner.instance.sendEntireMap((EntityPlayerMP) event.player);
        }
    }

    public static void sendToAllPlayers(IMessage packet) {
        INSTANCE.networkWrapper.sendToAll(packet);
    }

    public static void sendToPlayer(IMessage message, EntityPlayerMP player) {
        INSTANCE.networkWrapper.sendTo(message, player);
    }
}
