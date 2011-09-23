package ua.com.fielden.platform.entity.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.annotation.mutator.BeforeChange;
import ua.com.fielden.platform.entity.annotation.mutator.ClassParam;
import ua.com.fielden.platform.entity.annotation.mutator.DateParam;
import ua.com.fielden.platform.entity.annotation.mutator.DateTimeParam;
import ua.com.fielden.platform.entity.annotation.mutator.DblParam;
import ua.com.fielden.platform.entity.annotation.mutator.Handler;
import ua.com.fielden.platform.entity.annotation.mutator.IntParam;
import ua.com.fielden.platform.entity.annotation.mutator.MoneyParam;
import ua.com.fielden.platform.entity.annotation.mutator.StrParam;
import ua.com.fielden.platform.entity.factory.IMetaPropertyFactory;
import ua.com.fielden.platform.entity.validation.DomainValidationConfig;
import ua.com.fielden.platform.entity.validation.EntityExistsValidator;
import ua.com.fielden.platform.entity.validation.FinalValidator;
import ua.com.fielden.platform.entity.validation.GreaterOrEqualValidator;
import ua.com.fielden.platform.entity.validation.IBeforeChangeEventHandler;
import ua.com.fielden.platform.entity.validation.MaxLengthValidator;
import ua.com.fielden.platform.entity.validation.MaxValueValidator;
import ua.com.fielden.platform.entity.validation.NotEmptyValidator;
import ua.com.fielden.platform.entity.validation.NotNullValidator;
import ua.com.fielden.platform.entity.validation.RangePropertyValidator;
import ua.com.fielden.platform.entity.validation.annotation.EntityExists;
import ua.com.fielden.platform.entity.validation.annotation.GeProperty;
import ua.com.fielden.platform.entity.validation.annotation.GreaterOrEqual;
import ua.com.fielden.platform.entity.validation.annotation.LeProperty;
import ua.com.fielden.platform.entity.validation.annotation.Max;
import ua.com.fielden.platform.entity.validation.annotation.ValidationAnnotation;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.types.Money;
import ua.com.fielden.platform.utils.StringConverter;

import com.google.inject.Injector;

/**
 * Base implementation for {@link IMetaPropertyFactory}.
 *
 * @author TG Team
 *
 */
public abstract class AbstractMetaPropertyFactory implements IMetaPropertyFactory {

    protected final NotNullValidator notNullValidator = new NotNullValidator();
    protected final NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
    protected final FinalValidator finalValidator = new FinalValidator();
    protected final Map<Class<? extends AbstractEntity>, EntityExistsValidator> entityExistsValidators = Collections.synchronizedMap(new HashMap<Class<? extends AbstractEntity>, EntityExistsValidator>());
    protected final Map<Integer, GreaterOrEqualValidator> greaterOrEqualsValidators = Collections.synchronizedMap(new HashMap<Integer, GreaterOrEqualValidator>());
    protected final Map<Integer, MaxLengthValidator> maxLengthValidators = Collections.synchronizedMap(new HashMap<Integer, MaxLengthValidator>());
    protected final Map<Integer, MaxValueValidator> maxValueValidators = Collections.synchronizedMap(new HashMap<Integer, MaxValueValidator>());
    protected final Map<Class<?>, Map<String, RangePropertyValidator>> geRangeValidators = Collections.synchronizedMap(new HashMap<Class<?>, Map<String, RangePropertyValidator>>());
    protected final Map<Class<?>, Map<String, RangePropertyValidator>> leRangeValidators = Collections.synchronizedMap(new HashMap<Class<?>, Map<String, RangePropertyValidator>>());
    // type, property, array of handlers
    protected final Map<Class<?>, Map<String, IBeforeChangeEventHandler[]>> beforeChangeEventHandlers = Collections.synchronizedMap(new HashMap<Class<?>, Map<String, IBeforeChangeEventHandler[]>>());

    private Injector injector;

    protected final DomainValidationConfig domainConfig;
    protected final DomainMetaPropertyConfig domainMetaConfig;

    public AbstractMetaPropertyFactory(final DomainValidationConfig domainConfig, final DomainMetaPropertyConfig domainMetaConfig) {
	this.domainConfig = domainConfig;
	this.domainMetaConfig = domainMetaConfig;
    }

