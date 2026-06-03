package com.paathai.config;

import com.paathai.controller.LiveSessionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveSessionWebSocketHandler liveSessionWebSocketHandler;

    public WebSocketConfig(LiveSessionWebSocketHandler liveSessionWebSocketHandler) {
        this.liveSessionWebSocketHandler = liveSessionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveSessionWebSocketHandler, "/ws/live")
                .setAllowedOrigins("*"); // Handled by security config CORS
    }
}
