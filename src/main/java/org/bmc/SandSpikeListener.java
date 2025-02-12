package org.bmc;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.PlayerSwingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SandSpikeListener implements Listener {
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
//        System.out.println("Player is sneaking!");

        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        SandSpike SandSpike = CoreAbility.getAbility(player, SandSpike.class);

        if (bPlayer == null) {
//            System.out.println("\tBending player doesn't exist!");
            return;
        }

        String boundAbilityName = bPlayer.getBoundAbilityName();

        if (boundAbilityName == null) {
//            System.out.println("\tNo bound ability!");
            return;
        }

        if (player.isSneaking()) {
            return;
        }

        if (boundAbilityName.equalsIgnoreCase("SandSpike") && bPlayer.canBend(CoreAbility.getAbility(SandSpike.class)) && SandSpike == null) {
//            System.out.println("\tFound sandspike ability!");
            new SandSpike(player);
        }
    }

    @EventHandler
    public void onPlayerSwing(PlayerSwingEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null) {
            return;
        }

        String boundAbilityName = bPlayer.getBoundAbilityName();

        if (boundAbilityName == null) {
            return;
        }

        if (boundAbilityName.equalsIgnoreCase("SandSpike")) {
            SandSpike.shootSpikes(player);
        }
    }
}
