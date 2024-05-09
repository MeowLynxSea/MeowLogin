package cn.meowdream.meowlogin.listeners;

import cn.meowdream.meowlogin.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PluginCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command pluginCommand, String label, String[] args) {
        if (sender.hasPermission("meowlogin.reloadconfig")) {
            Main.getInstance().reloadConfig();
            sender.sendMessage("配置文件已重新加载。");
            return true;
        } else {
            sender.sendMessage("你没有权限执行此命令。");
            return false;
        }
    }
}
