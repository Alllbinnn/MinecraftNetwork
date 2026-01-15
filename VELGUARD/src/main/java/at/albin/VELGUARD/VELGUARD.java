package at.albin.VELGUARD;

import at.albin.VELGUARD.commands.PunishCommand;
import at.albin.VELGUARD.listener.MuteChatListener;
import at.albin.VELGUARD.rank.LuckPermsRankService;
import at.albin.VELGUARD.rank.RankManager;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;


public final class VELGUARD extends JavaPlugin {

    public static final String CHANNEL = "vellscaffolding:punishment";

    private MuteChatListener muteChatListener;
    private RankManager rankManager;
    private LuckPermsRankService rankService;


    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        rankManager = new RankManager(this);
        rankManager.load();

        // dynamische Permissions registrieren (optional, aber gewünscht)
        for (String rank : rankManager.getRanks()) {
            String node = "vellscaffolding.punishment.sgd." + rank.toLowerCase();
            if (getServer().getPluginManager().getPermission(node) == null) {
                getServer().getPluginManager().addPermission(
                        new Permission(node, PermissionDefault.FALSE)
                );
            }
        }

        rankService = new LuckPermsRankService(rankManager);

        muteChatListener = new MuteChatListener(this);
        getServer().getPluginManager().registerEvents(muteChatListener, this);

        PunishCommand executor = new PunishCommand(this, muteChatListener, rankService);

        getCommand("ban").setExecutor(executor);
        getCommand("unban").setExecutor(executor);
        getCommand("mute").setExecutor(executor);
        getCommand("unmute").setExecutor(executor);
        getCommand("kick").setExecutor(executor);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
    }

    public RankManager getRankManager() {
        return rankManager;
    }

}
