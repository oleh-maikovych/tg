package ua.com.fielden.platform.entity.query.fluent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.reflection.PropertyTypeDeterminator;

public class fetch<T extends AbstractEntity<?>> {
    private final Class<T> entityType;
    private final Map<String, fetch<? extends AbstractEntity<?>>> fetchModels = new HashMap<String, fetch<? extends AbstractEntity<?>>>();

    /**
     * Used mainly for serialisation.
     */
    protected fetch() {
	this.entityType = null;
    }

    fetch(final Class<T> entityType) {
	this.entityType = entityType;
	enhanceFetchModelWithKeyProperties();
    }

    private void enhanceFetchModelWithKeyProperties() {
	final List<String> keyMemberNames = Finder.getFieldNames(Finder.getKeyMembers(entityType));
	for (final String keyProperty : keyMemberNames) {
	    final Class propType = PropertyTypeDeterminator.determinePropertyType(entityType, keyProperty);
	    if (AbstractEntity.class.isAssignableFrom(propType)) {
		with(keyProperty, new fetch(propType));
	    }
	}
    }

    protected void withAll() {
	final List<Field> fields = Finder.findPropertiesThatAreEntities(entityType);
	for (final Field field : fields) {
	    fetchModels.put(field.getName(), new fetch(field.getType()));
	}
    }

    public fetch<T> with(final String propName) {
	final Class propType = PropertyTypeDeterminator.determinePropertyType(entityType, propName);
	if (AbstractEntity.class.isAssignableFrom(propType)) {
	    fetchModels.put(propName, new fetch(propType));
	} else {
	    throw new IllegalArgumentException(propName + " is of type " + propType.getName() + ". Only property, which is entity can be fetched");
	}
	return this;
    }

    public fetch<T> with(final String propName, final fetch<? extends AbstractEntity<?>> fetchModel) {
	if (AbstractEntity.class.isAssignableFrom(fetchModel.getEntityType())) {
	    fetchModels.put(propName, fetchModel);
	} else {
	    throw new IllegalArgumentException(propName + " has fetch model for type " + fetchModel.getEntityType().getName() + ". Fetch model with entity type is required.");
	}
	return this;
    }

    public Map<String, fetch<? extends AbstractEntity<?>>> getFetchModels() {
	return fetchModels;
    }

    public Class<T> getEntityType() {
	return entityType;
    }

    @Override
    public String toString() {
	return getString("     ");
    }

    private String getString(final String offset) {
	final StringBuffer sb = new StringBuffer();
	sb.append("\n");
	for (final Map.Entry<String, fetch<?>> fetchModel : fetchModels.entrySet()) {
	    sb.append(offset + fetchModel.getKey() + fetchModel.getValue().getString(offset + "   "));
	}

	return sb.toString();
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
	result = prime * result + ((fetchModels == null) ? 0 : fetchModels.hashCode());
	return result;
    }

    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!(obj instanceof fetch)) {
	    return false;
	}

	final fetch that = (fetch) obj;
	if (entityType == null) {
	    if (that.entityType != null) {
		return false;
	    }
	} else if (!entityType.equals(that.entityType)) {
	    return false;
	}
	if (fetchModels == null) {
	    if (that.fetchModels != null) {
		return false;
	    }
	} else if (!fetchModels.equals(that.fetchModels)) {
	    return false;
	}
	return true;
    }
}