package maeshil.openlog;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Openlog extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // 이벤트 리스너 등록
        Bukkit.getPluginManager().registerEvents(this, this);

        // Logger 필터 설정
        Logger logger = getLogger();
        logger.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                String message = record.getMessage();
                // 귓속말 명령어 필터링
                return !(message.contains("/w") || message.contains("/msg"));
            }
        });
    }

    @Override
    public void onDisable() {
        // 플러그인 종료 로직
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        // 서버 명령어 로그를 웹 서버로 전송
        String command = event.getCommand();
        if (!command.startsWith("w") && !command.startsWith("msg")) {
            sendLogToWebServer(command);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // 플레이어 명령어 로그를 필터링
        String message = event.getMessage();
        if (message.startsWith("/w") || message.startsWith("/msg")) {
            return; // 귓속말 명령어는 무시
        }
        sendLogToWebServer(message);
    }

    private void sendLogToWebServer(String log) {
        String urlString = "http://127.0.0.1:8000/logs"; // 로그를 전송할 URL
        try {
            // URL 객체 생성
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // HTTP 메서드 및 헤더 설정
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // JSON 형식으로 로그 데이터 작성
            String jsonInputString = "{\"log\": \"" + log.replace("\"", "\\\"") + "\"}";

            // 요청 본문에 데이터 쓰기
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                getLogger().info("로그 전송 성공: " + log);
            } else {
                getLogger().warning("로그 전송 실패: 응답 코드 " + responseCode);
            }

        } catch (Exception e) {
            getLogger().severe("로그 전송 중 오류 발생: " + e.getMessage());
        }
    }
}