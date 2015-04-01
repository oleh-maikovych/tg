package ua.com.fielden.platform.web.resources.webui;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import ua.com.fielden.platform.criteria.generator.ICriteriaGenerator;
import ua.com.fielden.platform.dao.IEntityDao;
import ua.com.fielden.platform.domaintree.IGlobalDomainTreeManager;
import ua.com.fielden.platform.domaintree.centre.ICentreDomainTreeManager.ICentreDomainTreeManagerAndEnhancer;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.factory.ICompanionObjectFinder;
import ua.com.fielden.platform.swing.menu.MiWithConfigurationSupport;
import ua.com.fielden.platform.swing.review.development.EnhancedCentreEntityQueryCriteria;
import ua.com.fielden.platform.utils.Pair;
import ua.com.fielden.platform.web.centre.EntityCentre;
import ua.com.fielden.platform.web.resources.RestServerUtil;

/**
 * The web resource for criteria serves as a back-end mechanism of criteria retrieval. It provides a base implementation for handling the following methods:
 * <ul>
 * <li>retrieve entity -- GET request.
 * </ul>
 *
 * @author TG Team
 *
 */
public class CriteriaResource<CRITERIA_TYPE extends AbstractEntity<?>> extends ServerResource {
    private final static Logger logger = Logger.getLogger(CriteriaResource.class);

    private final RestServerUtil restUtil;
    private final ICompanionObjectFinder companionFinder;

    private final Class<? extends MiWithConfigurationSupport<?>> miType;
    private final IGlobalDomainTreeManager gdtm;
    private final ICriteriaGenerator critGenerator;

    public CriteriaResource(
            final RestServerUtil restUtil,
            final ICompanionObjectFinder companionFinder,

            final EntityCentre centre,
            final IGlobalDomainTreeManager gdtm,
            final ICriteriaGenerator critGenerator,

            final Context context,
            final Request request,
            final Response response) {
        init(context, request, response);

        this.restUtil = restUtil;
        this.companionFinder = companionFinder;

        miType = centre.getMenuItemType();
        this.gdtm = gdtm;
        this.critGenerator = critGenerator;
    }

    /**
     * Handles GET requests resulting from tg-selection-criteria <code>retrieve()</code> method (new entity).
     */
    @Get
    @Override
    public Representation get() throws ResourceException {
        final ICentreDomainTreeManagerAndEnhancer originalCdtmae = CentreResourceUtils.getFreshCentre(gdtm, miType);
        return restUtil.rawListJSONRepresentation(
                CentreResourceUtils.createCriteriaValidationPrototype(miType, originalCdtmae, critGenerator, -1L),
                CentreResourceUtils.createCriteriaMetaValuesCustomObject(
                        CentreResourceUtils.createCriteriaMetaValues(originalCdtmae, CentreResourceUtils.getEntityType(miType)),
                        CentreResourceUtils.isFreshCentreChanged(miType, gdtm)
                        ));
    }

    /**
     * Handles POST request resulting resulting from tg-selection-criteria <code>validate()</code> method.
     */
    @Post
    @Override
    public Representation post(final Representation envelope) throws ResourceException {
        final ICentreDomainTreeManagerAndEnhancer originalCdtmae = CentreResourceUtils.getFreshCentre(gdtm, miType);
        final Map<String, Object> modifiedPropertiesHolder = EntityResourceUtils.restoreModifiedPropertiesHolderFrom(envelope, restUtil);
        CentreResourceUtils.applyMetaValues(originalCdtmae, CentreResourceUtils.getEntityType(miType), modifiedPropertiesHolder);
        final EnhancedCentreEntityQueryCriteria<AbstractEntity<?>, IEntityDao<AbstractEntity<?>>> validationPrototype =
                CentreResourceUtils.createCriteriaValidationPrototype(miType, originalCdtmae, critGenerator, EntityResourceUtils.getVersion(modifiedPropertiesHolder));

        return restUtil.rawListJSONRepresentation(
                EntityResourceUtils.constructEntityAndResetMetaValues(modifiedPropertiesHolder, validationPrototype, companionFinder).getKey(),
                CentreResourceUtils.createCriteriaMetaValuesCustomObject(
                        CentreResourceUtils.createCriteriaMetaValues(originalCdtmae, CentreResourceUtils.getEntityType(miType)),
                        CentreResourceUtils.isFreshCentreChanged(miType, gdtm)
                        ));
    }

    /**
     * Handles PUT request resulting from tg-selection-criteria <code>run()</code> method.
     */
    @Put
    @Override
    public Representation put(final Representation envelope) throws ResourceException {
        final ICentreDomainTreeManagerAndEnhancer originalCdtmae = CentreResourceUtils.getFreshCentre(gdtm, miType);
        final Map<String, Object> modifiedPropertiesHolder = EntityResourceUtils.restoreModifiedPropertiesHolderFrom(envelope, restUtil);
        CentreResourceUtils.applyMetaValues(originalCdtmae, CentreResourceUtils.getEntityType(miType), modifiedPropertiesHolder);
        final EnhancedCentreEntityQueryCriteria<AbstractEntity<?>, IEntityDao<AbstractEntity<?>>> validationPrototype = CentreResourceUtils.createCriteriaValidationPrototype(miType, originalCdtmae, critGenerator, EntityResourceUtils.getVersion(modifiedPropertiesHolder));
        final AbstractEntity<?> applied = EntityResourceUtils.constructEntityAndResetMetaValues(modifiedPropertiesHolder, validationPrototype, companionFinder).getKey();

        final Pair<Map<String, Object>, ArrayList<?>> pair =
                CentreResourceUtils.createCriteriaMetaValuesCustomObjectWithResult(
                        modifiedPropertiesHolder,
                        CentreResourceUtils.createCriteriaMetaValues(originalCdtmae, CentreResourceUtils.getEntityType(miType)),
                        applied,
                        CentreResourceUtils.isFreshCentreChanged(miType, gdtm));
        if (pair.getValue() == null) {
            return restUtil.rawListJSONRepresentation(applied, pair.getKey());
        }

        final ArrayList<Object> list = new ArrayList<Object>();
        list.add(applied);
        list.add(pair.getKey());

        list.addAll(pair.getValue()); // TODO why is this needed for serialisation to perform without problems?!

        return restUtil.rawListJSONRepresentation(list.toArray());
    }
}
