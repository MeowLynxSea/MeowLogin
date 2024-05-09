package cn.meowdream.meowlogin;

import cn.meowdream.meowlogin.listeners.Captcha;
import cn.meowdream.meowlogin.listeners.Auth;
import cn.meowdream.meowlogin.listeners.PluginCommand;
import cn.meowdream.meowlogin.utils.Metrics;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private Logger logger;
    private PluginDescriptionFile descriptionFile;
    private static Main instance;

    @Override
    public void onEnable() {
        logger = getLogger();
        instance = this;
        registerConfig();
        descriptionFile = getDescription();

        Metrics metrics = new Metrics(this,21668);

        getServer().getPluginManager().registerEvents(new Auth(),this);
        getServer().getPluginManager().registerEvents(new Captcha(),this);

        getCommand("mlreload").setExecutor(new PluginCommand());
        logger.info("""
               
               
               =====================================================================
               
                      __  ___                          __                _      \s
                     /  |/  / ___  ___  _    __       / /  ___   ___ _  (_)  ___\s
                    / /|_/ / / -_)/ _ \\| |/|/ /      / /__/ _ \\ / _ `/ / /  / _ \\
                   /_/  /_/  \\__/ \\___/|__,__/      /____/\\___/ \\_, / /_/  /_//_/
                                                               /___/            \s
                            
                                        Author: Mewn_Lynsi
                                        
               =====================================================================
               """);
        logger.info("Enabled " + descriptionFile.getName() + " " + descriptionFile.getVersion() + "...");
    }

    @Override
    public void onDisable() {
        logger.info("Disabled " + descriptionFile.getName() + "...");
        logger = null;
    }

    private void registerConfig() {
        saveDefaultConfig();
    }

    public static Main getInstance(){
        return instance;
    }
}
