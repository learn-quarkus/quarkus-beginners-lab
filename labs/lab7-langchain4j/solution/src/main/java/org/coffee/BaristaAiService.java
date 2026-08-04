package org.coffee;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe, a specialty coffee shop.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee or our cafe, politely redirect the conversation.
    """)
public interface BaristaAiService {

    String chat(@UserMessage String message);
}
