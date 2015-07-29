package ua.com.fielden.platform.web.centre.widgets;

import java.util.Map;

import ua.com.fielden.platform.utils.Pair;
import ua.com.fielden.platform.web.centre.api.context.CentreContextConfig;
import ua.com.fielden.platform.web.view.master.api.widgets.autocompleter.impl.AbstractEntityAutocompletionWidget;

/**
 * A wrapper for <code>tg-entity-search-criteria</code> that represents a widget for specifying multiple search queries against a property of an entity type as a part of an entity
 * centre.
 *
 * @author TG Team
 *
 */
public class EntityCritAutocompletionWidget extends AbstractEntityAutocompletionWidget {
    private final CentreContextConfig centreContextConfig;

    public EntityCritAutocompletionWidget(final Pair<String, String> titleDesc, final String propertyName, final CentreContextConfig centreContextConfig) {
        super("editors/tg-entity-editor", titleDesc, propertyName);
        this.centreContextConfig = centreContextConfig;
    }

    @Override
    protected Map<String, Object> createCustomAttributes() {
        final Map<String, Object> attrs = super.createCustomAttributes();
        attrs.put("autocompletion-type", "[[miType]]");

        attrs.put("multi", "true");
        this.addCentreContextBindings(attrs, centreContextConfig);
        return attrs;
    };
}
