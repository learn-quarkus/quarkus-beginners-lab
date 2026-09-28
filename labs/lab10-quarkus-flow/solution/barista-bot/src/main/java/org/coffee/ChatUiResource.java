package org.coffee;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAiService;

    @Inject
    ChatMemoryStore chatMemoryStore;

    // In-memory display history: sessionId → ordered list of {role, text} turns
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@CookieParam("session") String session) {
        List<Map<String, String>> history = session != null
                ? sessions.getOrDefault(session, List.of())
                : List.of();
        return chat.data("history", history);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response ask(
            @CookieParam("session") String session,
            @FormParam("message") String message) {

        if (message == null || message.isBlank()) {
            return Response.ok(chat.data("history", List.of()).render())
                    .type(MediaType.TEXT_HTML).build();
        }

        String sessionId = (session != null && !session.isBlank())
                ? session : UUID.randomUUID().toString();

        String reply = baristaAiService.chat(sessionId, message.trim());

        List<Map<String, String>> history =
                sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(Map.of("role", "user", "text", message.trim()));
        history.add(Map.of("role", "bot",  "text", reply));

        NewCookie cookie = new NewCookie.Builder("session")
                .value(sessionId).path("/").build();

        return Response.ok(chat.data("history", history).render())
                .type(MediaType.TEXT_HTML)
                .cookie(cookie)
                .build();
    }

    /**
     * End the current chat session — clears display history, evicts the
     * LangChain4j memory store, and expires the session cookie.
     */
    @POST
    @Path("/end")
    public Response end(@CookieParam("session") String session) {
        if (session != null && !session.isBlank()) {
            sessions.remove(session);
            chatMemoryStore.deleteMessages(session);
        }

        // Expire the cookie by setting max-age to 0
        NewCookie expired = new NewCookie.Builder("session")
                .value("").path("/").maxAge(0).build();

        return Response.seeOther(URI.create("/"))
                .cookie(expired)
                .build();
    }
}
