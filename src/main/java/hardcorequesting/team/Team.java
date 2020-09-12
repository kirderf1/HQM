package hardcorequesting.team;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import hardcorequesting.HardcoreQuesting;
import hardcorequesting.io.SaveHandler;
import hardcorequesting.io.adapter.TeamAdapter;
import hardcorequesting.network.NetworkManager;
import hardcorequesting.network.message.TeamMessage;
import hardcorequesting.quests.*;
import hardcorequesting.quests.reward.ReputationReward;
import hardcorequesting.reputation.Reputation;
import hardcorequesting.reputation.ReputationManager;
import net.fabricmc.api.EnvType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Team {
    public static boolean reloadedInvites;
    private int id = -1;
    private List<PlayerEntry> players = new ArrayList<>();
    private List<Team> invites;
    private String name;
    private Map<String, Integer> reputation;
    private Map<UUID, QuestData> questData;
    private int clientTeamLives = -1;
    private RewardSetting rewardSetting = RewardSetting.getDefault();
    private LifeSetting lifeSetting = LifeSetting.SHARE;
    
    public Team(UUID uuid) {
        if (uuid != null)
            players.add(new PlayerEntry(uuid, true, true));
        createQuestData();
        createReputation();
        this.invites = new ArrayList<>();
    }
    
    public static class Manager extends SimpleSerializable {
        public Manager(QuestLine parent) {
            super(parent);
        }
        
        @Override
        public String filePath() {
            return "teams.json";
        }
        
        @Override
        public boolean isData() {
            return true;
        }
        
        @Override
        public void loadFromString(Optional<String> string) {
            QuestingDataManager questingDataManager = QuestingDataManager.getInstance();
            questingDataManager.getTeams().clear();
            TeamAdapter.clearInvitesMap();
            List<Team> teams = string
                    .flatMap(s -> SaveHandler.<List<Team>>load(s, new TypeToken<List<Team>>() {}.getType()))
                    .orElseGet(Lists::newArrayList);
            for (int i = 0; i < teams.size(); i++)
                questingDataManager.getTeams().add(null);
            teams.stream().filter(team -> !team.isSingle()).forEach(team -> questingDataManager.getTeams().set(team.getId(), team));
            TeamAdapter.commitInvitesMap();
            if (HardcoreQuesting.LOADING_SIDE == EnvType.CLIENT)
                TeamLiteStat.updateTeams(questingDataManager.getTeams().stream().map(Team::toLiteStat).collect(Collectors.toList()));
        }
        
        @Override
        public String saveToString() {
            return SaveHandler.save(QuestingDataManager.getInstance().getTeams(), new TypeToken<List<Team>>() {}.getType());
        }
        
        public String saveToString(Player player) {
            Team team = QuestingDataManager.getInstance().getQuestingData(player).getTeam();
            if (team.isSingle()) return "[]";
            return "[" + SaveHandler.save(team, Team.class) + "]";
        }
    }
    
    public static void declineAll(UUID playerID) {
        for (Team team : QuestingDataManager.getInstance().getTeams()) {
            for (Iterator<PlayerEntry> iterator = team.getPlayers().iterator(); iterator.hasNext(); ) {
                PlayerEntry playerEntry = iterator.next();
                if (!playerEntry.isInTeam() && playerEntry.getUUID().equals(playerID)) {
                    iterator.remove();
                    team.refreshTeamData(TeamUpdateSize.ONLY_OWNER);
                    break;
                }
            }
        }
    }
    
    public void resetProgress(Quest quest) {
        questData.put(quest.getQuestId(), Quest.getQuest(quest.getQuestId()).createData(getPlayerCount()));
        refreshData();
    }
    
    public void resetCompletion(Quest quest) {
        QuestData data = getQuestData(quest.getQuestId());
        data.completed = false;
        data.claimed = false;
        data.available = true;
        refreshData();
    }
    
    public Map<UUID, QuestData> getQuestData() {
        return this.questData;
    }
    
    public TeamLiteStat toLiteStat() {
        return new TeamLiteStat(name, getPlayerCount(), getSharedLives(), getProgress());
    }
    
    public float getProgress() {
        int completed = 0;
        int total = 0;
        for (QuestData data : questData.values()) {
            if (data != null) {
                total++;
                if (data.completed) completed++;
            }
        }
        return (float) completed / total;
    }
    
    public void receiveAndSyncReputation(Quest quest, List<ReputationReward> reputationList) {
        for (ReputationReward reputationReward : reputationList)
            setReputation(reputationReward.getReward(), getReputation(reputationReward.getReward()) + reputationReward.getValue());
        
        for (PlayerEntry entry : getPlayers())
            if (entry.isInTeam())
                NetworkManager.sendToPlayer(TeamUpdateType.REPUTATION_RECEIVED.build(this, quest, reputationList), entry.getPlayerMP());
    }
    
    public RewardSetting getRewardSetting() {
        return rewardSetting;
    }
    
    public void setRewardSetting(RewardSetting rewardSetting) {
        this.rewardSetting = rewardSetting;
    }
    
    public LifeSetting getLifeSetting() {
        return lifeSetting;
    }
    
    public void setLifeSetting(LifeSetting lifeSetting) {
        this.lifeSetting = lifeSetting;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public boolean isSharingLives() {
        return lifeSetting == LifeSetting.SHARE;
    }
    
    public int getSharedLives() {
        if (clientTeamLives != -1) {
            return clientTeamLives;
        }
        
        int lives = 0;
        for (PlayerEntry entry : getPlayers()) {
            if (entry.isInTeam()) {
                lives += QuestingDataManager.getInstance().getQuestingData(entry.getUUID()).getRawLives();
            }
        }
        return lives;
    }
    
    public int getPlayerCount() {
        return (int) players.stream().filter(PlayerEntry::isInTeam).count();
    }
    
    public void addPlayer(PlayerEntry entry) {
        players.add(entry);
    }
    
    public void removePlayer(Player player) {
        removePlayer(player.getUUID());
    }
    
    public void removePlayer(UUID uuid) {
        int id = 0;
        for (PlayerEntry player : players) {
            if (player.isInTeam()) {
                if (player.getUUID().equals(uuid)) {
                    Team leaveTeam = new Team(uuid);
                    for (UUID i : questData.keySet()) {
                        QuestData leaveData = leaveTeam.questData.get(i);
                        QuestData data = questData.get(i);
                        if (data != null) {
                            boolean[] old = data.reward;
                            data.reward = new boolean[old.length - 1];
                            for (int j = 0; j < data.reward.length; j++) {
                                if (j < id) {
                                    data.reward[j] = old[j];
                                } else {
                                    data.reward[j] = old[j + 1];
                                }
                            }
                            
                            leaveData.reward[0] = old[id];
                        }
                    }
                    
                    players.remove(id);
                    
                    for (UUID i : questData.keySet()) {
                        QuestData leaveData = leaveTeam.questData.get(i);
                        QuestData data = questData.get(i);
                        if (data != null && Quest.getQuest(i) != null) {
                            Quest.getQuest(i).copyProgress(leaveData, data);
                        }
                    }
                    
                    ReputationManager reputationManager = ReputationManager.getInstance();
                    for (String i : reputationManager.getReputations().keySet()) {
                        Reputation reputation = reputationManager.getReputation(i);
                        if (reputation != null) {
                            leaveTeam.setReputation(reputation, this.getReputation(reputation));
                        }
                    }
                    
                    QuestingDataManager.getInstance().getQuestingData(uuid).setTeam(leaveTeam);
                    refreshTeamData(player, TeamUpdateSize.ONLY_MEMBERS);
                    leaveTeam.refreshTeamData(player, TeamUpdateSize.ONLY_MEMBERS);
                    NetworkManager.sendToPlayer(TeamUpdateType.LEAVE_TEAM.build(this, player.getUUID(), leaveTeam), player.getPlayerMP());
                    break;
                }
                id++;
            }
        }
        
        
    }
    
    public void refreshTeamData(TeamUpdateSize type) {
        for (PlayerEntry entry : getPlayers())
            refreshTeamData(entry, type);
    }
    
    private void refreshTeamData(PlayerEntry entry, TeamUpdateSize type) {
        boolean valid = false;
        switch (type) {
            case ALL:
                valid = true;
                break;
            case ONLY_MEMBERS:
                valid = entry.isInTeam();
                break;
            case ONLY_OWNER:
                valid = entry.isOwner();
                break;
        }
        
        if (valid) {
            NetworkManager.sendToPlayer(TeamUpdateType.FULL.build(this), entry.getPlayerMP());
        }
    }
    
    public void refreshTeamLives() {
        if (!isSingle() && isSharingLives()) {
            for (PlayerEntry entry : getPlayers()) {
                NetworkManager.sendToPlayer(TeamUpdateType.LIVES.build(this), entry.getPlayerMP());
            }
        }
    }
    
    public void refreshData() {
        for (PlayerEntry entry : getPlayers())
            refreshTeamData(entry, TeamUpdateSize.ALL);
    }
    
    public void clearProgress() {
        questData.clear();
        createQuestData();
        int playerCount = getPlayerCount();
        for (Quest quest : Quest.getQuests().values()) {
            if (quest != null && questData.get(quest.getQuestId()) != null) {
                quest.initRewards(playerCount, questData.get(quest.getQuestId()));
            }
        }
        refreshData();
    }
    
    public void deleteTeam() {
        for (int i = players.size() - 1; i >= 0; i--) {
            PlayerEntry player = players.get(i);
            if (player.isInTeam()) {
                removePlayer(player.getUUID());
            } else {
                players.remove(i);
            }
        }
        
        QuestingDataManager questingDataManager = QuestingDataManager.getInstance();
        List<Team> teams = questingDataManager.getTeams();
        teams.remove(id);
        
        for (int i = id; i < teams.size(); i++) {
            Team team = teams.get(i);
            team.id--;
        }
        
        NetworkManager.sendToAllPlayers(TeamUpdateType.REMOVE_TEAM.build(this));
        
        //refresh all clients with open books
        for (Player player : HardcoreQuesting.getServer().getPlayerList().getPlayers()) {
            Team team = questingDataManager.getQuestingData(player).getTeam();
            PlayerEntry entry = team.getEntry(player.getUUID());
            if (entry != null) {
                team.refreshTeamData(entry, TeamUpdateSize.ALL);
            }
        }
    }
    
    public List<Team> getInvites() {
        return invites;
    }
    
    public List<PlayerEntry> getPlayers() {
        return players;
    }
    
    public void create(String name) {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.CREATE, name));
    }
    
    public void invite(String name) {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.INVITE, name));
    }
    
    public void accept() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.ACCEPT, "" + id));
    }
    
    public void decline() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.DECLINE, "" + id));
    }
    
    public void kick(UUID playerID) {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.KICK, playerID.toString()));
    }
    
    public void leave() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.LEAVE, ""));
    }
    
    public void disband() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.DISBAND, ""));
    }
    
    public void nextLifeSetting() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.NEXT_LIFE_SETTING, ""));
    }
    
    public void nextRewardSetting() {
        NetworkManager.sendToServer(new TeamMessage(TeamAction.NEXT_REWARD_SETTING, ""));
    }
    
    public boolean isOwner(@NotNull Player player) {
        return isOwner(player.getUUID());
    }
    
    public boolean isOwner(@Nullable UUID uuid) {
        PlayerEntry entry = getEntry(uuid);
        return entry != null && entry.isOwner();
    }
    
    @Nullable
    public PlayerEntry getEntry(@Nullable UUID uuid) {
        for (PlayerEntry playerEntry : getPlayers()) {
            if (playerEntry.getUUID().equals(uuid)) {
                return playerEntry;
            }
        }
        return null;
    }
    
    private void createReputation() {
        reputation = new HashMap<>();
        for (Reputation reputation : ReputationManager.getInstance().getReputations().values())
            createReputation(reputation.getId());
    }
    
    public int getReputation(Reputation reputation) {
        return getReputation(reputation.getId());
    }
    
    public void setReputation(Reputation reputation, int value) {
        setReputation(reputation.getId(), value);
    }
    
    private void createReputation(String id) {
        reputation.put(id, 0);
    }
    
    public int getReputation(String id) {
        Integer value = reputation.get(id);
        return value == null ? 0 : value;
    }
    
    public void setReputation(String id, Integer value) {
        reputation.put(id, value);
    }
    
    private void createQuestData() {
        questData = new HashMap<>();
        Quest.getQuests().keySet().forEach(this::createQuestData);
    }
    
    private void createQuestData(UUID questId) {
        this.questData.put(questId, Quest.getQuest(questId).createData(1));
    }
    
    public QuestData getQuestData(UUID questId) {
        if (!this.questData.containsKey(questId)) {
            createQuestData(questId);
        }
        return this.questData.get(questId);
    }
    
    public void setQuestData(UUID questId, QuestData data) {
        questData.put(questId, data);
    }
    
    public boolean isSingle() {
        return id == -1;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setClientTeamLives(int lives) {
        this.clientTeamLives = lives;
    }
    
    public void update(Team team) {
        this.name = team.name;
        this.questData = team.questData;
        this.reputation = team.reputation;
        this.lifeSetting = team.lifeSetting;
        this.rewardSetting = team.rewardSetting;
        this.players = team.players;
    }
    
    public static String saveTeam(Player entity) {
        Team team = QuestingDataManager.getInstance().getQuestingData(entity).getTeam();
        if (team.isSingle()) return ""; // return an empty string when the team is single
        return SaveHandler.saveTeam(team);
    }
}
