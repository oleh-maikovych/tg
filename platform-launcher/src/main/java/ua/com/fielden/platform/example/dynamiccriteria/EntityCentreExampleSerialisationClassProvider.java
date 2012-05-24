package ua.com.fielden.platform.example.dynamiccriteria;

import ua.com.fielden.platform.basic.config.IApplicationSettings;
import ua.com.fielden.platform.example.dynamiccriteria.entities.SimpleCompositeEntity;
import ua.com.fielden.platform.example.dynamiccriteria.entities.SimpleECEEntity;
import ua.com.fielden.platform.example.dynamiccriteria.entities.SimpleNestedEntity;
import ua.com.fielden.platform.serialisation.impl.DefaultSerialisationClassProvider;

import com.google.inject.Inject;

public class EntityCentreExampleSerialisationClassProvider extends DefaultSerialisationClassProvider {

    @Inject
    public EntityCentreExampleSerialisationClassProvider(final IApplicationSettings settings) throws Exception {
	super(settings);
	types.add(SimpleECEEntity.class);
	types.add(SimpleNestedEntity.class);
	types.add(SimpleCompositeEntity.class);
    }

}
