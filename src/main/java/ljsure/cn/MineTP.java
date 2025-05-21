package ljsure.cn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MineTP extends JavaPlugin implements TabExecutor {

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("tpw")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpw")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("killl")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String @NotNull [] args) {
        if (cmd.getName().equalsIgnoreCase("tpw")) {
            return handleTPW(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("killl")) {
            return handleKill(sender);
        }
        return false;
    }

    private boolean handleTPW(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (!player.hasPermission("minetp.tpw")) {
            player.sendMessage("§c你没有使用权限！");
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§c找不到玩家 " + args[0]);
                return true;
            }
            player.teleport(target);
            player.sendMessage("§a已传送到玩家 " + target.getName());
            sendAdminNotification(player, "player", target.getName());
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Location dest = new Location(player.getWorld(), x, y, z);

                if (player.getLocation().distanceSquared(dest) > 1500 * 1500) {
                    player.sendMessage("§c目标坐标超出1500格范围！");
                    return true;
                }

                player.teleport(dest);
                player.sendMessage(String.format("§a已传送到坐标 %.1f, %.1f, %.1f", x, y, z));
                sendAdminNotification(player, "location", dest);
            } catch (NumberFormatException e) {
                player.sendMessage("§c无效的坐标格式！");
            }
            return true;
        }

        player.sendMessage("§c用法: /tpw <玩家> 或 /tpw <x> <y> <z>");
        return false;
    }

    private boolean handleKill(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (!player.hasPermission("minetp.killl")) {
            player.sendMessage("§c你没有使用权限！");
            return true;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill " + player.getName());
        player.sendMessage("§e你已自杀！");
        sendAdminNotification(player, "kill", null);
        return true;
    }

    private void sendAdminNotification(Player executor, String type, Object data) {
        String message;
        switch (type) {
            case "player":
                String targetName = (String) data;
                message = String.format("§7 %s 传送到玩家 %s", executor.getName(), targetName);
                break;
            case "location":
                Location loc = (Location) data;
                message = String.format("§7 %s 传送到坐标 %.1f, %.1f, %.1f",
                        executor.getName(), loc.getX(), loc.getY(), loc.getZ());
                break;
            case "kill":
                message = String.format("§7 %s 自杀了", executor.getName());
                break;
            default:
                return;
        }

        // 发送给所有有权限的管理员
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("minetp.notify")) {
                admin.sendMessage(message);
            }
        }
        // 同时发送到控制台
        Bukkit.getConsoleSender().sendMessage(message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command cmd, @NotNull String alias, String @NotNull [] args) {
        if (cmd.getName().equalsIgnoreCase("tpw") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}