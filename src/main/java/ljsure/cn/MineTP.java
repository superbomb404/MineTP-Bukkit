package ljsure.cn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MineTP extends JavaPlugin implements TabExecutor {

    private FileConfiguration config;
    private int maxRadius = 1500; // 默认半径

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        config = getConfig();

        // 加载配置
        loadConfig();

        // 注册命令
        Objects.requireNonNull(getCommand("wtp")).setExecutor(this);
        Objects.requireNonNull(getCommand("wtp")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("killl")).setExecutor(this);
        Objects.requireNonNull(getCommand("minetp")).setExecutor(this);
        Objects.requireNonNull(getCommand("minetp")).setTabCompleter(this);

        getLogger().info("MineTP 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 确保配置已保存
        saveConfig();
        getLogger().info("MineTP 插件已禁用！");
    }

    private void loadConfig() {
        // 加载配置值，如果不存在则使用默认值
        maxRadius = config.getInt("settings.max-radius", 1500);

        // 如果配置中没有白名单，创建空列表
        if (!config.contains("whitelist")) {
            config.set("whitelist", new ArrayList<String>());
            saveConfig();
        }

        getLogger().info("配置已加载：最大半径=" + maxRadius + "，白名单数量=" + config.getStringList("whitelist").size());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String @NotNull [] args) {
        if (cmd.getName().equalsIgnoreCase("wtp")) {
            return handlewtp(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("killl")) {
            return handleKill(sender);
        } else if (cmd.getName().equalsIgnoreCase("minetp")) {
            return handleMineTP(sender, args);
        }
        return false;
    }

    private boolean handlewtp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        // 检查白名单
        if (!isInWhitelist(player.getName())) {
            player.sendMessage("§c你不在白名单中，无法使用传送功能！");
            return true;
        }

        // 移除了权限检查，所有玩家都可以使用（只要在白名单中）
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

                if (player.getLocation().distanceSquared(dest) > maxRadius * maxRadius) {
                    player.sendMessage("§c目标坐标超出 " + maxRadius + " 格范围！");
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

        player.sendMessage("§c用法: /wtp <玩家> 或 /wtp <x> <y> <z>");
        return false;
    }

    private boolean handleKill(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        // 移除了权限检查，所有玩家都可以使用
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill " + player.getName());
        player.sendMessage("§e你已自杀！");
        sendAdminNotification(player, "kill", null);
        return true;
    }

    private boolean handleMineTP(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6=== MineTP 管理命令 ===");
            sender.sendMessage("§a/minetp add <玩家名> §7- 添加玩家到白名单");
            sender.sendMessage("§a/minetp remove <玩家名> §7- 从白名单移除玩家");
            sender.sendMessage("§a/minetp list §7- 查看白名单列表");
            sender.sendMessage("§a/minetp radius <数值> §7- 设置传送最大半径");
            sender.sendMessage("§a/minetp reload §7- 重载配置");
            sender.sendMessage("§a/minetp info §7- 查看当前配置信息");
            return true;
        }

        if (!sender.hasPermission("minetp.admin")) {
            sender.sendMessage("§c你没有管理权限！");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                return handleAddWhitelist(sender, args);
            case "remove":
                return handleRemoveWhitelist(sender, args);
            case "list":
                return handleListWhitelist(sender);
            case "radius":
                return handleSetRadius(sender, args);
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            default:
                sender.sendMessage("§c未知子命令！");
                return false;
        }
    }

    private boolean handleAddWhitelist(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c用法: /minetp add <玩家名>");
            return false;
        }

        String playerName = args[1];
        if (isInWhitelist(playerName)) {
            sender.sendMessage("§c玩家 " + playerName + " 已在白名单中！");
            return true;
        }

        List<String> whitelist = config.getStringList("whitelist");
        whitelist.add(playerName.toLowerCase());
        config.set("whitelist", whitelist);
        saveConfig(); // 立即保存配置

        sender.sendMessage("§a已添加玩家 " + playerName + " 到白名单");
        getLogger().info(sender.getName() + " 添加玩家 " + playerName + " 到白名单");
        return true;
    }

    private boolean handleRemoveWhitelist(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c用法: /minetp remove <玩家名>");
            return false;
        }

        String playerName = args[1];
        if (!isInWhitelist(playerName)) {
            sender.sendMessage("§c玩家 " + playerName + " 不在白名单中！");
            return true;
        }

        List<String> whitelist = config.getStringList("whitelist");
        whitelist.remove(playerName.toLowerCase());
        config.set("whitelist", whitelist);
        saveConfig(); // 立即保存配置

        sender.sendMessage("§a已从白名单移除玩家 " + playerName);
        getLogger().info(sender.getName() + " 从白名单移除玩家 " + playerName);
        return true;
    }

    private boolean handleListWhitelist(CommandSender sender) {
        List<String> whitelist = config.getStringList("whitelist");
        if (whitelist.isEmpty()) {
            sender.sendMessage("§e白名单为空");
        } else {
            sender.sendMessage("§6=== 白名单列表 (" + whitelist.size() + "人) ===");
            for (String name : whitelist) {
                sender.sendMessage("§7- " + name);
            }
        }
        return true;
    }

    private boolean handleSetRadius(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c用法: /minetp radius <数值>");
            return false;
        }

        try {
            int radius = Integer.parseInt(args[1]);
            if (radius <= 0) {
                sender.sendMessage("§c半径必须为正数！");
                return false;
            }

            maxRadius = radius;
            config.set("settings.max-radius", radius);
            saveConfig(); // 立即保存配置

            sender.sendMessage("§a已设置传送最大半径为 " + radius + " 格");
            getLogger().info(sender.getName() + " 设置传送半径为 " + radius);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的数值格式！");
            return false;
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        reloadConfig();
        config = getConfig();
        loadConfig();
        sender.sendMessage("§a配置已重载！");
        getLogger().info(sender.getName() + " 重载了配置");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        List<String> whitelist = config.getStringList("whitelist");
        sender.sendMessage("§6=== MineTP 配置信息 ===");
        sender.sendMessage("§7最大传送半径: §a" + maxRadius + " 格");
        sender.sendMessage("§7白名单人数: §a" + whitelist.size() + " 人");
        sender.sendMessage("§7配置文件: §aplugins/MineTP/config.yml");
        return true;
    }

    private boolean isInWhitelist(String playerName) {
        List<String> whitelist = config.getStringList("whitelist");
        return whitelist.contains(playerName.toLowerCase());
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
        if (cmd.getName().equalsIgnoreCase("wtp") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        } else if (cmd.getName().equalsIgnoreCase("minetp")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                String partial = args[0].toLowerCase();
                for (String subCmd : new String[]{"add", "remove", "list", "radius", "reload", "info"}) {
                    if (subCmd.startsWith(partial)) {
                        completions.add(subCmd);
                    }
                }
                return completions;
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}