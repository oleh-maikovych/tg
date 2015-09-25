package ua.com.fielden.platform.sample.domain;

import ua.com.fielden.platform.dao.DefaultEntityProducer;
import ua.com.fielden.platform.dao.IEntityProducer;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.factory.ICompanionObjectFinder;
import ua.com.fielden.platform.entity.fetch.IFetchProvider;

import com.google.inject.Inject;

/**
 * A producer for new instances of entity {@link TgEntityForColourMaster}.
 *
 * @author TG Team
 *
 */
public class TgEntityForColourMasterProduser extends DefaultEntityProducer<TgEntityForColourMaster> implements IEntityProducer<TgEntityForColourMaster> {
    private final ITgEntityForColourMaster coTgEntityForColourMaster;

    @Inject
    public TgEntityForColourMasterProduser(final EntityFactory factory, final ICompanionObjectFinder companionFinder, final ITgEntityForColourMaster coTgEntityForColourMaster) {
        super(factory, TgEntityForColourMaster.class, companionFinder);
        this.coTgEntityForColourMaster = coTgEntityForColourMaster;
    }

    @Override
    protected TgEntityForColourMaster provideDefaultValues(final TgEntityForColourMaster entity) {
        final IFetchProvider<TgEntityForColourMaster> fetchStrategy = coTgEntityForColourMaster.getFetchProvider();
        final TgEntityForColourMaster defValue = coTgEntityForColourMaster.findByKeyAndFetch(fetchStrategy.<TgEntityForColourMaster> fetchFor("producerInitProp").fetchModel(), "DEFAULT_KEY");

        System.out.println("defValue.getProperty(producerInitProp).isProxy() == " + defValue.getProperty("producerInitProp").isProxy());

        entity.setProducerInitProp(defValue);
        return entity;
    }
}