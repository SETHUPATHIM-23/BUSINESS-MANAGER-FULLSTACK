package com.businessmanager.backend.printing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateRenderService {

    private final TemplateEngine templateEngine;

    /**
     * Renders a given HTML template using Thymeleaf with the provided context variables.
     *
     * @param templateName The name of the template (e.g. "invoice", "purchase_order").
     * @param variables    A map of variables to expose in the template context.
     * @return The rendered HTML string.
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        log.info("Rendering HTML template: {}", templateName);
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }
}
