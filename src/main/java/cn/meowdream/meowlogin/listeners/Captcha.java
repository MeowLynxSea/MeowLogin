package cn.meowdream.meowlogin.listeners;

import cn.meowdream.meowlogin.Main;
import cn.meowdream.meowlogin.utils.FancyCaptchaGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static cn.meowdream.meowlogin.listeners.Auth.getCurrentDateTime;
import static org.bukkit.Bukkit.getLogger;

public class Captcha implements Listener {

    private final HashMap<UUID, ItemStack> playerItems = new HashMap<>();
    private final HashMap<UUID, MapView> playerMaps = new HashMap<>();
    private final HashMap<UUID, String[]> playerCaptcha = new HashMap<>();
    private final HashMap<UUID, Boolean> playerVerification = new HashMap<>();
    // 用于存储玩家的BossBar
    private final HashMap<UUID, BukkitTask> countdownTasks = new HashMap<>(); // 用于存储倒计时任务
    private final HashMap<UUID, BossBar> playerBossBars = new HashMap<>();
    private final File dataFile = new File("plugins/MeowLogin/data.yml");
    private final FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    FileConfiguration config = Main.getInstance().getConfig();

    private final int COUNTDOWN_DURATION = config.getInt("captcha-countdown-duration");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(!config.getBoolean("enable-captcha")){
            playerVerification.put(player.getUniqueId(), true);
            player.sendMessage("§a亲爱的" + player.getDisplayName() + "，欢迎来到" + config.getString("server-name") + "！");
            return;
        }

        player.setInvulnerable(true);

        if (playerVerification.containsKey(player.getUniqueId())) {
            if (playerVerification.get(player.getUniqueId())) {
                return;
            } else {
                playerVerification.put(player.getUniqueId(), false);
                player.sendMessage(config.getString("captcha-needed"));
            }
        } else {
            playerVerification.put(player.getUniqueId(), false);
            player.sendMessage(config.getString("captcha-needed"));
        }

        // 使用BukkitScheduler安排一个同步任务来应用效果
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // 赋予失明效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

