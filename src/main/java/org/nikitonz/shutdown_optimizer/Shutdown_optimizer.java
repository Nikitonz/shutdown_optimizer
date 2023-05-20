package org.nikitonz.shutdown_optimizer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Shutdown_optimizer extends JavaPlugin implements Listener, CommandExecutor {
    private int countdownTask;
    private int playerCount;
    private int shutdownTime;
    private FileConfiguration config;
    private File configFile;

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shutdown").setExecutor(this);
        getCommand("makeyourtime").setExecutor(this);
        loadConfig();
        startCountdown();
        getLogger().info("\u001b[34m" + "Plugin enabled" +"\u001B[0m");
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("\u001b[31m" + "Plugin disabled" +"\u001B[0m");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shutdown")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("now")) {

                    shutdownNow();
                } else {
                    try {

                        int shutdownDelay = Integer.parseInt(args[0]);
                        scheduleShutdown(shutdownDelay);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid argument. Please use a numeric value.");
                    }
                }
            } else {
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("makeyourtime")) {
            if (args.length == 1) {
                try {

                    int newShutdownTime = Integer.parseInt(args[0]);
                    setShutdownTime(newShutdownTime);
                    sender.sendMessage(ChatColor.GREEN + "Shutdown time has been updated to " + newShutdownTime + " minutes.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid argument. Please use a numeric value.");
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerCount = Bukkit.getServer().getOnlinePlayers().size();
        if (playerCount > 0) {
            resetCountdown();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerCount = Bukkit.getServer().getOnlinePlayers().size() - 1;
        if (playerCount == 0) {
            startCountdown();
        }
    }

    private void startCountdown() {
        cancelCountdown();
        countdownTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "save-all");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
            }, 10 * 20);
        }, shutdownTime * 60 * 20);
    }

    private void resetCountdown() {
        cancelCountdown();
        startCountdown();
    }

    private void cancelCountdown() {
        if (countdownTask != -1) {
            Bukkit.getScheduler().cancelTask(countdownTask);
            countdownTask = -1;
        }
    }

    private void shutdownNow() {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "save-all");
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
    }

    private void scheduleShutdown(int delay) {
        int shutdownDelayTicks = delay * 60 * 20;
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, this::shutdownNow, shutdownDelayTicks);
    }

    private void setShutdownTime(int newShutdownTime) {
        shutdownTime = newShutdownTime;
        config.set("shutdown-time", shutdownTime);
        saveConfig();
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        shutdownTime = config.getInt("shutdown-time", 10);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().warning(ChatColor.RED + "Failed to save config.yml!");
        }
    }
}