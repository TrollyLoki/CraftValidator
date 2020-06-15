package net.trollyloki.craftvalidator;

import org.bukkit.plugin.java.JavaPlugin;

public class CraftValidator extends JavaPlugin {

    private static CraftValidator instance;

    @Override
    public void onEnable() {

        instance = this;

        getServer().getPluginManager().registerEvents(new DetectionListener(), this);

    }

    @Override
    public void onDisable() {

        instance = null;

    }

    public static CraftValidator getInstance() {
        return instance;
    }

}
