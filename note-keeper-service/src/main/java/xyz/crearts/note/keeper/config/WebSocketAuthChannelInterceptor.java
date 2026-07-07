package xyz.crearts.note.keeper.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.service.JwtService;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = resolveUserId(accessor);
            if (userId == null) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
            accessor.setUser((Principal) () -> userId);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String userId = currentUserId(accessor);
            if (userId == null) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/updates/")) {
                String allowedDestination = "/topic/updates/" + userId;
                if (!allowedDestination.equals(destination)) {
                    throw new AccessDeniedException("Cannot subscribe to another user's updates");
                }
            }
        }

        return message;
    }

    private String resolveUserId(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.validateToken(authHeader.substring(7));
    }

    private String currentUserId(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        return user != null ? user.getName() : null;
    }
}
