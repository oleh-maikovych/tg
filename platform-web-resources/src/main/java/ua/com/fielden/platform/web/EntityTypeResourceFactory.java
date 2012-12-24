package ua.com.fielden.platform.web;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;

import ua.com.fielden.platform.dao.IEntityDao;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.security.provider.IUserController;
import ua.com.fielden.platform.security.user.IUserProvider;
import ua.com.fielden.platform.serialisation.api.ISerialiser;
import ua.com.fielden.platform.web.resources.EntityQueryResource;
import ua.com.fielden.platform.web.resources.EntityTypeResource;
import ua.com.fielden.platform.web.resources.RestServerUtil;

import com.google.inject.Injector;

/**
 * This is {@link Restlet} implementation that provides logic for correct entity oriented resource instantiation. Specifically, it should be used to instantiate
 * {@link EntityTypeResource} for specific entity types.
 *
 * @author 01es
 *
 */
public class EntityTypeResourceFactory<T extends AbstractEntity<?>, DAO extends IEntityDao<T>> extends Restlet {
    private final Class<DAO> daoType;
    private final Injector injector;
    private final EntityFactory factory;
    private final RestServerUtil restUtil;

    /**
     * Instances of DAO and factory should be thread-safe as they are used by multiple instances of resources serving concurrent requests.
     */
    public EntityTypeResourceFactory(final Class<DAO> daoType, final Injector injector, final EntityFactory factory) {
	this.daoType = daoType;
	this.injector = injector;
	this.factory = factory;
	this.restUtil = new RestServerUtil(injector.getInstance(ISerialiser.class));
    }

    @Override
    public void handle(final Request request, final Response response) {
	super.handle(request, response);

	final DAO dao = injector.getInstance(daoType);

	final String username = (String) request.getAttributes().get("username");
	injector.getInstance(IUserProvider.class).setUsername(username, injector.getInstance(IUserController.class));

	if (Method.GET.equals(request.getMethod()) || Method.HEAD.equals(request.getMethod()) || Method.PUT.equals(request.getMethod())) {
	    new EntityTypeResource<T>(dao, factory, restUtil, getContext(), request, response).handle();
	} else if (Method.POST.equals(request.getMethod())) {
	    new EntityQueryResource<T>(dao, restUtil, getContext(), request, response).handle();
	}
    }
}
