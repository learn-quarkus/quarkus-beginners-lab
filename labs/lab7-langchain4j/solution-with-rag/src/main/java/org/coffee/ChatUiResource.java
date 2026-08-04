package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAiService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return chat.data("question", null, "reply", null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ask(@FormParam("message") String message) {
        if (message == null || message.isBlank()) {
            return chat.data("question", null, "reply", null);
        }
        String reply = baristaAiService.chat(message.trim());
        return chat.data("question", message.trim(), "reply", reply);
    }
}