    @Override
    public IBeforeChangeEventHandler[] create(//
    final Annotation annotation,//
    final AbstractEntity<?> entity,//
    final String propertyName,//
    final Class<?> propertyType) throws Exception {
	if (injector == null) {
	    throw new IllegalStateException("Meta-property factory is not fully initialised -- injector is missing");
	}
	// identify the type of annotation
	ValidationAnnotation value = null;
	for (final ValidationAnnotation validationAnnotation : ValidationAnnotation.values()) {
	    if (validationAnnotation.getType().equals(annotation.annotationType())) {
		value = validationAnnotation;
	    }
	}
	// check whether it can be recognised as a valid annotation permitted for validation purpose
	if (value == null) {
	    throw new IllegalArgumentException("Unrecognised validation annotation has been encountered.");
	}
	// try to instantiate validator
	switch (value) {
	case NOT_NULL:
	    return new IBeforeChangeEventHandler[] { notNullValidator };
	case NOT_EMPTY:
	    return new IBeforeChangeEventHandler[] { notEmptyValidator };
	case ENTITY_EXISTS:
	    return new IBeforeChangeEventHandler[] { createEntityExists((EntityExists) annotation) };
	case FINAL:
	    return new IBeforeChangeEventHandler[] { finalValidator };
	case GREATER_OR_EQUAL:
	    return new IBeforeChangeEventHandler[] { createGreaterOrEqualValidator(((GreaterOrEqual) annotation).value()) };
	case LE_PROPETY:
	    return new IBeforeChangeEventHandler[] { createLePropertyValidator(entity, propertyName, ((LeProperty) annotation).value()) };
	case GE_PROPETY:
	    return new IBeforeChangeEventHandler[] { createGePropertyValidator(entity, ((GeProperty) annotation).value(), propertyName) };
	case MAX:
	    if (Number.class.isAssignableFrom(propertyType) || double.class == propertyType || int.class == propertyType) {
		return new IBeforeChangeEventHandler[] { createMaxValueValidator(((Max) annotation).value()) };
	    } else if (String.class == propertyType) {
		return new IBeforeChangeEventHandler[] { createMaxLengthValidator(((Max) annotation).value()) };
	    }
	    throw new RuntimeException("Property " + propertyName + " of type " + propertyType.getName() + " does not support Max validation.");
	case DOMAIN:
	    return new IBeforeChangeEventHandler[] { domainConfig.getValidator(entity.getType(), propertyName) };
	case BEFORE_CHANGE:
	    return createBeforeChange(entity, propertyName, (BeforeChange) annotation);
	default:
	    throw new IllegalArgumentException("Unsupported validation annotation has been encountered.");
	}
    }

    /**
     * Creates validators declared as BCE handlers.
     *
     * @param entity
     * @param propertyName
     * @param annotation
     * @return
     */
    private IBeforeChangeEventHandler[] createBeforeChange(final AbstractEntity<?> entity, final String propertyName, final BeforeChange annotation) {
	// TODO Implement creation of BCE handlers
	// 0. If the cache contains handlers for the entity and property then return them. Otherwise, step 1.
	final Map<String, IBeforeChangeEventHandler[]> typeHandlers = beforeChangeEventHandlers.get(entity.getType());
	if (typeHandlers != null && typeHandlers.containsKey(propertyName)) {
	    return typeHandlers.get(propertyName);
	}
	// 1. BeforeChange annotations has property <code>value</code>, which is an array of annotations Handler.
	//    Need to iterate over all these handler-annotations for instantiation of event handlers.
	final Handler[] handlerDeclarations = annotation.value();
	// 2. For each event handler do
	//    2.1 Instantiate a handler using injector for property <code>value</code>, which contains handler's class declaration
	//    2.2 For each value in arrays <code>non_ordinary</code>, <code>integer</code>, <code>str</code>, <code>dbl</code>, <code>date</code>, <code>date_time</code>, <code>money</code>
	//	  initialise handler's parameters.
	final IBeforeChangeEventHandler[] handlers = new IBeforeChangeEventHandler[handlerDeclarations.length];
	for (int index = 0; index < handlerDeclarations.length; index++) {
	    final Handler hd = handlerDeclarations[index];
	    final IBeforeChangeEventHandler handler = injector.getInstance(hd.value());
	    initNonOrdinaryHandlerParameters(entity, hd, handler);
	    initIntegerHandlerParameters(entity, hd, handler);
	    initDoubleHandlerParameters(entity, hd, handler);
	    initStringHandlerParameters(entity, hd, handler);
	    initDateHandlerParameters(entity, hd, handler);
	    initDateTimeHandlerParameters(entity, hd, handler);
	    initMoneyHandlerParameters(entity, hd, handler);

	    handlers[index] = handler;
	}
	// 3. Cache all instantiated handlers against the entity and property.
	if (typeHandlers == null) { // currently there are no handlers associate with any of the type properties
	    // the use of LinkedHashMap is critical in order to maintain the order of BCE handlers
	    final Map<String, IBeforeChangeEventHandler[]> newTypeHandlers = new LinkedHashMap<String, IBeforeChangeEventHandler[]>();
	    beforeChangeEventHandlers.put(entity.getType(), newTypeHandlers);
	}
	beforeChangeEventHandlers.get(entity.getType()).put(propertyName, handlers);

	// 4. Return an array of instantiated handlers.
	return handlers;
    }

