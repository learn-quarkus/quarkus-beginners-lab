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
    Template chat;                      // maps to src/main/resources/templates/chat.html

    @Inject
    BaristaAiService baristaAiService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return chat.data("message", null, "reply", null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ask(@FormParam("message") String message) {
        String reply = (message == null || message.isBlank())
                ? "Please type a question first!"
                : baristaAiService.chat(message);
        return chat.data("message", message, "reply", reply);
    }
}
