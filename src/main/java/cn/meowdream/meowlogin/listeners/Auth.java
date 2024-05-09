package cn.meowdream.meowlogin.listeners;

import cn.meowdream.meowlogin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class Auth implements Listener {

    private String apiUrl;

    public static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    public static String encodeURI(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName(),
                playerIP = player.getAddress().getAddress().toString();

        FileConfiguration config = Main.getInstance().getConfig();

        try {
            apiUrl = config.getString("api-server");
            String encodedName = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
            URL url = new URL(apiUrl + "/login/checkStatus?username=" + encodedName + "&&IP=" + playerIP + "&&app=" + encodeURI(config.getString("server-name")) + "&&desc=" + encodeURI(config.getString("server-desc")));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            int apiResponse = Integer.parseInt(response.toString());
            if (apiResponse == 0) {
                event.setJoinMessage("§a" + playerName + "§f 从沉睡中苏醒了");
            } else if (apiResponse == -1) {
                player.kickPlayer("""
                        无法连接到服务器
                                                
                        §c原因：您尚未登录
                                                
                        §8==================================
                                                
                        §7请前往本服务器QQ频道，使用频道机器人进行登录。
                        详细信息请私信§f“汐汐酱”§7发送“/帮助”指令获取或查阅本服务器Wiki
                        完成以上操作后，请重新连接服务器
                                                
                        频道号：""" + config.getString("channel-id") + """
                        
                        §7更多帮助请参考：§f""" + config.getString("reference-page") + """
                        §7
                        
                                                
                        """ + getCurrentDateTime());
            } else {
                player.kickPlayer("""
                        无法连接到服务器
                        
                        §c原因：无效的登录响应
                        
                        §8==================================
                        
                        §7您的配置一切正常！
                        该故障可能由服务器配置错误引起，请联系管理员解决
                        
                        频道号：""" + config.getString("channel-id") + """
                        
                        §7更多帮助请参考：§f""" + config.getString("reference-page") + """
                        §7
                        
                                                
                        """ + getCurrentDateTime());
                getLogger().info("Unexpected response from server: " + apiResponse);
            }
        } catch (IOException e) {
            player.kickPlayer("""
                    无法连接到服务器
                                            
                    §c原因：验证服务器离线
                                            
                    §8==================================
                                            
                    §7我们暂时无法连接到身份验证服务，请稍候再试
                    若长时间无法进入服务器，请联系管理员解决
                                            
                    频道号：""" + config.getString("channel-id") + """
                    
                    §7更多帮助请参考：§f""" + config.getString("reference-page") + """
                    §7
                    
                                            
                    """ + getCurrentDateTime());
            getLogger().log(Level.SEVERE, "Error while sending player join event to API.", e);
        }
    }
}
