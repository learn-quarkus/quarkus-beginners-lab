package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    When answering menu questions, use the available tools to get accurate, up-to-date information.
    When a customer asks to order, place, or buy an item:
      1. Use getItemPrice to look up the price per item.
      2. Calculate totalPrice = price × quantity.
      3. Use the placeOrder tool to submit the order.
    """)
public interface BaristaAiService {

    @McpToolBox("menu")
    @ToolBox(OrderTools.class)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