            // 赋予夜视效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
        });

        player.sendTitle(ChatColor.BLUE + "风控系统", ChatColor.WHITE + "请在聊天框中输入验证码", 10, 70, 20);

        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查是否手持物品
        if (!isCustomMap(item)) {
            // 存储玩家手持的物品
            playerItems.put(player.getUniqueId(), item);

            // 删除玩家手持的物品
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

            item = new ItemStack(Material.FILLED_MAP, 1);
            MapMeta meta = (MapMeta) item.getItemMeta();

            // 创建新地图并添加到玩家手中
            MapView mapView = null;
            if (meta.hasMapView()) {
                if (meta != null) {
                    mapView = meta.getMapView();
                }
            } else {
                mapView = Bukkit.createMap(player.getWorld());
            }

            if (mapView != null) {
                mapView.getRenderers().clear(); // 清除默认渲染器
                mapView.addRenderer(new MapRenderer() {
                    @Override
                    public void render(MapView map, MapCanvas canvas, Player player) {
                        String base64Image;
                        String[] captcha;
                        if (playerCaptcha.containsKey(player.getUniqueId())) {
                            captcha = playerCaptcha.get(player.getUniqueId());
                        } else {
                            FancyCaptchaGenerator fancyCaptchaGenerator = new FancyCaptchaGenerator();
                            captcha = fancyCaptchaGenerator.generateCaptcha();
                            playerCaptcha.put(player.getUniqueId(), captcha);
                        }
                        base64Image = captcha[0];
                        // 将BASE64编码的图片渲染到地图上
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                            BufferedImage image = ImageIO.read(bis);

                            // 确保图片大小符合要求 (128x128)
                            if (image.getWidth() != 128 || image.getHeight() != 128) {
                                getLogger().warning("Image dimensions are not 128x128 pixels.");
                                return;
                            }

                            // 将图片像素绘制到地图画布上
                            for (int y = 0; y < 128; y++) {
                                for (int x = 0; x < 128; x++) {
                                    int colorValue = image.getRGB(x, y);
                                    Color color = new Color(colorValue, true); // 使用 true 表示保留 alpha 通道
                                    canvas.setPixel(x, y, MapPalette.matchColor(color));
                                }
                            }
                        } catch (IOException e) {
                            getLogger().warning("Error decoding BASE64 image: " + e.getMessage());
                        }
                        // 在地图上添加文字
                        canvas.drawText(10, 10, MinecraftFont.Font, "MeowCaptcha");
                    }
                });
                meta.setMapView(mapView);
            }

            if (meta != null) {
                meta.setCustomModelData(1); // 设置特殊的 CustomModelData，用于区分自定义地图
            }
            item.setItemMeta(meta);

            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), item);

            // 存储玩家地图
            playerMaps.put(player.getUniqueId(), mapView);
        }

        // 创建一个新的BossBar，并设置样式和颜色
        BossBar bossBar = Bukkit.createBossBar("Verification Countdown", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(1.0); // 设置初始进度为满
        bossBar.addPlayer(player); // 将玩家添加到BossBar中

        // 将BossBar存储到Map中
        playerBossBars.put(player.getUniqueId(), bossBar);

        // 启动一个倒计时任务
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = COUNTDOWN_DURATION;

            @Override
            public void run() {
                // 计算剩余时间的百分比
                double progress = (double) countdown / COUNTDOWN_DURATION;

                // 更新BossBar的进度
                if (progress < 0) {
                    progress = 0;
                } else if (progress > 1) {
                    progress = 1;
                }
                bossBar.setProgress(progress);

                // 更新BossBar的进度
                bossBar.setProgress(progress);

                // 更新BossBar显示的时间
                bossBar.setTitle("验证倒计时: " + countdown + "s");

                if (progress < 0.5) {
                    bossBar.setColor(BarColor.YELLOW);
                }
                if (progress < 0.2) {
                    bossBar.setColor(BarColor.RED);
                }

                // 如果倒计时结束且玩家未验证通过，则踢出玩家
                if (countdown <= 0 && !playerVerification.getOrDefault(player.getUniqueId(), false)) {
                    // 使用BukkitScheduler安排一个同步任务来移除效果
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        do {
                            player.removePotionEffect(PotionEffectType.BLINDNESS);
                        } while (player.hasPotionEffect(PotionEffectType.BLINDNESS));

                        do {
                            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        } while (player.hasPotionEffect(PotionEffectType.NIGHT_VISION));

                    });

                    playerVerification.remove(player.getUniqueId());
                    playerCaptcha.remove(player.getUniqueId());
                    // 检查玩家是否存在存储的物品和地图
                    if (playerItems.containsKey(player.getUniqueId()) && playerMaps.containsKey(player.getUniqueId())) {
                        // 还原玩家手中的物品
                        player.getInventory().setItemInMainHand(playerItems.get(player.getUniqueId()));

                        // 删除玩家手中的地图
                        MapView mapView = playerMaps.get(player.getUniqueId());
                        if (mapView != null) {
                            mapView.removeRenderer(mapView.getRenderers().get(0));
                            player.getInventory().removeItem(new ItemStack(Material.FILLED_MAP, 1, (short) mapView.getId()));
                        }

                        playerItems.remove(player.getUniqueId());
                        playerMaps.remove(player.getUniqueId());
                    }

                    // 移除玩家对应的BossBar和倒计时任务
                    BossBar bossBar = playerBossBars.remove(player.getUniqueId());
                    if (bossBar != null) {
                        bossBar.removeAll(); // 移除所有玩家
                    }

                    player.kickPlayer("""
                            被服务器踢出
                                                    
                            §c原因：验证超时
                                                    
                            §8==================================
                                                    
                            §7进入服务器后，请在规定时间内通过人机验证。
                                                    
                            频道号：""" + config.getString("channel-id") + """
                            
                            §7更多帮助请参考：§f""" + config.getString("reference-page") + """
                            §7
                            
                            
                            """ + getCurrentDateTime());

                    cancel(); // 停止倒计时任务
                }

                countdown--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L); // 每秒更新一次

        // 存储倒计时任务，以便后续取消
        countdownTasks.put(player.getUniqueId(), countdownTask);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        // 检查是否扔出的物品是地图，并且是自定义地图
        if (item.getType() == Material.FILLED_MAP && isCustomMap(item) && playerMaps.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // 阻止玩家扔出地图
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        playerVerification.remove(playerId);
        playerCaptcha.remove(playerId);
        // 检查玩家是否存在存储的物品和地图
        if (playerItems.containsKey(player.getUniqueId()) && playerMaps.containsKey(player.getUniqueId())) {
            // 还原玩家手中的物品
            player.getInventory().setItemInMainHand(playerItems.get(player.getUniqueId()));

            // 删除玩家手中的地图
            MapView mapView = playerMaps.get(player.getUniqueId());
            if (mapView != null) {
                mapView.removeRenderer(mapView.getRenderers().get(0));
                player.getInventory().removeItem(new ItemStack(Material.FILLED_MAP, 1, (short) mapView.getId()));
            }

            playerItems.remove(player.getUniqueId());
            playerMaps.remove(player.getUniqueId());
        }

        // 移除玩家对应的BossBar和倒计时任务
        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll(); // 移除所有玩家
        }

        BukkitTask countdownTask = countdownTasks.remove(playerId);
        if (countdownTask != null) {
            countdownTask.cancel(); // 取消倒计时任务
        }

        // 使用BukkitScheduler安排一个同步任务来移除效果
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // 取消失明效果
            player.removePotionEffect(PotionEffectType.BLINDNESS);

            // 取消夜视效果
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        });

        playerVerification.remove(playerId);
        playerCaptcha.remove(playerId);
        playerItems.remove(playerId);
        playerMaps.remove(playerId);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        playerVerification.remove(playerId);
        playerCaptcha.remove(playerId);
        // 检查玩家是否存在存储的物品和地图
        if (playerItems.containsKey(player.getUniqueId()) && playerMaps.containsKey(player.getUniqueId())) {
            // 还原玩家手中的物品
            player.getInventory().setItemInMainHand(playerItems.get(player.getUniqueId()));

            // 删除玩家手中的地图
            MapView mapView = playerMaps.get(player.getUniqueId());
            if (mapView != null) {
                mapView.removeRenderer(mapView.getRenderers().get(0));
                player.getInventory().removeItem(new ItemStack(Material.FILLED_MAP, 1, (short) mapView.getId()));
            }

            playerItems.remove(player.getUniqueId());
            playerMaps.remove(player.getUniqueId());
        }

        // 移除玩家对应的BossBar和倒计时任务
        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll(); // 移除所有玩家
        }

        BukkitTask countdownTask = countdownTasks.remove(playerId);
        if (countdownTask != null) {
            countdownTask.cancel(); // 取消倒计时任务
        }

        // 使用BukkitScheduler安排一个同步任务来移除效果
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // 取消失明效果
            player.removePotionEffect(PotionEffectType.BLINDNESS);

            // 取消夜视效果
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        });

        playerVerification.remove(playerId);
        playerCaptcha.remove(playerId);
        playerItems.remove(playerId);
        playerMaps.remove(playerId);
    }

    // 检查是否为自定义地图
    private boolean isCustomMap(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModelData = item.getItemMeta().getCustomModelData();
            return customModelData == 1;
        }
        return false;
    }

    // Handle player commands
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(config.getString("captcha-not-authed"));
        }
    }

    // Handle player chat
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            if (playerCaptcha.containsKey(player.getUniqueId())) {
                String[] captcha;
                captcha = playerCaptcha.get(player.getUniqueId());
                if (Objects.equals(captcha[1], event.getMessage())) {
                    playerVerification.put(player.getUniqueId(), true);
                    player.sendTitle(ChatColor.GREEN + "验证成功", ChatColor.YELLOW + "您现在可正常游戏了", 10, 70, 20);
                    player.sendMessage("§a亲爱的" + player.getDisplayName() + "，欢迎来到" + config.getString("server-name") + "！");
                    if (playerItems.containsKey(player.getUniqueId()) && playerMaps.containsKey(player.getUniqueId())) {
                        // 如果玩家验证通过，则取消倒计时任务和移除 BossBar
                        cancelCountdown(player);

                        player.setInvulnerable(false);

                        // 还原玩家手中的物品
                        player.getInventory().setItemInMainHand(playerItems.get(player.getUniqueId()));

                        // 删除玩家手中的地图
                        MapView mapView = playerMaps.get(player.getUniqueId());
                        mapView.removeRenderer(mapView.getRenderers().get(0));
                        player.getInventory().removeItem(new ItemStack(Material.FILLED_MAP, 1, (short) mapView.getId()));

                        // 使用BukkitScheduler安排一个同步任务来移除效果
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            do {
                                player.removePotionEffect(PotionEffectType.BLINDNESS);
                            } while (player.hasPotionEffect(PotionEffectType.BLINDNESS));

                            do {
                                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                            } while (player.hasPotionEffect(PotionEffectType.NIGHT_VISION));

                        });

                        playerItems.remove(player.getUniqueId());
                        playerMaps.remove(player.getUniqueId());
                    }
                } else {
                    event.getPlayer().sendMessage(config.getString("captcha-code-error"));
                }
            } else {
                event.getPlayer().sendMessage(config.getString("captcha-not-authed"));
            }
            event.setCancelled(true);
        }
    }

    // Handle player interactions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            event.setCancelled(true);
        }
    }

    // 处理玩家移动
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // 检查玩家是否未验证
        if (!playerVerification.getOrDefault(playerId, false)) {
            // 获取当前位置和目标位置
            Location from = event.getFrom();
            Location to = event.getTo();

            // 将水平方向上的移动分量设置为零
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());

            // 将水平方向上的速度设置为零
            event.getPlayer().setVelocity(new Vector(0, 0, 0));

            // 更新玩家的目标位置
            event.setTo(to);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSwitch(PlayerItemHeldEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(config.getString("captcha-not-authed"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpenEvent(InventoryOpenEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!playerVerification.getOrDefault(playerId, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(config.getString("captcha-not-authed"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisable(PluginDisableEvent event) {
        saveData();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnable(PluginEnableEvent event) {
        loadData();
    }

    private void saveData() {
        dataConfig.set("playerItems", null);
        dataConfig.set("playerMaps", null);

        for (UUID playerId : playerItems.keySet()) {
            dataConfig.set("playerItems." + playerId, playerItems.get(playerId));
        }

        for (UUID playerId : playerMaps.keySet()) {
            dataConfig.set("playerMaps." + playerId, playerMaps.get(playerId).getId());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
        }
    }

    private void loadData() {
        if (dataFile.exists()) {
            ConfigurationSection itemsSection = dataConfig.getConfigurationSection("playerItems");
            if (itemsSection != null) {
                for (String playerIdStr : itemsSection.getKeys(false)) {
                    UUID playerId = UUID.fromString(playerIdStr);
                    ItemStack itemStack = (ItemStack) itemsSection.get(playerIdStr);
                    playerItems.put(playerId, itemStack);
                }
            }

            ConfigurationSection mapsSection = dataConfig.getConfigurationSection("playerMaps");
            if (mapsSection != null) {
                for (String playerIdStr : mapsSection.getKeys(false)) {
                    UUID playerId = UUID.fromString(playerIdStr);
                    int mapViewId = mapsSection.getInt(playerIdStr);
                    MapView mapView = Bukkit.getMap(mapViewId);
                    if (mapView != null) {
                        playerMaps.put(playerId, mapView);
                    }
                }
            }
        }
    }

    private void cancelCountdown(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask countdownTask = countdownTasks.remove(playerId);
        if (countdownTask != null) {
            countdownTask.cancel(); // 取消倒计时任务
        }

        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll(); // 移除所有玩家
        }
    }
}
