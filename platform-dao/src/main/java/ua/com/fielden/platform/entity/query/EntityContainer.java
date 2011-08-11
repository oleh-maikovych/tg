package ua.com.fielden.platform.entity.query;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.meta.MetaProperty;
import ua.com.fielden.platform.entity.query.EntityAggregates;
import ua.com.fielden.platform.reflection.Finder;

public class EntityContainer<R extends AbstractEntity> {
    	private final static String ID_PROPERTY_NAME = "id";

    	Class<R> resultType; // should also cover marker interfaces for TgCompositeUserType
	R entity;
	//Long id;
	boolean shouldBeFetched;
	Map<String, Object> primitives = new HashMap<String, Object>();
	Map<String, EntityContainer> entities = new HashMap<String, EntityContainer>();
	Map<String, Collection<EntityContainer>> collections = new HashMap<String, Collection<EntityContainer>>();
	private Logger logger = Logger.getLogger(this.getClass());

	public EntityContainer(final Class resultType, final boolean shouldBeFetched) {
	    this.resultType = resultType;

	// TODO inspect whether given resultType is not assigned from hibernate CompositeUserType - if it is - then treat accordingly

//	    if (id != null) {
//		this.id = ((Number) id).longValue();
//	    }
	    this.shouldBeFetched = shouldBeFetched;
	}

	public boolean notYetInitialised() {
	    return primitives.size() + entities.size() + collections.size() == 0;
	}

	public boolean isInstantiated() {
	    return entity != null;
	}

	public Long getId() {
	    final Object idObject = primitives.get(ID_PROPERTY_NAME);
	    return idObject != null ? ((Number) idObject).longValue() : null;//id;
	}

	public R instantiate(final EntityFactory entFactory, final boolean userViewOnly) {
	    logger.info("instantiating: " + resultType.getName() + " for id = " + getId() + " lightWeight = " + userViewOnly);
	    entity = userViewOnly ? entFactory.newPlainEntity(resultType, getId()) : entFactory.newEntity(resultType, getId());
	    entity.setInitialising(true);
	    for (final Map.Entry<String, Object> primPropEntry : primitives.entrySet()) {
		try {
		    setPropertyValue(entity, primPropEntry.getKey(), primPropEntry.getValue(), userViewOnly);
		    //entity.set(primPropEntry.getKey(), primPropEntry.getValue());
		} catch (final Exception e) {
		    throw new IllegalStateException("Can't set value [" + primPropEntry.getValue() + "] of type ["
			    + (primPropEntry.getValue() != null ? primPropEntry.getValue().getClass().getName() : " unknown") + "] for property [" + primPropEntry.getKey()
			    + "] due to:" + e);
		}
	    }

	    for (final Map.Entry<String, EntityContainer> entityEntry : entities.entrySet()) {
		if (entityEntry.getValue() == null || entityEntry.getValue().notYetInitialised() || !entityEntry.getValue().shouldBeFetched) {
		    setPropertyValue(entity, entityEntry.getKey(), null,  userViewOnly);
		} else if (entityEntry.getValue().isInstantiated()) {
		    setPropertyValue(entity, entityEntry.getKey(), entityEntry.getValue().entity, userViewOnly);
		} else {
		    setPropertyValue(entity, entityEntry.getKey(), entityEntry.getValue().instantiate(entFactory, userViewOnly), userViewOnly);
		}
	    }

	    for (final Map.Entry<String, Collection<EntityContainer>> entityEntry : collections.entrySet()) {
		Collection collectionalProp = null;
		try {
		    collectionalProp = entityEntry.getValue().getClass().newInstance();
		} catch (final Exception e) {
		    throw new RuntimeException("COULD NOT EXECUTE [collectionalProp = entityEntry.getValue().getClass().newInstance();] due to: " + e);
		}
		for (final EntityContainer container : entityEntry.getValue()) {
		    if (!container.notYetInitialised()) {
			collectionalProp.add(container.instantiate(entFactory, userViewOnly));
		    }
		}
		setPropertyValue(entity, entityEntry.getKey(), collectionalProp, userViewOnly);
	    }

	    if (!userViewOnly) {
		handleMetaProperties(entity);
	    }

	    entity.setInitialising(false);

	    return entity;
	}

	private void setPropertyValue(final AbstractEntity entity, final String propName, final Object propValue, final boolean userViewOnly) {
	    if (!userViewOnly || EntityAggregates.class.isAssignableFrom(resultType)) {
		entity.set(propName, propValue);
	    } else {
		try {
		    final Field field = Finder.findFieldByName(resultType, propName);
		    field.setAccessible(true);
//		    if (propValue == null && Money.class.isAssignableFrom(field.getType())) {
//			field.set(entity, new Money("0"));
//		    } else {
			field.set(entity, propValue);
//		    }
		    field.setAccessible(false);
		} catch (final Exception e) {
		    throw new RuntimeException("Can't set value for property " + propName + " due to:" + e.getMessage());
		}
	    }
	}

	private AbstractEntity<?> handleMetaProperties(final AbstractEntity<?> instance) {
	    final Object[] state = instance.getState();
	    final String[] propertyNames = instance.getPropertyNames();

	    // handle property "key" assignment
	    final int keyIndex = Arrays.asList(propertyNames).indexOf("key");
	    if (keyIndex >= 0 && state[keyIndex] != null) {
		instance.set("key", state[keyIndex]);
	    }

	    for (final MetaProperty meta : instance.getProperties().values()) {
		if (meta != null) {
		    final Object newOriginalValue = instance.get(meta.getName());
		    meta.setOriginalValue(newOriginalValue);
		    if (!meta.isCollectional()) {
			meta.define(newOriginalValue);
		    }
		}
	    }
	    instance.setDirty(false);
	    return instance;
	}

}
