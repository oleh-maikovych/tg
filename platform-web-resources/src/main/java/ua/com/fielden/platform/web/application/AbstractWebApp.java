package ua.com.fielden.platform.web.application;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import ua.com.fielden.platform.entity.functional.centre.IQueryRunner;
import ua.com.fielden.platform.entity.functional.centre.QueryRunner;
import ua.com.fielden.platform.web.WebAppConfig;
import ua.com.fielden.platform.web.factories.MainWebApplicationResourceFactory;
import ua.com.fielden.platform.web.factories.webui.CentreResourceFactory;
import ua.com.fielden.platform.web.factories.webui.FileResourceFactory;
import ua.com.fielden.platform.web.factories.webui.FunctionalEntityResourceFactory;
import ua.com.fielden.platform.web.factories.webui.SerialisationTestResourceFactory;
import ua.com.fielden.platform.web.factories.webui.TgReflectorComponentResourceFactory;
import ua.com.fielden.platform.web.factories.webui.WebViewResourceFactory;

import com.google.inject.Injector;

/**
 * Represents the web application that is running on the server as a resource provider for browser client. Extend this abstract web application in order to provide custom entity
 * centres, entity masters and other custom views.
 *
 * @author TG Team
 *
 */
public abstract class AbstractWebApp extends Application {

    private final Injector injector;
    // TODO when authentication mechanism will be implemented then user name won't be needed any longer.
    private final String username;
    /**
     * The paths for any kind of file resources those are needed for browser client. These are mapped to the '/resources/' router path. Also these resource paths might be augmented
     * with other custom paths. When client asks for a resource then this application will search for that resource in these paths starting from the custom ones.
     */
    private final Set<String> resourcePaths = new LinkedHashSet<>();

    protected final Logger logger = Logger.getLogger(getClass());

    /**
     * Creates an instance of {@link AbstractWebApp} with custom application name, description, author, owner and resource paths.
     *
     * @param context
     * @param injector
     * @param resourcePaths
     *            - additional root paths for file resources. (see {@link #resourcePaths} for more information).
     * @param appName
     *            - meaningful application name.
     * @param desc
     *            - short description for this application.
     * @param owner
     *            - the application owner.
     * @param author
     *            - the application author
     * @param username
     *            - TODO will be removed later after the authentication mechanism for web browser client will be implemented.
     */
    public AbstractWebApp(
            final Context context,
            final Injector injector,
            final String[] resourcePaths,
            final String appName,
            final String desc,
            final String owner,
            final String author,
            final String username) {
        super(context);
        //        this.platformJsScriptsLocation = "../../tg/platform-web-ui/src/main/web/ua/com/fielden/platform/web/";
        //        this.platformVendorJsScriptsLocation = "../../tg/platform-web-ui/src/main/resources/";
        // --> TODO not so elegant and flexible. There should be more elegant version for development and deployment. Use application.props file.
        //        this.platformGisJsScriptsLocation = platformJsScriptsLocation + "gis/";
        // --> TODO not so elegant and flexible. There should be more elegant version for development and deployment. Use application.props file.
        this.username = username;
        this.injector = injector;
        this.resourcePaths.addAll(Arrays.asList("", "ua/com/fielden/platform/web/"));
        this.resourcePaths.addAll(Arrays.asList(resourcePaths));
        setName(appName);
        setDescription(desc);
        setOwner(owner);
        setAuthor(author);
    }

    /**
     * Creates router and configures it with default resources and their paths.
     *
     */
    @Override
    public final Restlet createInboundRoot() {
        // Create router and web application for registering resources.
        final Router router = new Router(getContext());
        final WebAppConfig webAppConfig = new WebAppConfig(getName());

        // Initialise web application with entity centres, entity masters and other custom views.
        initWebApplication(webAppConfig);

        // Attach main application resource.
        router.attach("/", new MainWebApplicationResourceFactory(webAppConfig));

        // Registering entity centres.
        attachCentreResources(router, webAppConfig);

        // Registering web models.
        attachCustomWebViewResources(router, webAppConfig);

        // TODO Register entity masters and other custom views.

        // Attach resource for entity centre query runner.
        // TODO This is a spike resource. Must be replaced with generic functional entity resource.
        router.attach("/users/{username}/QueryRunner", new FunctionalEntityResourceFactory<QueryRunner, IQueryRunner>(IQueryRunner.class, injector));
        attachFunctionalEntities(router, injector);

        // Register resources those are in resource paths.
        attacheResources(router);

        // serialisation testing resource
        router.attach("/test/serialisation", new SerialisationTestResourceFactory(injector));
        // type meta info resource
        router.attach("/tg-reflector", new TgReflectorComponentResourceFactory(injector));

        return router;
    }

    /**
     * Attach custom functional entities to the router.
     *
     * @param router
     * @param injector
     */
    protected abstract void attachFunctionalEntities(final Router router, final Injector injector);

    /**
     * Configures router for entity centre resource.
     *
     * @param router
     * @param webAppConfig
     *            - holds the entity centre configurations.
     */
    private void attachCustomWebViewResources(final Router router, final WebAppConfig webAppConfig) {
        logger.info("\t\tWeb models resources (generated) attaching...");
        router.attach("/webview/{webViewName}", new WebViewResourceFactory(webAppConfig.getCustomViews()));
    }

    /**
     * Configures router for entity centre resource.
     *
     * @param router
     * @param webAppConfig
     *            - holds the entity centre configurations.
     */
    private void attachCentreResources(final Router router, final WebAppConfig webAppConfig) {
        logger.info("\t\tCentre resources attaching...");
        router.attach("/users/{username}/centre/{centreName}", new CentreResourceFactory(webAppConfig.getCentres(), injector));
    }

    /**
     * Configures router for file resources needed for web browser client.
     *
     * @param router
     */
    private void attacheResources(final Router router) {
        logger.info("\t\tResources attaching for:..." + "\n\t\t" + StringUtils.join(resourcePaths, "/\n\t\t") + "/");
        router.attach("/resources/", new FileResourceFactory(Collections.unmodifiableSet(resourcePaths)), Template.MODE_STARTS_WITH);
    }

    /**
     * Implement this in order to provide custom configurations for entity centre, master and other views.
     *
     * @param app
     */
    protected abstract void initWebApplication(WebAppConfig app);
}