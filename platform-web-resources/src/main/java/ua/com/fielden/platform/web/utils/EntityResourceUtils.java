package ua.com.fielden.platform.web.utils;

import static java.lang.String.format;
import static java.util.Locale.getDefault;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.from;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;

import ua.com.fielden.platform.basic.autocompleter.PojoValueMatcher;
import ua.com.fielden.platform.continuation.NeedMoreData;
import ua.com.fielden.platform.dao.CommonEntityDao;
import ua.com.fielden.platform.dao.DefaultEntityProducerWithContext;
import ua.com.fielden.platform.dao.IEntityDao;
import ua.com.fielden.platform.dao.IEntityProducer;
import ua.com.fielden.platform.dao.QueryExecutionModel;
import ua.com.fielden.platform.dao.exceptions.UnexpectedNumberOfReturnedEntities;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.AbstractFunctionalEntityForCollectionModification;
import ua.com.fielden.platform.entity.AbstractFunctionalEntityWithCentreContext;
import ua.com.fielden.platform.entity.DynamicEntityKey;
import ua.com.fielden.platform.entity.IContinuationData;
import ua.com.fielden.platform.entity.annotation.CritOnly;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.MapTo;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.factory.ICompanionObjectFinder;
import ua.com.fielden.platform.entity.fetch.IFetchProvider;
import ua.com.fielden.platform.entity.functional.centre.CentreContextHolder;
import ua.com.fielden.platform.entity.functional.centre.SavingInfoHolder;
import ua.com.fielden.platform.entity.functional.master.AcknowledgeWarnings;
import ua.com.fielden.platform.entity.meta.MetaProperty;
import ua.com.fielden.platform.entity.meta.PropertyDescriptor;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity_centre.review.criteria.EntityQueryCriteria;
import ua.com.fielden.platform.error.Result;
import ua.com.fielden.platform.reflection.AnnotationReflector;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.reflection.PropertyTypeDeterminator;
import ua.com.fielden.platform.reflection.Reflector;
import ua.com.fielden.platform.types.Colour;
import ua.com.fielden.platform.types.Hyperlink;
import ua.com.fielden.platform.types.Money;
import ua.com.fielden.platform.utils.EntityUtils;
import ua.com.fielden.platform.utils.MiscUtilities;
import ua.com.fielden.platform.utils.Pair;
import ua.com.fielden.platform.web.centre.CentreContext;
import ua.com.fielden.platform.web.centre.CentreUtils;
import ua.com.fielden.platform.web.resources.RestServerUtil;
import ua.com.fielden.platform.web.resources.webui.EntityResource;
import ua.com.fielden.platform.web.resources.webui.EntityValidationResource;

/**
 * This utility class contains the methods that are shared across {@link EntityResource} and {@link EntityValidationResource}.
 *
 * @author TG Team
 *
 */
public class EntityResourceUtils<T extends AbstractEntity<?>> {
    private final EntityFactory entityFactory;
    private final static Logger logger = Logger.getLogger(EntityResourceUtils.class);
    private final Class<T> entityType;
    private final IEntityDao<T> co;
    private final IEntityProducer<T> entityProducer;
    private final ICompanionObjectFinder companionFinder;

    public EntityResourceUtils(final Class<T> entityType, final IEntityProducer<T> entityProducer, final EntityFactory entityFactory, final ICompanionObjectFinder companionFinder) {
        this.entityType = entityType;
        this.companionFinder = companionFinder;
        this.co = companionFinder.<IEntityDao<T>, T> find(this.entityType);

        this.entityFactory = entityFactory;
        this.entityProducer = entityProducer;
    }

    /**
     * Initialises the entity for retrieval.
     *
     * @param id
     *            -- entity identifier
     * @return
     */
    public T createValidationPrototype(final Long id) {
        final T entity;
        if (id != null) {
            entity = co.findById(id, co.getFetchProvider().fetchModel());
        } else {
            entity = entityProducer.newEntity();
        }
        return entity;
    }

    /**
     * Initialises the functional entity for centre-context-dependent retrieval.
     *
     * @param centreContext
     *            the context for functional entity creation
     *
     * @return
     */
    public T createValidationPrototypeWithContext(
            final Long id,
            final CentreContext<T, AbstractEntity<?>> centreContext,
            final String chosenProperty,
            final Long compoundMasterEntityId,
            final AbstractEntity<?> masterContext) {
        if (id != null) {
            return co.findById(id, co.getFetchProvider().fetchModel());
        } else {
            final DefaultEntityProducerWithContext<T> defProducer = (DefaultEntityProducerWithContext<T>) entityProducer;
            defProducer.setCentreContext(centreContext);
            defProducer.setChosenProperty(chosenProperty);
            defProducer.setCompoundMasterEntityId(compoundMasterEntityId);
            defProducer.setMasterEntity(masterContext);
            return defProducer.newEntity();
        }
    }
    
