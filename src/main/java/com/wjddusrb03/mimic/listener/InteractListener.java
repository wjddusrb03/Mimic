package com.wjddusrb03.mimic.listener;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import com.wjddusrb03.mimic.mimic.MimicState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Iterator;

/**
 * 플레이어 상호작용으로 미믹이 트리거되는 이벤트를 처리한다.
 */
public class InteractListener implements Listener {

    private final Mimic plugin;

    public InteractListener(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 우클릭으로 블록을 열면 미믹 트리거.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        Player player = e.getPlayer();

        // 크리에이티브 모드 체크
        if (player.getGameMode() == GameMode.CREATIVE
                && !plugin.getConfig().getBoolean("trigger-in-creative", false)) {
            return;
        }

        MimicEntity mimic = plugin.getMimicManager().getMimicAt(
                block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (mimic == null) return;

        // 이벤트 취소 (상자 열기 등 방지)
        e.setCancelled(true);

        // 미믹 트리거 (정상 트리거, 선제 공격 아님)
        mimic.trigger(player, plugin, false);
    }

    /**
     * 좌클릭(블록 파괴)으로 미믹을 때리면 선제 공격으로 트리거.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        // 크리에이티브 모드 체크
        if (player.getGameMode() == GameMode.CREATIVE
                && !plugin.getConfig().getBoolean("trigger-in-creative", false)) {
            return;
        }

        MimicEntity mimic = plugin.getMimicManager().getMimicAt(
                block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (mimic == null) return;

        // 블록 파괴 취소 (미믹이 직접 블록을 제거함)
        e.setCancelled(true);

        // 선제 공격으로 트리거
        mimic.trigger(player, plugin, true);
    }

    /**
     * 미믹 블록 위치의 인벤토리가 열리는 것을 방지한다.
     * (상자/배럴 등 컨테이너 블록 미믹)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        // InventoryHolder가 블록인지 확인
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof org.bukkit.block.Container container)) return;

        Block block = container.getBlock();
        MimicEntity mimic = plugin.getMimicManager().getMimicAt(
                block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (mimic == null) return;

        // 인벤토리 열기 취소
        e.setCancelled(true);
    }

    /**
     * 피스톤이 미믹 블록을 밀어내지 못하게 한다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            MimicEntity mimic = plugin.getMimicManager().getMimicAt(
                    block.getWorld(), block.getX(), block.getY(), block.getZ());
            if (mimic != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 폭발로 미믹 블록이 파괴되면 자동 트리거한다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            MimicEntity mimic = plugin.getMimicManager().getMimicAt(
                    block.getWorld(), block.getX(), block.getY(), block.getZ());
            if (mimic == null) continue;

            // 폭발 목록에서 미믹 블록 제거 (미믹이 직접 처리)
            it.remove();

            // 근처 플레이어를 찾아서 트리거
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : block.getWorld().getPlayers()) {
                double dist = p.getLocation().distance(block.getLocation().add(0.5, 0.5, 0.5));
                if (dist < nearestDist && dist < 32) {
                    nearest = p;
                    nearestDist = dist;
                }
            }

            if (nearest != null) {
                mimic.trigger(nearest, plugin, false);
            }
        }
    }
}
