package com.conflictview.websocket;

import com.conflictview.dto.ConflictMapDTO;
import com.conflictview.dto.NewsArticleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConflictUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishConflictUpdate(ConflictMapDTO update) {
        messagingTemplate.convertAndSend("/topic/conflicts/updates", update);
    }

    public void publishNewArticle(String conflictId, NewsArticleDTO article) {
        messagingTemplate.convertAndSend("/topic/news/" + conflictId, article);
    }
}