    /**
     * Resets the context for the entity to <code>null</code> in case where the entity is {@link AbstractFunctionalEntityWithCentreContext} descendant.
     * <p>
     * This is necessary to be done just before sending the entity to the client application (retrieval, validation and saving actions). It should not be done in producer
     * because the validation prototype's context could be used later (during application of modified properties, or in DAO save method etc.).
     * 
     * @param entity
     * @return
     */
    public static <T extends AbstractEntity<?>> T resetContextBeforeSendingToClient(final T entity) {
        if (entity instanceof AbstractFunctionalEntityWithCentreContext && ((AbstractFunctionalEntityWithCentreContext) entity).getContext() != null) {
            final AbstractFunctionalEntityWithCentreContext<?> funcEntity = (AbstractFunctionalEntityWithCentreContext<?>) entity;
            funcEntity.setContext(null); // it is necessary to reset centreContext not to send it back to the client!
            funcEntity.getProperty("context").resetState();
        }
        return entity;
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public ICompanionObjectFinder getCompanionFinder() {
        return companionFinder;
    }

    public static <T extends AbstractEntity<?>, V extends AbstractEntity<?>> IFetchProvider<V> fetchForProperty(final ICompanionObjectFinder coFinder, final Class<T> entityType, final String propertyName) {
        if (EntityQueryCriteria.class.isAssignableFrom(entityType)) {
            final Class<? extends AbstractEntity<?>> originalType = CentreUtils.getOriginalType(entityType);
            final String originalPropertyName = CentreUtils.getOriginalPropertyName(entityType, propertyName);

            final boolean isEntityItself = "".equals(originalPropertyName); // empty property means "entity itself"
            return isEntityItself ? (IFetchProvider<V>) coFinder.find(originalType).getFetchProvider() : fetchForPropertyOrDefault(coFinder, originalType, originalPropertyName);
        } else {
            return coFinder.find(entityType).getFetchProvider().fetchFor(propertyName);
        }
    }

    /**
     * Returns fetch provider for property or, if the property should not be fetched according to default strategy, returns the 'default' property fetch provider with 'keys'
     * (simple an composite) and 'desc' (if 'desc' exists in domain entity).
     *
     * @param coFinder
     * @param entityType
     * @param propertyName
     * @return
     */
    private static <V extends AbstractEntity<?>> IFetchProvider<V> fetchForPropertyOrDefault(final ICompanionObjectFinder coFinder, final Class<? extends AbstractEntity<?>> entityType, final String propertyName) {
        final IFetchProvider<? extends AbstractEntity<?>> fetchProvider = coFinder.find(entityType).getFetchProvider();
        //        return fetchProvider.fetchFor(propertyName);
        return fetchProvider.shouldFetch(propertyName)
                ? fetchProvider.fetchFor(propertyName)
                : fetchProvider.with(propertyName).fetchFor(propertyName);
    }

    /**
     * Determines the version that is shipped with 'modifiedPropertiesHolder'.
     *
     * @param modifiedPropertiesHolder
     * @return
     */
    public static Long getVersion(final Map<String, Object> modifiedPropertiesHolder) {
        final Object arrivedVersionVal = modifiedPropertiesHolder.get(AbstractEntity.VERSION);
        return ((Integer) arrivedVersionVal).longValue();
    }

    /**
     * Applies the values from <code>modifiedPropertiesHolder</code> into the <code>entity</code>. The values needs to be converted from the client-side component-specific form into
     * the values, which can be set into Java entity's property.
     *
     * @param modifiedPropertiesHolder
     * @param entity
     * @return
     */
    public static <M extends AbstractEntity<?>> M apply(final Map<String, Object> modifiedPropertiesHolder, final M entity, final ICompanionObjectFinder companionFinder) {
        final Class<M> type = (Class<M>) entity.getType();
        final boolean isEntityStale = entity.getVersion() > getVersion(modifiedPropertiesHolder);

        final Set<String> appliedProps = new LinkedHashSet<>();
        final List<String> touchedProps = (List<String>) modifiedPropertiesHolder.get("@@touchedProps");
        
        // iterate through untouched properties first:
        //  (the order of application does not really matter - untouched properties were really applied earlier through some definers, that originate from touched properties)
        for (final Map.Entry<String, Object> nameAndVal : modifiedPropertiesHolder.entrySet()) {
            final String name = nameAndVal.getKey();
            if (!name.equals(AbstractEntity.ID) && !name.equals(AbstractEntity.VERSION) && !name.startsWith("@") /* custom properties disregarded */ && !touchedProps.contains(name)) {
                final Map<String, Object> valAndOrigVal = (Map<String, Object>) nameAndVal.getValue();
                // The 'modified' properties are marked using the existence of "val" sub-property.
                if (valAndOrigVal.containsKey("val")) { // this is a modified property
                    logger.debug(String.format("Apply untouched modified: type [%s] name [%s] isEntityStale [%s] valAndOrigVal [%s]", type.getSimpleName(), name, isEntityStale, valAndOrigVal));
                    applyModifiedPropertyValue(type, name, valAndOrigVal, entity, companionFinder, isEntityStale);
                    appliedProps.add(name);
                } else { // this is unmodified property
                    if (!isEntityStale) {
                        // do nothing
                    } else {
                        final Object originalValue = convert(type, name, valAndOrigVal.get("origVal"), reflectedValueId(valAndOrigVal, "origVal"), companionFinder);
                        final Object actualValue = entity.get(name);
                        if (EntityUtils.isStale(originalValue, actualValue)) {
                            logger.info(String.format("The property [%s] has been recently changed by other user for type [%s] to the value [%s]. Original value is [%s].", name, entity.getClass().getSimpleName(), actualValue, originalValue));
                            entity.getProperty(name).setDomainValidationResult(Result.warning(entity, "The property has been recently changed by other user."));
                        }
                    }
                }
            }
        }
        // iterate through touched properties:
        //  (the order of application is strictly the same as was done by the user in Web UI client - the only difference is 
        //  such that properties, that were touched twice or more times, will be applied only once)
        for (final String touchedProp : touchedProps) {
            final String name = touchedProp;
            final Map<String, Object> valAndOrigVal = (Map<String, Object>) modifiedPropertiesHolder.get(name);
            // The 'modified' properties are marked using the existence of "val" sub-property.
            if (valAndOrigVal.containsKey("val")) { // this is a modified property
                logger.debug(String.format("Apply touched modified: type [%s] name [%s] isEntityStale [%s] valAndOrigVal [%s]", type.getSimpleName(), name, isEntityStale, valAndOrigVal));
                applyModifiedPropertyValue(type, name, valAndOrigVal, entity, companionFinder, isEntityStale);
            } else { // this is unmodified property
                // IMPORTANT:
                // Unlike to the case of untouched properties, all touched properties should be applied,
                //  even unmodified ones.
                // This is necessary in order to mimic the user interaction with the entity (like was in Swing client)
                //  to have the ACE handlers executed for all touched properties.
                logger.debug(String.format("Apply touched unmodified: type [%s] name [%s] isEntityStale [%s] valAndOrigVal [%s]", type.getSimpleName(), name, isEntityStale, valAndOrigVal));
                applyUnmodifiedPropertyValue(type, name, valAndOrigVal, entity, companionFinder, isEntityStale);
            }
            appliedProps.add(name);
        }
        // IMPORTANT: the check for invalid will populate 'required' checks.
        //            It is necessary in case when some property becomes required after the change of other properties.
        entity.isValid();

        disregardCritOnlyRequiredProperties(entity);
        disregardNotAppliedRequiredProperties(entity, appliedProps);
        disregardAppliedRequiredPropertiesWithEmptyValueForNotPersistedEntity(entity, appliedProps);

        return entity;
    }
    
    /**
     * Applies the property value against the entity.
     * 
     * @param shouldApplyOriginalValue - indicates whether the 'origVal' should be applied (with 'enforced mutation') or 'val' (with simple mutation)
     * @param type
     * @param name
     * @param valAndOrigVal
     * @param entity
     * @param companionFinder
     * @param isEntityStale
     */
    private static <M extends AbstractEntity<?>> void applyPropertyValue(final boolean shouldApplyOriginalValue, final Class<M> type, final String name, final Map<String, Object> valAndOrigVal, final M entity, final ICompanionObjectFinder companionFinder, final boolean isEntityStale) {
        final String reflectedValueName = shouldApplyOriginalValue ? "origVal" : "val";
        final Object val = valAndOrigVal.get(reflectedValueName);
        final Object newValue = convert(type, name, val, reflectedValueId(valAndOrigVal, reflectedValueName), companionFinder);
        if (notFoundEntity(type, name, val, newValue)) {
            final String msg = String.format("No entity with key [%s] has been found.", val);
            logger.info(msg);
            entity.getProperty(name).setDomainValidationResult(Result.failure(entity, msg));
        } else if (!isEntityStale) {
            enforceSet(shouldApplyOriginalValue, name, entity, newValue);
        } else {
            final Object staleOriginalValue = convert(type, name, valAndOrigVal.get("origVal"), reflectedValueId(valAndOrigVal, "origVal"), companionFinder);
            final Object actualValue = entity.get(name);
            if (EntityUtils.isConflicting(newValue, staleOriginalValue, actualValue)) {
                logger.info(String.format("The property [%s] has been recently changed by other user for type [%s] to the value [%s]. Stale original value is [%s], newValue is [%s]. Please revert property value to resolve conflict.", name, entity.getClass().getSimpleName(), actualValue, staleOriginalValue, newValue));
                entity.getProperty(name).setDomainValidationResult(Result.failure(entity, "The property has been recently changed by other user. Please revert property value to resolve conflict."));
            } else {
                enforceSet(shouldApplyOriginalValue, name, entity, newValue);
            }
        }
    }
    
    /**
     * Extracts reflected value ID for 'val' or 'origVal' reflectedValueName if it exists.
     * 
     * @param valAndOrigVal
     * @param reflectedValueName
     * @return
     */
    private static Optional<Long> reflectedValueId(final Map<String, Object> valAndOrigVal, final String reflectedValueName) {
        final Object reflectedValueId = valAndOrigVal.get(reflectedValueName + "Id");
        if (reflectedValueId == null) {
            return Optional.empty();
        } else {
            return Optional.of(extractLongValueFrom(reflectedValueId));
        }
    }
    
    /**
     * Applies the modified property value ('val') against the entity.
     * 
     * @param type
     * @param name
     * @param valAndOrigVal
     * @param entity
     * @param companionFinder
     * @param isEntityStale
     */
    private static <M extends AbstractEntity<?>> void applyModifiedPropertyValue(final Class<M> type, final String name, final Map<String, Object> valAndOrigVal, final M entity, final ICompanionObjectFinder companionFinder, final boolean isEntityStale) {
        applyPropertyValue(false, type, name, valAndOrigVal, entity, companionFinder, isEntityStale);
    }
    
    /**
     * Applies the unmodified property value ('origVal') against the entity (using 'enforced mutation').
     * 
     * @param type
     * @param name
     * @param valAndOrigVal
     * @param entity
     * @param companionFinder
     * @param isEntityStale
     */
    private static <M extends AbstractEntity<?>> void applyUnmodifiedPropertyValue(final Class<M> type, final String name, final Map<String, Object> valAndOrigVal, final M entity, final ICompanionObjectFinder companionFinder, final boolean isEntityStale) {
        applyPropertyValue(true, type, name, valAndOrigVal, entity, companionFinder, isEntityStale);
    }

    /**
     * Sets the value for the entity property.
     * 
     * @param enforce - indicates whether to use 'enforced mutation'
     * @param name
     * @param entity
     * @param newValue
     */
    private static <M extends AbstractEntity<?>> void enforceSet(final boolean enforce, final String name, final M entity, final Object newValue) {
        if (enforce) {
            final MetaProperty<Object> metaProperty = entity.getProperty(name);
            final boolean currEnforceMutator = metaProperty.isEnforceMutator();
            metaProperty.setEnforceMutator(true);
            try {
                entity.set(name, newValue);
            } finally {
                metaProperty.setEnforceMutator(currEnforceMutator);
            }
        } else {
            entity.set(name, newValue);
        }
    }

    /**
     * Disregards the 'required' errors for those properties, that were not 'applied' (for both criteria and simple entities).
     *
     * @param entity
     * @param appliedProps
     *            -- list of 'applied' properties, i.e. those for which the setter has been invoked (maybe in 'enforced' manner)
     * @return
     */
    public static <M extends AbstractEntity<?>> M disregardNotAppliedRequiredProperties(final M entity, final Set<String> appliedProps) {
        entity.nonProxiedProperties().filter(mp -> mp.isRequired() && !appliedProps.contains(mp.getName())).forEach(mp -> {
            mp.setRequiredValidationResult(Result.successful(entity));
        });
        
        return entity;
    }
    
    /**
     * Disregards the 'required' errors for those properties, that were provided with some value and then cleared back to empty value during editing of new entity.
     *
     * @param entity
     * @param appliedProps
     *            -- list of 'applied' properties, i.e. those for which the setter has been invoked (maybe in 'enforced' manner)
     * @return
     */
    public static <M extends AbstractEntity<?>> M disregardAppliedRequiredPropertiesWithEmptyValueForNotPersistedEntity(final M entity, final Set<String> appliedProps) {
        if (!entity.isPersisted()) {
            entity.nonProxiedProperties().filter(mp -> mp.isRequired() && appliedProps.contains(mp.getName()) && mp.getValue() == null).forEach(mp -> {
                mp.setRequiredValidationResult(Result.successful(entity));
            });
        }

        return entity;
    }

    /**
     * Disregards the 'required' errors for crit-only properties on masters for non-criteria entity types.
     * 
     * @param entity
     */
    public static <M extends AbstractEntity<?>> void disregardCritOnlyRequiredProperties(final M entity) {
        final Class<?> managedType = entity.getType();
        if (!EntityQueryCriteria.class.isAssignableFrom(managedType)) {
            entity.nonProxiedProperties().filter(mp -> mp.isRequired()).forEach(mp -> {
                final String prop = mp.getName();
                final CritOnly critOnlyAnnotation = AnnotationReflector.getPropertyAnnotation(CritOnly.class, managedType, prop);
                if (critOnlyAnnotation != null) {
                    mp.setRequiredValidationResult(Result.successful(entity));
                }
            });
        }
    }

    /**
     * Returns <code>true</code> if the property is of entity type and the entity was not found by the search string (reflectedValue), <code>false</code> otherwise.
     *
     * @param type
     * @param propertyName
     * @param reflectedValue
     * @param newValue
     * @return
     */
    private static <M extends AbstractEntity<?>> boolean notFoundEntity(final Class<M> type, final String propertyName, final Object reflectedValue, final Object newValue) {
        return reflectedValue != null && newValue == null && EntityUtils.isEntityType(PropertyTypeDeterminator.determinePropertyType(type, propertyName));
    }
    
    /**
     * Determines property type.
     * <p>
     * The exception from standard logic is only for "collection modification func action", where the type of "chosenIds", "addedIds" and "removedIds" properties
     * determines from the second type parameter of the func action type. This is done due to generic nature of that types (see ID_TYPE parameter in {@link AbstractFunctionalEntityForCollectionModification}).
     * 
     * @param type
     * @param propertyName
     * @return
     */
    private static Class determinePropertyType(final Class<?> type, final String propertyName) {
        final Class propertyType;
        if (AbstractFunctionalEntityForCollectionModification.class.isAssignableFrom(type) && AbstractFunctionalEntityForCollectionModification.isCollectionOfIds(propertyName)) {
            if (type.getAnnotatedSuperclass() == null) {
                throw Result.failure(new IllegalStateException(String.format("The AnnotatedSuperclass of functional entity %s (for collection modification) is somehow not defined.", type.getSimpleName())));
            }
            if (!(type.getAnnotatedSuperclass().getType() instanceof ParameterizedType)) {
                throw Result.failure(new IllegalStateException(String.format("The AnnotatedSuperclass's Type %s of functional entity %s (for collection modification) is somehow not ParameterizedType.", type.getAnnotatedSuperclass().getType(), type.getSimpleName())));
            }
            final ParameterizedType parameterizedEntityType = (ParameterizedType) type.getAnnotatedSuperclass().getType();
            if (parameterizedEntityType.getActualTypeArguments().length != 1 || !(parameterizedEntityType.getActualTypeArguments()[0] instanceof Class)) {
                throw Result.failure(new IllegalStateException(String.format("The type parameters %s of functional entity %s (for collection modification) is malformed.", Arrays.asList(parameterizedEntityType.getActualTypeArguments()), type.getSimpleName())));
            }
            propertyType = (Class) parameterizedEntityType.getActualTypeArguments()[0];
        } else {
            propertyType = PropertyTypeDeterminator.determinePropertyType(type, propertyName);
        }
        return propertyType;
    }

    /**
     * Converts <code>reflectedValue</code>, which could be a string, a number, a boolean or a null, into a value of appropriate type (the type of the actual property).
     *
     * @param type
     * @param propertyName
     * @param reflectedValue
     * @param reflectedValueId -- in case where the property is entity-typed, this parameter represent an optional ID of the entity-typed value returned from the client application
     * 
     * @return
     */
    private static <M extends AbstractEntity<?>> Object convert(final Class<M> type, final String propertyName, final Object reflectedValue, final Optional<Long> reflectedValueId, final ICompanionObjectFinder companionFinder) {
        if (reflectedValue == null) {
            return null;
        }
        final Class<?> propertyType = determinePropertyType(type, propertyName);
        
        // NOTE: "missing value" for Java entities is also 'null' as for JS entities
        if (EntityUtils.isEntityType(propertyType)) {
            if (PropertyTypeDeterminator.isCollectional(type, propertyName)) {
                throw new UnsupportedOperationException(String.format("Unsupported conversion to [%s + %s] from reflected value [%s]. Entity-typed collectional properties are not supported.", type.getSimpleName(), propertyName, reflectedValue));
            }

            final Class<AbstractEntity<?>> entityPropertyType = (Class<AbstractEntity<?>>) propertyType;

            if (EntityUtils.isPropertyDescriptor(entityPropertyType)) {
                final Class<AbstractEntity<?>> enclosingEntityType = (Class<AbstractEntity<?>>) AnnotationReflector.getPropertyAnnotation(IsProperty.class, type, propertyName).value();
                return extractPropertyDescriptor((String) reflectedValue, enclosingEntityType).orElse(null);
            } else if (reflectedValueId.isPresent()) {
                logger.debug(String.format("ID-based restoration of value: type [%s] property [%s] propertyType [%s] id [%s] reflectedValue [%s].", type.getSimpleName(), propertyName, entityPropertyType.getSimpleName(), reflectedValueId.get(), reflectedValue));
                // regardless of whether entityPropertyType is composite or not, the entity should be retrieved by non-empty reflectedValueId that has been arrived from the client application
                final IEntityDao<AbstractEntity<?>> propertyCompanion = companionFinder.find(entityPropertyType).uninstrumented();
                return propertyCompanion.findById(reflectedValueId.get(), fetchForProperty(companionFinder, type, propertyName).fetchModel());
            } else if (EntityUtils.isCompositeEntity(entityPropertyType)) {
                logger.debug(String.format("KEY-based restoration of value: type [%s] property [%s] propertyType [%s] id [%s] reflectedValue [%s].", type.getSimpleName(), propertyName, entityPropertyType.getSimpleName(), reflectedValueId, reflectedValue));
                final String compositeKeyAsString = buildSearchByValue(propertyType, entityPropertyType, (String) reflectedValue);
                final EntityResultQueryModel<AbstractEntity<?>> model = select(entityPropertyType).where().//
                /*      */prop(AbstractEntity.KEY).iLike().anyOfValues((Object[]) MiscUtilities.prepare(Arrays.asList(compositeKeyAsString))).//
                /*      */model();
                final QueryExecutionModel<AbstractEntity<?>, EntityResultQueryModel<AbstractEntity<?>>> qem = from(model).with(fetchForProperty(companionFinder, type, propertyName).fetchModel()).model();
                try {
                    final IEntityDao<AbstractEntity<?>> propertyCompanion = companionFinder.<IEntityDao<AbstractEntity<?>>, AbstractEntity<?>> find(entityPropertyType).uninstrumented();
                    return propertyCompanion.getEntity(qem);
                } catch (final UnexpectedNumberOfReturnedEntities e) {
                    return null;
                }
            } else {
                logger.debug(String.format("KEY-based restoration of value: type [%s] property [%s] propertyType [%s] id [%s] reflectedValue [%s].", type.getSimpleName(), propertyName, entityPropertyType.getSimpleName(), reflectedValueId, reflectedValue));
                final String[] keys = MiscUtilities.prepare(Arrays.asList((String) reflectedValue));
                final String key;
                if (keys.length > 1) {
                    throw new IllegalArgumentException(format("Value [%s] does not represent a single key value, which is required for coversion to an instance of type [%s].", reflectedValue, entityPropertyType.getName()));
                } else if (keys.length == 0) {
                    key = "";
                } else {
                    key = keys[0];
                }
                
                final IEntityDao<AbstractEntity<?>> propertyCompanion = companionFinder.find(entityPropertyType).uninstrumented();
                return propertyCompanion.findByKeyAndFetch(fetchForProperty(companionFinder, type, propertyName).fetchModel(), key);
            }
            // prev implementation => return propertyCompanion.findByKeyAndFetch(getFetchProvider().fetchFor(propertyName).fetchModel(), reflectedValue);
        } else if (PropertyTypeDeterminator.isCollectional(type, propertyName)) {
            final Class<?> collectionType = Finder.findFieldByName(type, propertyName).getType();
            final boolean isSet = Set.class.isAssignableFrom(collectionType);
            final boolean isList = List.class.isAssignableFrom(collectionType);
            final boolean isStringElem = String.class.isAssignableFrom(propertyType);
            final boolean isLongElem = Long.class.isAssignableFrom(propertyType);
            if (!isSet && !isList || !isStringElem && !isLongElem) {
                throw new UnsupportedOperationException(String.format("Unsupported conversion to [%s@%s] from reflected value [%s] of collectional type [%s] with [%s] elements. Only [Set / List] of [String / Long] elements are supported.", propertyName, type.getSimpleName(), reflectedValue, collectionType.getSimpleName(), propertyType.getSimpleName()));
            }
            final List<Object> list = (ArrayList<Object>) reflectedValue;
            final Stream<Object> stream = list.stream().map( 
                item -> item == null ? null : 
                    isStringElem ? item.toString() : extractLongValueFrom(item)
            );
            return stream.collect(Collectors.toCollection(isSet ? LinkedHashSet::new : ArrayList::new));
        } else if (EntityUtils.isString(propertyType)) {
            return reflectedValue;
        } else if (Integer.class.isAssignableFrom(propertyType)) {
            return reflectedValue;
        } else if (EntityUtils.isBoolean(propertyType)) {
            return reflectedValue;
        } else if (EntityUtils.isDate(propertyType)) {
            return reflectedValue instanceof Integer ? new Date(((Integer) reflectedValue).longValue()) : new Date((Long) reflectedValue);
        } else if (BigDecimal.class.isAssignableFrom(propertyType)) {
            final MapTo mapTo = AnnotationReflector.getPropertyAnnotation(MapTo.class, type, propertyName);
            final CritOnly critOnly = AnnotationReflector.getPropertyAnnotation(CritOnly.class, type, propertyName);
            final Integer propertyScale = mapTo != null && mapTo.scale() >= 0 ? ((int) mapTo.scale())
                    : (critOnly != null && critOnly.scale() >= 0 ? ((int) critOnly.scale()) : 2)/* default value from Hibernate */;

            if (reflectedValue instanceof Integer) {
                return new BigDecimal((Integer) reflectedValue).setScale(propertyScale, RoundingMode.HALF_UP);
            } else if (reflectedValue instanceof Long) {
                return BigDecimal.valueOf((Long) reflectedValue).setScale(propertyScale, RoundingMode.HALF_UP);
            } else if (reflectedValue instanceof BigInteger) {
                return new BigDecimal((BigInteger) reflectedValue).setScale(propertyScale, RoundingMode.HALF_UP);
            } else if (reflectedValue instanceof BigDecimal) {
                return ((BigDecimal) reflectedValue).setScale(propertyScale, RoundingMode.HALF_UP);
            } else {
                throw new IllegalStateException("Unknown number type for 'reflectedValue'.");
            }
        } else if (Money.class.isAssignableFrom(propertyType)) {
            final Map<String, Object> map = (Map<String, Object>) reflectedValue;

            final BigDecimal amount = new BigDecimal(map.get("amount").toString());
            final String currencyStr = (String) map.get("currency");
            final Integer taxPercentage = (Integer) map.get("taxPercent");

            if (taxPercentage == null) {
                if (StringUtils.isEmpty(currencyStr)) {
                    return new Money(amount);
                } else {
                    return new Money(amount, Currency.getInstance(currencyStr));
                }
            } else {
                if (StringUtils.isEmpty(currencyStr)) {
                    return new Money(amount, taxPercentage,  Currency.getInstance(getDefault()));
                } else {
                    return new Money(amount, taxPercentage, Currency.getInstance(currencyStr));
                }
            }

        } else if (Colour.class.isAssignableFrom(propertyType)) {
            final Map<String, Object> map = (Map<String, Object>) reflectedValue;
            final String hashlessUppercasedColourValue = (String) map.get("hashlessUppercasedColourValue");
            return hashlessUppercasedColourValue == null ? null : new Colour(hashlessUppercasedColourValue);
        } else if (Hyperlink.class.isAssignableFrom(propertyType)) {
            final Map<String, Object> map = (Map<String, Object>) reflectedValue;
            final String linkValue = (String) map.get("value");
            return linkValue == null ? null : new Hyperlink(linkValue);
        } else if (Long.class.isAssignableFrom(propertyType)) {
            return extractLongValueFrom(reflectedValue);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported conversion to [%s@%s] from reflected value [%s] of type [%s].", propertyName, type.getSimpleName(), reflectedValue, propertyType.getSimpleName()));
        }
    }

    /**
     * Extracts from number-like <code>reflectedValue</code> its {@link Long} representation.
     * 
     * @param reflectedValue
     * @return
     */
    private static Long extractLongValueFrom(final Object reflectedValue) {
        if (reflectedValue instanceof Integer) {
            return ((Integer) reflectedValue).longValue();
        } else if (reflectedValue instanceof Long) {
            return (Long) reflectedValue;
        } else if (reflectedValue instanceof BigInteger) {
            return ((BigInteger) reflectedValue).longValue();
        } else {
            throw new IllegalStateException(String.format("Unknown number type for 'reflectedValue' (%s) - can not convert to Long.", reflectedValue));
        }
    }

    /**
     * If one of the composite key members is of type {@link PropertyDescriptor} then the search-by value needs to be modified by converting the provided string representation
     * for property descriptors to the required form.
     * 
     * @param propertyType
     * @param entityPropertyType
     * @param compositeKeyAsString
     * @return
     */
    private static String buildSearchByValue(final Class<?> propertyType, final Class<AbstractEntity<?>> entityPropertyType, final String compositeKeyAsString) {
        // if one or more composite key members are of type ProperyDescriptor then those values need to be converted to a DB aware representation
        // regrettable this process is error prone due to a potential use of the key member separator as part of property titles...
        final List<Field> keyMembers = Finder.getKeyMembers(entityPropertyType);
        final boolean hasPropDescKeyMembers = keyMembers.stream().filter(f -> EntityUtils.isPropertyDescriptor(f.getType())).findFirst().map(f -> true).orElse(false);
        // do we have key members of type PropertyDescriptor
        if (!hasPropDescKeyMembers) {
            return compositeKeyAsString;
        } else {
            final StringBuilder convertedKeyValue = new StringBuilder();
            String keyValues = compositeKeyAsString; // mutable!
            final String keyMemberSeparator = Reflector.getKeyMemberSeparator((Class<? extends AbstractEntity<DynamicEntityKey>>) propertyType);
            for (int index = 0; index < keyMembers.size(); index++) {
                final boolean isLastKeyMember = index == keyMembers.size() - 1;
                final int separatorIndex = isLastKeyMember ? keyValues.length() : keyValues.indexOf(keyMemberSeparator);
                // there must be exactly keyMembers.size() - 1 separators
                // but just in case let's validate the found index
                if (separatorIndex < 0) {
                    throw new IllegalArgumentException(format("Composite key value [%s] must have [%s] separators.", compositeKeyAsString, keyMembers.size() - 1));
                }
                final String value = isLastKeyMember ? keyValues : keyValues.substring(0, separatorIndex);
                keyValues = isLastKeyMember ? "" : keyValues.substring(separatorIndex + 1);
                
                final Field field = keyMembers.get(index);
                if (EntityUtils.isPropertyDescriptor(field.getType())) {
                    final Class<AbstractEntity<?>> enclosingEntityType = (Class<AbstractEntity<?>>) AnnotationReflector.getPropertyAnnotation(IsProperty.class, entityPropertyType, field.getName()).value();
                    final Optional<PropertyDescriptor<AbstractEntity<?>>> propDesc = extractPropertyDescriptor(value, enclosingEntityType);
                    if (!propDesc.isPresent()) {
                        throw new IllegalArgumentException(format("Could not convert value [%s] to a property descriptor within type [%s].", value, enclosingEntityType.getSimpleName()));
                    }
                    convertedKeyValue.append(propDesc.get().toString());
                } else {
                    convertedKeyValue.append(value);
                }
                
                if (index < keyMembers.size() - 1) {
                    convertedKeyValue.append(keyMemberSeparator);
                }
            }
            return convertedKeyValue.toString();
        }
    }

    /**
     * Tries to extract a property descriptor from a given string value.
     * 
     * @param reflectedValue
     * @param matcher
     * @return
     */
    private static Optional<PropertyDescriptor<AbstractEntity<?>>> extractPropertyDescriptor(final String value, final Class<AbstractEntity<?>> enclosingEntityType) {
        final List<PropertyDescriptor<AbstractEntity<?>>> allPropertyDescriptors = Finder.getPropertyDescriptors(enclosingEntityType);
        final PojoValueMatcher<PropertyDescriptor<AbstractEntity<?>>> matcher = new PojoValueMatcher<>(allPropertyDescriptors, AbstractEntity.KEY, allPropertyDescriptors.size());
        final List<PropertyDescriptor<AbstractEntity<?>>> matchedPropertyDescriptors = matcher.findMatches(value);
        if (matchedPropertyDescriptors.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(matchedPropertyDescriptors.get(0));
    }

    /**
     * Restores the holder of modified properties into the map [propertyName; webEditorSpecificValue].
     *
     * @param envelope
     * @return
     */
    public static Map<String, Object> restoreModifiedPropertiesHolderFrom(final Representation envelope, final RestServerUtil restUtil) {
        return (Map<String, Object>) restUtil.restoreJSONMap(envelope);
    }

    /**
     * Restores the holder of context and criteria entity.
     *
     * @param envelope
     * @return
     */
    public static CentreContextHolder restoreCentreContextHolder(final Representation envelope, final RestServerUtil restUtil) {
        return restUtil.restoreJSONEntity(envelope, CentreContextHolder.class);
    }

    /**
     * Restores the {@link Result} from JSON envelope.
     *
     * @param envelope
     * @return
     */
    public static Result restoreJSONResult(final Representation envelope, final RestServerUtil restUtil) {
        return restUtil.restoreJSONResult(envelope);
    }

    /**
     * Restores the holder of saving information (modified props + centre context, if any).
     *
     * @param envelope
     * @return
     */
    public static SavingInfoHolder restoreSavingInfoHolder(final Representation envelope, final RestServerUtil restUtil) {
        return restUtil.restoreJSONEntity(envelope, SavingInfoHolder.class);
    }
    
    /**
     * Just saves the entity.
     *
     * @param entity
     * @param continuations -- continuations of the entity to be used during saving
     * 
     * @return
     */
    public T save(final T entity, final Map<String, IContinuationData> continuations) {
        return saveWithContinuations(entity, continuations, (CommonEntityDao<T>) this.co);
    }

    /**
     * Saves the <code>entity</code> with its <code>continuations</code>.
     * 
     * In case where warnings exist, <code>continuations</code> should include respective warnings acknowledgement continuation if it was accepted by the user.
     * Could throw continuations exceptions, 'no changes' exception or validation exceptions.
     *
     * @param entity
     * @param continuations -- continuations of the entity to be used during saving
     * 
     * @return
     */
    private static <T extends AbstractEntity<?>> T saveWithContinuations(final T entity, final Map<String, IContinuationData> continuations, final CommonEntityDao<T> co) {
        final boolean continuationsPresent = !continuations.isEmpty();
        
        // iterate over properties in search of the first invalid one (without required checks)
        final java.util.Optional<Result> firstFailure = entity.nonProxiedProperties()
        .filter(mp -> mp.getFirstFailure() != null)
        .findFirst().map(mp -> mp.getFirstFailure());
        
        // returns first failure if exists or successful result if there was no failure.
        final Result isValid = firstFailure.isPresent() ? firstFailure.get() : Result.successful(entity);
        
        if (isValid.isSuccessful()) {
            final String acknowledgementContinuationName = "_acknowledgedForTheFirstTime";
            if (entity.hasWarnings() && (!continuationsPresent || continuations.get(acknowledgementContinuationName) == null)) {
                throw new NeedMoreData("Warnings need acknowledgement", AcknowledgeWarnings.class, acknowledgementContinuationName);
            } else if (entity.hasWarnings() && continuationsPresent && continuations.get(acknowledgementContinuationName) != null) {
                entity.nonProxiedProperties().forEach(prop -> prop.clearWarnings());
            }
        }
        
        // 1) non-persistent entities should always be saved (isDirty will always be true)
        // 2) persistent but not persisted (new) entities should always be saved (isDirty will always be true)
        // 3) persistent+persisted+dirty (by means of dirty properties existence) entities should always be saved
        // 4) persistent+persisted+notDirty+inValid entities should always be saved: passed to companion 'save' method to process validation errors in domain-driven way by companion object itself
        // 5) persistent+persisted+notDirty+valid entities saving should be skipped
        if (!entity.isDirty() && entity.isValid().isSuccessful()) {
            throw Result.failure("There are no changes to save.");
        }
        
        if (continuationsPresent) {
            co.setMoreData(continuations);
        } else {
            co.clearMoreData();
        }
        final T saved = co.save(entity);
        if (continuationsPresent) {
            co.clearMoreData();
        }
        return saved;
    }
    
    /**
     * Deletes the entity.
     *
     * @param entityId
     */
    public void delete(final Long entityId) {
        co.delete(entityFactory.newEntity(entityType, entityId));
    }

    /**
     * Constructs the entity from the client envelope.
     * <p>
     * The envelope contains special version of entity called 'modifiedPropertiesHolder' which has only modified properties and potentially some custom stuff with '@' sign as the
     * prefix. All custom properties will be disregarded, but can be used later from the returning map.
     * <p>
     * All normal properties will be applied in 'validationPrototype'.
     *
     * @param envelope
     * @return applied validationPrototype and modifiedPropertiesHolder map
     */
    public Pair<T, Map<String, Object>> constructEntity(final Map<String, Object> modifiedPropertiesHolder, final Long id) {
        return constructEntity(modifiedPropertiesHolder, createValidationPrototype(id), getCompanionFinder());
    }

    /**
     * Constructs the entity from the client envelope.
     * <p>
     * The envelope contains special version of entity called 'modifiedPropertiesHolder' which has only modified properties and potentially some custom stuff with '@' sign as the
     * prefix. All custom properties will be disregarded, but can be used later from the returning map.
     * <p>
     * All normal properties will be applied in 'validationPrototype'.
     *
     * @param envelope
     * @return applied validationPrototype and modifiedPropertiesHolder map
     */
    public Pair<T, Map<String, Object>> constructEntity(
            final Map<String, Object> modifiedPropertiesHolder,
            final CentreContext<T, AbstractEntity<?>> centreContext,
            final String chosenProperty,
            final Long compoundMasterEntityId,
            final AbstractEntity<?> masterContext, final int tabCount) {

        logger.debug(EntityResource.tabs(tabCount) + "constructEntity: started.");
        final Object arrivedIdVal = modifiedPropertiesHolder.get(AbstractEntity.ID);
        final Long id = arrivedIdVal == null ? null : Long.parseLong(arrivedIdVal + "");

        final T validationPrototypeWithContext = createValidationPrototypeWithContext(id, centreContext, chosenProperty, compoundMasterEntityId, masterContext);
        logger.debug(EntityResource.tabs(tabCount) + "constructEntity: validationPrototypeWithContext.");
        final Pair<T, Map<String, Object>> constructed = constructEntity(modifiedPropertiesHolder, validationPrototypeWithContext, getCompanionFinder());
        logger.debug(EntityResource.tabs(tabCount) + "constructEntity: finished.");
        return constructed;
    }

    /**
     * Constructs the entity from the client envelope.
     * <p>
     * The envelope contains special version of entity called 'modifiedPropertiesHolder' which has only modified properties and potentially some custom stuff with '@' sign as the
     * prefix. All custom properties will be disregarded, but can be used later from the returning map.
     * <p>
     * All normal properties will be applied in 'validationPrototype'.
     *
     * @return applied validationPrototype and modifiedPropertiesHolder map
     */
    private static <M extends AbstractEntity<?>> Pair<M, Map<String, Object>> constructEntity(final Map<String, Object> modifiedPropertiesHolder, final M validationPrototype, final ICompanionObjectFinder companionFinder) {
        return new Pair<>(apply(modifiedPropertiesHolder, validationPrototype, companionFinder), modifiedPropertiesHolder);
    }

    /**
     * Constructs the entity from the client envelope.
     * <p>
     * The envelope contains special version of entity called 'modifiedPropertiesHolder' which has only modified properties and potentially some custom stuff with '@' sign as the
     * prefix. All custom properties will be disregarded, but can be used later from the returning map.
     * <p>
     * All normal properties will be applied in 'validationPrototype'.
     *
     * @return applied validationPrototype and modifiedPropertiesHolder map
     */
    public Pair<T, Map<String, Object>> constructEntity(final Map<String, Object> modifiedPropertiesHolder) {
        final Object arrivedIdVal = modifiedPropertiesHolder.get(AbstractEntity.ID);
        final Long id = arrivedIdVal == null ? null : Long.parseLong(arrivedIdVal + "");

        return constructEntity(modifiedPropertiesHolder, id);
    }

    public EntityFactory entityFactory() {
        return entityFactory;
    }

    /**
     * This method wraps the function of representation creation to handle properly <b>undesired</b> server errors.
     * <p>
     * Please note that all <b>expected</b> exceptional situations should be handled inside the respective 'representationCreator' and one should not rely on this method for such
     * errors.
     *
     * @param representationCreator
     * @return
     */
    public static Representation handleUndesiredExceptions(final Response response, final Supplier<Representation> representationCreator, final RestServerUtil restUtil) {
        try {
            return representationCreator.get();
        } catch (final Exception undesiredEx) {
            logger.error(undesiredEx.getMessage(), undesiredEx);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            return restUtil.errorJSONRepresentation(undesiredEx);
        }
    }
}
