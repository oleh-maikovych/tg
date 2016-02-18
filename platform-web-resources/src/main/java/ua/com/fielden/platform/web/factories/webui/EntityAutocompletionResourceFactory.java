package ua.com.fielden.platform.web.factories.webui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.factory.ICompanionObjectFinder;
import ua.com.fielden.platform.reflection.ClassesRetriever;
import ua.com.fielden.platform.security.provider.IUserController;
import ua.com.fielden.platform.security.user.IUserProvider;
import ua.com.fielden.platform.web.master.EntityMaster;
import ua.com.fielden.platform.web.resources.RestServerUtil;
import ua.com.fielden.platform.web.resources.webui.EntityAutocompletionResource;

import com.google.inject.Injector;

/**
 * A factory for entity autocompletion resources which instantiate resources based on entity type and propertyName of the autocompletion property.
 *
 * The entity type information is a part of the URI: "/users/{username}/validation/{entityType}/{property}".
 *
 * @author TG Team
 *
 */
public class EntityAutocompletionResourceFactory extends Restlet {

    private final Injector injector;
    private final RestServerUtil restUtil;
    private final EntityFactory factory;
    private final Map<Class<? extends AbstractEntity<?>>, EntityMaster<? extends AbstractEntity<?>>> masters;

    /**
     * Instantiates a factory for entity validation resources.
     *
     * @param masters
     *            -- a list of {@link EntityMaster}s from which fetch models and other information arrive
     * @param injector
     */
    public EntityAutocompletionResourceFactory(final List<EntityMaster<? extends AbstractEntity<?>>> masters, final Injector injector) {
        this.masters = new LinkedHashMap<>();
        for (final EntityMaster<? extends AbstractEntity<?>> master : masters) {
            this.masters.put(master.getEntityType(), master);
        }
        this.injector = injector;
        this.restUtil = injector.getInstance(RestServerUtil.class);
        this.factory = injector.getInstance(EntityFactory.class);
    }

    @Override
    public void handle(final Request request, final Response response) {
        super.handle(request, response);

        if (Method.POST == request.getMethod()) {
            final String username = (String) request.getAttributes().get("username");
            injector.getInstance(IUserProvider.class).setUsername(username, injector.getInstance(IUserController.class));

            final String entityTypeString = (String) request.getAttributes().get("entityType");
            final String propertyName = (String) request.getAttributes().get("property");

            final Class<? extends AbstractEntity<?>> entityType = (Class<? extends AbstractEntity<?>>) ClassesRetriever.findClass(entityTypeString);
            final EntityMaster<? extends AbstractEntity<?>> master = this.masters.get(entityType);

            final EntityAutocompletionResource<AbstractEntity<?>> resource = new EntityAutocompletionResource<AbstractEntity<?>>((Class<AbstractEntity<?>>) entityType, propertyName, master.createValueMatcher(injector, propertyName), injector.getInstance(ICompanionObjectFinder.class), restUtil, getContext(), request, response);
            resource.handle();
        }
    }
}