    /**
     * Initialises non-ordinary handler parameters as provided in {@link Handler#non_ordinary()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initNonOrdinaryHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final ClassParam param : hd.non_ordinary()) {
	    final Class<?> type = param.value();
	    final Object value = injector.getInstance(type);
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, value);
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises integer handler parameters as provided in {@link Handler#integer()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initIntegerHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final IntParam param : hd.integer()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, param.value());
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises double handler parameters as provided in {@link Handler#dbl()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initDoubleHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final DblParam param : hd.dbl()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, param.value());
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises {@link String} handler parameters as provided in {@link Handler#str()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initStringHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final StrParam param : hd.str()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, param.value());
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises {@link Date} handler parameters as provided in {@link Handler#date()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initDateHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final DateParam param : hd.date()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, StringConverter.toDate(param.value()));
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises {@link DateTime} handler parameters as provided in {@link Handler#date_time()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initDateTimeHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final DateTimeParam param : hd.date_time()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, StringConverter.toDateTime(param.value()));
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }

    /**
     * Initialises {@link Money} handler parameters as provided in {@link Handler#money()}.
     *
     * @param entity
     * @param hd
     * @param handler
     */
    private void initMoneyHandlerParameters(final AbstractEntity<?> entity, final Handler hd, final IBeforeChangeEventHandler handler) {
	for (final MoneyParam param : hd.money()) {
	    final Field paramField = Finder.getFieldByName(handler.getClass(), param.name());
	    paramField.setAccessible(true);
	    try {
		paramField.set(handler, StringConverter.toMoney(param.value()));
	    } catch (final Exception ex) {
		throw new IllegalStateException("Could not initialise parameter " + param.name() + "@" + handler.getClass().getName(), ex);
	    }
	}
    }


    private IBeforeChangeEventHandler createGePropertyValidator(final AbstractEntity<?> entity, final String[] lowerBoundaryProperties, final String upperBoundaryProperty) {
	if (geRangeValidators.get(entity.getType()) == null) {
	    geRangeValidators.put(entity.getType(), Collections.synchronizedMap(new HashMap<String, RangePropertyValidator>()));
	}
	final Map<String, RangePropertyValidator> propertyValidators = geRangeValidators.get(entity.getType());
	if (propertyValidators.get(upperBoundaryProperty) == null) {
	    propertyValidators.put(upperBoundaryProperty, new RangePropertyValidator(lowerBoundaryProperties, true));
	}
	return propertyValidators.get(upperBoundaryProperty);
    }

    private IBeforeChangeEventHandler createLePropertyValidator(final AbstractEntity<?> entity, final String lowerBoundaryProperty, final String[] upperBoundaryProperties) {
	if (leRangeValidators.get(entity.getType()) == null) {
	    leRangeValidators.put(entity.getType(), Collections.synchronizedMap(new HashMap<String, RangePropertyValidator>()));
	}
	final Map<String, RangePropertyValidator> propertyValidators = leRangeValidators.get(entity.getType());
	if (propertyValidators.get(lowerBoundaryProperty) == null) {
	    propertyValidators.put(lowerBoundaryProperty, new RangePropertyValidator(upperBoundaryProperties, false));
	}
	return propertyValidators.get(lowerBoundaryProperty);
    }

    private IBeforeChangeEventHandler createGreaterOrEqualValidator(final Integer key) {
	if (!greaterOrEqualsValidators.containsKey(key)) {
	    greaterOrEqualsValidators.put(key, new GreaterOrEqualValidator(key));
	}
	return greaterOrEqualsValidators.get(key);
    }

    private IBeforeChangeEventHandler createMaxLengthValidator(final Integer key) {
	if (!maxLengthValidators.containsKey(key)) {
	    maxLengthValidators.put(key, new MaxLengthValidator(key));
	}
	return maxLengthValidators.get(key);
    }

    private IBeforeChangeEventHandler createMaxValueValidator(final Integer key) {
	if (!maxValueValidators.containsKey(key)) {
	    maxValueValidators.put(key, new MaxValueValidator(key));
	}
	return maxValueValidators.get(key);
    }

    protected abstract IBeforeChangeEventHandler createEntityExists(final EntityExists anotation);

    @Override
    public IMetaPropertyDefiner create(final AbstractEntity<?> entity, final String propertyName) throws Exception {
	return domainMetaConfig.getDefiner(entity.getType(), propertyName);
    }

    @Override
    public void setInjector(final Injector injector) {
	this.injector = injector;
    }
}
