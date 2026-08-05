package org.coffee;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
public class ChatResource {

    @Inject
    BaristaAiService baristaAiService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(@QueryParam("message") String message) {
        if (message == null || message.isBlank()) {
            return "Ask me anything about coffee! Try: ?message=What's a good morning coffee?";
        }
        return baristaAiService.chat("swagger", message);
    }
}
