package ua.com.fielden.platform.web.factories.webui;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;

import ua.com.fielden.platform.serialisation.api.SerialiserEngines;
import ua.com.fielden.platform.serialisation.api.impl.TgJackson;
import ua.com.fielden.platform.web.resources.RestServerUtil;
import ua.com.fielden.platform.web.resources.webui.TgReflectorComponentResource;

import com.google.inject.Injector;

/**
 * Resource factory for tg-reflector component.
 *
 * @author TG Team
 *
 */
public class TgReflectorComponentResourceFactory extends Restlet {
    private final RestServerUtil restUtil;
    private final TgJackson tgJackson;

    public TgReflectorComponentResourceFactory(final Injector injector) {
        this.restUtil = injector.getInstance(RestServerUtil.class);
        this.tgJackson = (TgJackson) this.restUtil.getSerialiser().getEngine(SerialiserEngines.JACKSON);
    }

    @Override
    public void handle(final Request request, final Response response) {
        super.handle(request, response);

        // final String username = (String) request.getAttributes().get("username");
        // injector.getInstance(IUserProvider.class).setUsername(username, injector.getInstance(IUserController.class));

        if (Method.GET == request.getMethod()) {
            final TgReflectorComponentResource resource = new TgReflectorComponentResource(restUtil, getContext(), request, response, tgJackson);
            resource.handle();
        }
    }
}
