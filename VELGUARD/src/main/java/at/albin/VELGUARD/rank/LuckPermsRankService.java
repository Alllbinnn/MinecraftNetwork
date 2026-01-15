package at.albin.VELGUARD.rank;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsRankService {

    private final RankManager ranks;
    private final LuckPerms lp;

    public record CheckResult(boolean allowed, String actorRank, String targetRank) {}

    public LuckPermsRankService(RankManager ranks) {
        this.ranks = ranks;
        this.lp = LuckPermsProvider.get();
    }

    public CompletableFuture<CheckResult> canPunish(UUID actorUuid, UUID targetUuid, RankProvider actorProvider) {
        CompletableFuture<User> targetF = lp.getUserManager().loadUser(targetUuid);

        // Actor-Rang kommt von Bukkit hasPermission (online), Target von LP-User (auch offline)
        return targetF.thenApply(targetUser -> {
            String actorRank = actorProvider.getActorRank();          // aus hasPermission()
            String targetRank = resolveRankFromUser(targetUser);      // aus LP cache

            boolean ok = ranks.canPunish(actorRank, targetRank);
            return new CheckResult(ok, actorRank, targetRank);
        });
    }

    private String resolveRankFromUser(User user) {
        if (user == null) return "default";

        QueryOptions qo = lp.getContextManager().getQueryOptions(user)
                .orElse(lp.getContextManager().getStaticQueryOptions());

        for (String rank : ranks.getRanks()) {
            String node = "vellscaffolding.punishment.sgd." + rank.toLowerCase();
            if (user.getCachedData().getPermissionData(qo).checkPermission(node).asBoolean()) {
                return rank.toLowerCase();
            }
        }
        return "default";
    }

    // kleines Interface, damit wir den Actor-Rang im Command bequem liefern können
    public interface RankProvider {
        String getActorRank();
    }
}