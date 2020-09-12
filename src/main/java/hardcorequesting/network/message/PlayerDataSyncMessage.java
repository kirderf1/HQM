package hardcorequesting.network.message;

import hardcorequesting.network.IMessage;
import hardcorequesting.network.IMessageHandler;
import hardcorequesting.quests.QuestLine;
import hardcorequesting.quests.QuestingDataManager;
import hardcorequesting.util.SyncUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class PlayerDataSyncMessage implements IMessage {
    
    private boolean local, remote, questing, hardcore;
    private String teams;
    private String data;
    private String deaths;
    
    public PlayerDataSyncMessage() {
    }
    
    public PlayerDataSyncMessage(QuestLine questLine, boolean local, boolean remote, Player player) {
        this.local = local;
        this.remote = remote;
        this.questing = questLine.questingDataManager.isQuestActive();
        this.hardcore = questLine.questingDataManager.isHardcoreActive();
        this.teams = questLine.teamManager.saveToString(player);
        this.deaths = questLine.deathStatsManager.saveToString();
        this.data = questLine.questingDataManager.data.saveToString(player);
    }
    
    @Override
    public void fromBytes(FriendlyByteBuf buf, PacketContext context) {
        this.local = buf.readBoolean();
        this.remote = buf.readBoolean();
        this.questing = buf.readBoolean();
        this.hardcore = buf.readBoolean();
        this.teams = SyncUtil.readLargeString(buf);
        this.deaths = SyncUtil.readLargeString(buf);
        this.data = SyncUtil.readLargeString(buf);
    }
    
    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.local);
        buf.writeBoolean(this.remote);
        buf.writeBoolean(this.questing);
        buf.writeBoolean(this.hardcore);
        SyncUtil.writeLargeString(this.teams, buf);
        SyncUtil.writeLargeString(this.deaths, buf);
        SyncUtil.writeLargeString(this.data, buf);
    }
    
    public static class Handler implements IMessageHandler<PlayerDataSyncMessage, IMessage> {
        
        @Environment(EnvType.CLIENT)
        @Override
        public IMessage onMessage(PlayerDataSyncMessage message, PacketContext ctx) {
            ctx.getTaskQueue().execute(() -> handle(message, ctx));
            return null;
        }
        
        @Environment(EnvType.CLIENT)
        private void handle(PlayerDataSyncMessage message, PacketContext ctx) {
            QuestLine.receiveDataFromServer(Minecraft.getInstance().player, message.remote);
            QuestLine questLine = QuestLine.getActiveQuestLine();
            questLine.provideTemp(questLine.teamManager, message.teams);
            questLine.provideTemp(questLine.questingDataManager.data, message.data);
            questLine.provideTemp(questLine.questingDataManager.state, QuestingDataManager.saveQuestingState(message.questing, message.hardcore));
            questLine.provideTemp(questLine.deathStatsManager, message.deaths);
            questLine.loadAll();
        }
    }
}
