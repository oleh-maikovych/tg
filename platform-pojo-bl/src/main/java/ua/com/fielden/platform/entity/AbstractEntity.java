package ua.com.fielden.platform.entity;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ua.com.fielden.platform.entity.annotation.Calculated;
import ua.com.fielden.platform.entity.annotation.CompositeKeyMember;
import ua.com.fielden.platform.entity.annotation.Dependent;
import ua.com.fielden.platform.entity.annotation.DescReadonly;
import ua.com.fielden.platform.entity.annotation.DescRequired;
import ua.com.fielden.platform.entity.annotation.DescTitle;
import ua.com.fielden.platform.entity.annotation.Invisible;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.KeyReadonly;
import ua.com.fielden.platform.entity.annotation.KeyTitle;
import ua.com.fielden.platform.entity.annotation.KeyType;
import ua.com.fielden.platform.entity.annotation.MapTo;
import ua.com.fielden.platform.entity.annotation.Observable;
import ua.com.fielden.platform.entity.annotation.Readonly;
import ua.com.fielden.platform.entity.annotation.Required;
import ua.com.fielden.platform.entity.annotation.Title;
import ua.com.fielden.platform.entity.annotation.UpperCase;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.factory.IMetaPropertyFactory;
import ua.com.fielden.platform.entity.ioc.EntityModule;
import ua.com.fielden.platform.entity.ioc.ObservableMutatorInterceptor;
import ua.com.fielden.platform.entity.meta.IMetaPropertyDefiner;
import ua.com.fielden.platform.entity.meta.MetaProperty;
import ua.com.fielden.platform.entity.meta.PropertyDescriptor;
import ua.com.fielden.platform.entity.validation.DomainValidationConfig;
import ua.com.fielden.platform.entity.validation.IBeforeChangeEventHandler;
import ua.com.fielden.platform.entity.validation.annotation.DomainValidation;
import ua.com.fielden.platform.entity.validation.annotation.NotNull;
import ua.com.fielden.platform.entity.validation.annotation.ValidationAnnotation;
import ua.com.fielden.platform.error.Result;
import ua.com.fielden.platform.reflection.AnnotationReflector;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.reflection.PropertyTypeDeterminator;
import ua.com.fielden.platform.reflection.Reflector;
import ua.com.fielden.platform.swing.review.DynamicEntityQueryCriteria;
import ua.com.fielden.platform.utils.PropertyChangeSupportEx;
import ua.com.fielden.platform.utils.PropertyChangeSupportEx.PropertyChangeOrIncorrectAttemptListener;

import com.google.inject.Inject;

/**
 * <h3>General Info</h3>
 *
 * This class serves as a common parent for all entity types. It is envisaged that all entity classes should have a surrogate key <code>id</code>, business key <code>key</code> and
 * the description <code>desc</code>.
 *
 * Also, this class provides a generic implementation for methods {@link #hashCode()}, {@link #equals(Object)()}, {@link #toString()} and {@link #compareTo(AbstractEntity)}, which
 * is based on the used of the business key and thus should be suitable in most cases.
 *
 * Composite key with no reference to other entities can be easily mapped by Hibernate as a component. However, Hibernate does not permit many-to-one associations as part of the
 * component mapping. Thus, the need for special treatment of composite keys with such associations.
 *
 * Let's introduce a use case. Consider that there is a purchase order (PO) entity and its items. The business key for PO is just a string reflecting its number. The business key
 * for PO item is composite consisting of the sequential number (i.e. 1, 2, 3) representing the order of items associated with a PO, and a PO itself. A possible implementation of
 * such key could be as follows:
 * <p>
 *
 * <pre>
 * public class PoItemKey {
 * 	private Integer number;
 * 	private PurchaserOrder po;
 * 	...
 * }
 * </pre>
 *
 * <p>
 * And the PO item class would have this class as a <code>key</code> property:
 * <p>
 *
 * <pre>
 * public class PoItem {
 * 	private PoItemKey key;
 * 	...
 * }
 * </pre>
 *
 * <p>
 * Unfortunately, there is no way to map property PoItem.key with Hibernate. So what needs to be done is this.<br/>
 * <p>
 * First, introduce public inner class PoItemKey within PoItem, which implements {@link Comparable} and methods {@link #equals(Object)}, {@link #hashCode()} based on properties
 * from the enclosing class PoItem:<br/>
 *
 * <pre>
 * class PoItemKey implements Comparable[PoItemKey] {
 * 	 &#064;Override public int hashCode() {
 * 		return getPurchaseOrder().hashCode() * 23 + getNumber().hashCode() * 13;
 * 	}
 * 	 &#064;Override public boolean equals(final Object obj) {
 * 		...
 * 		final PoItemKey cmpTo = (PoItemKey) obj;
 * 		return getPurchaseOrder().equals(cmpTo.getPurchaseOrder()) &amp;&amp;
 *                     getNumber().equals(cmpTo.getNumber());
 * 	}
 * 	 &#064;Override public int compareTo(final PoItemKey cmpTo) {
 *              if (getPurchaseOrder().equals(cmpTo.getPurchaseOrder())) {
 *                 return getNumber().compareTo(cmpTo.getNumber());
 *              }  else {
 *                 return getPurchaseOrder().compareTo(cmpTo.getPurchaseOrder());
 *              }
 * 	}
 *      ...
 * }
 * </pre>
 *
 * <p>
 * The PoItem class itself should need to have properties <code>purchaseOrder</code> and <code>number</>, which can be easily mapped by Hibernate.
 * <p>
 * Please note that class {@link DynamicEntityKey} automates implementation of entities with composite keys.
 * <p>
 *
 * <h3>Additional Functionality</h3>
 *
 * AbstractEntity supports meta properties, property change listeners and validation.
 * <p>
 * For any field to be recognised as property that requires meta-information annotation {@link IsProperty} should be used.
 * Meta-properties are represented by {@link MetaProperty}, which are instantiated by {@link IMetaPropertyFactory}.
 * A concrete {@link IMetaPropertyFactory} implementation can be set or injected using setter {@link #setMetaPropertyFactory(IMetaPropertyFactory)}.
 * <p>
 * At this stage meta-property instantiation also ensures association of the created instance with property validators, which are defined by annotations on the corresponding setter that are listed by enumeration {@link ValidationAnnotation}.
 * An instance of {@link IMetaPropertyFactory} is responsible for instantiation of validators, which implement {@link IBeforeChangeEventHandler} interface.
 * <p>
 * In order for validators to perform validation upon an attempt to set a property value, setters should be intercepted.
 * Intercepter {@link ValidationMutatorInterceptor} was implemented specifically to handle validation of values being passed into setters.
 * Its implementation uses validators associated with property during meta-property instantiation.
 *
 * However, entity instance should be created with Guice intercepter provided with a module configured to bind this intercepter.
 * <p>
 * A similar situation is with support of property change event handling. Any setter annotated with {@link Observable} should be intercepted by {@link ObservableMutatorInterceptor},
 * which can be achieved by using appropriately configured Guice module.
 *
 * Please refer {@link EntityModule} for more details.
 *
 * <h3>Property mutators</h3>
 * The <i>property</i> specification as defined in JavaBeans does not cover fully the needs identified by our team for working with business entities where properties have loosely coupled validation logic and change observation.
 * Also, the approach taken in JavaBeans does not provide the possibility to follow [http://en.wikipedia.org/wiki/Fluent_interface fluent interface] programming approach.
 * Thus, Trident Genesis introduces its own notion of property, property mutators and accessors.
 * <p>
 * <p>
 * In TG there are two main kinds of properties:
 * <ul>
 *  <li>Simple Property -- such as Double, String, Entity etc.
 *  <li>Collectional Property -- Collection[String], List[Entity] etc.
 * </ul>
 * The treatment for accessors of these two kinds are identical. However, mutators are quite different.
 *  <p>
 * Let's denote [property] as a placeholder for the property name. The property accessor is a method with no parameters and name '''get[property]'''.
 * At least at this stage it does not support any annotations and its use goes unnoticeably. It is important, however, to note that accessor implementation for collectional properties has to return immutable collection, which is purely developer's responsibility.
 * Otherwise, any modification to the property would also happen unnoticeably and would most likely violate TG principles of validation and observation. Also, this is a good programming practice in general, enforced in the TG programming model.
 * <p>
 * The mutator concept is a different beast, and should be introduced separately for simple and collectional properties.
 * <ul>
 *   <li>Mutator for a simple property -- a method with name '''set[property]''' and one parameter matching the type of the field representing the property.
 *
 *       It may and usually should be annotated with {@link Observable} to ensure observation of the property change. And may have a number of validation annotations such as {@link NotNull} etc.
 *       Please note that mutator with at least one validation annotation should also be annotated with {@link ValidationRequired} -- this is enforced by the platform and failure to comply results in early runtime exception.
 *   </li>
 *   <li>Mutators for a collectional property -- there are three possible mutators recognised by TG:
 *       <ul>
 *          <li>Method name is '''set[property]''', which accepts one parameter of the same type such as Collection[Type] or List[type].
 *              This method as any other mutator should be provided with annotations discussed in the previous item.
 *              However, there is a general recommendation to implement such mutator in the following manner:
 *              <pre>
 *                  EntityOwner setCollecionalProperty(final Collection[Type] newValues) {
 *                  	collecionalProperty.clear();
 *                  	collecionalProperty.addAll(newValues);
 *                  	return this;
 *                  }
 * </pre>
 *          </li>
 *          <li>Method name is '''addTo[property]''' with one parameter of collectional parameter type. So, if property has type ''Collection[Type]'' then input parameter of type ''Type'' is expected.
 *              This mutator is called ''incrementor'' -- it leads to an incremental expansion of the collectional property. The same rules for method annotations apply as above.
 *          </li>
 *          <li>Method name is '''removeFrom[property]''' with one parameter of collectional parameter type. So, if property has type ''Collection[Type]'' then input parameter of type ''Type'' is expected.
 *              This mutator is called ''decrementor'' -- it leads to an incremental reduction of the collectional property. The same rules for method annotations apply as above.
 *          </li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * Please note that mutators for the same collectional property may have different validation annotations.
 * However, annotation {@link DomainValidation} always points to the same actual validator, and it is developer's responsibility to ensure that it executes correctly regardless as part of what mutator it has been invoked.
 * Please refer to {@link DomainValidation} documentation for more details.
 * <p>
 * ==================================================================<br/>
 * <p>
 * <h3>Notable changes</h3>
 * Date: 2008-10-28
 * Introduced validation synchronisation mechanism.
 *
 * Date: 2008-10-29
 * Implemented support for domain validation logic. Simply annotate property setter with {@link DomainValidation} and
 * provide appropriate {@link IBeforeChangeEventHandler} instance as part of {@link DomainValidationConfig} configuration.
 *
 * Date: 2009-02-09
 * Introduced property <code>initialising</code> to indicate an entity state where its properties are being initialised and thus no properties require any validation.
 *
 * Date: 2009-02-10 Implemented runtime validation of {@link KeyType} presence. If annotation is not present a runtime exception is thrown preventing entity instantiation.
 *
 * Date: 2009-06-10 Introduced property <code>version</code> to enable optimistic locking in Hibernate.
 *
 * Date: 2010-03-18 Added restore to original feature.
 *
 * Date: 2010-04-06 Implemented method copy, which can copy anything to anything.
 *
 * @param <K>
 *            Type of the business key (e.g. String, Integer, component), which must implement {@link Comparable}.
 *
 * @author TG Team
 */
public abstract class AbstractEntity<K extends Comparable> implements Serializable, Comparable<AbstractEntity<K>>, IBindingEntity {
    private static final long serialVersionUID = 1L;

    private transient final Logger logger;

    @MapTo("_ID")
    private Long id;
    @MapTo("_VERSION")
    private Long version = 0L;
    @IsProperty
    @UpperCase
    @MapTo("KEY_")
    private K key;
    private transient final boolean compositeKey;
    @IsProperty
    @MapTo("DESC_")
    private String desc;

    /** Convenient constants to reference properties by name */
    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String KEY = "key";
    public static final String GETKEY = "getKey()";
    public static final String DESC = "desc";

    /**
     * Provides property change support.
     */
    private transient final PropertyChangeSupportEx changeSupport;
    /**
     * Holds meta-properties for entity properties.
     */
    private transient final Map<String, MetaProperty> properties;
    /**
     * Indicates if entity instance is being initialised.
     */
    private transient boolean initialising = false;

    /*
     * Block of fields responsible for synchronisation of validation for properties and entity itself.
     */
    private transient final Lock lock;
    private transient final Condition validationInProgress;
    private transient volatile int lockCount;

    private transient final Class<K> keyType;
    private transient final Class<?> actualEntityType;
    /**
     * A reference to the application specific {@link EntityFactory} instance responsible for instantiation of this and other entities. It is also used for entity cloning.
     */
    private transient EntityFactory entityFactory;

    /**
     * Property factory is responsible for meta-property instantiation. The actual instantiation happens in the setter.
     */
    private transient IMetaPropertyFactory metaPropertyFactory;

    /**
     * This is a default constructor, which is required for reflective construction.
     */
    @SuppressWarnings("unchecked")
    protected AbstractEntity() {
	actualEntityType = PropertyTypeDeterminator.stripIfNeeded(getClass());
	changeSupport = new PropertyChangeSupportEx(this);
	properties = new HashMap<String, MetaProperty>();
	lock = new ReentrantLock();
	validationInProgress = lock.newCondition();
	lockCount = 0;

	keyType = (Class<K>) AnnotationReflector.getKeyType(this.getClass());
	if (keyType == null) {
	    throw new IllegalStateException("Type " + this.getClass().getName() + " is not fully defined.");
	}

	compositeKey = Finder.getKeyMembers(getType()).size() > 1;
	/*
	 * if (hasCompositeKey() && DynamicEntityKey.class.equals(keyType)) { setKey((K) new DynamicEntityKey((AbstractEntity<DynamicEntityKey>)this)); }
	 */

	logger = Logger.getLogger(this.getType());
    }

    /**
     * The main entity constructor.
     *
     * @param id
     * @param key
     * @param desc
     */
    protected AbstractEntity(final Long id, final K key, final String desc) {
	this();
	this.id = id;
	this.key = key;
	this.desc = desc;
    }

    public Long getId() {
	return id;
    }

    /**
     * This setter is protected so that descendants could provide additional functionality if required (e.g. validation).
     *
     * @param id
     */
    protected void setId(final Long id) {
	this.id = id;
    }

    public String getDesc() {
	if (AbstractEntity.class.isAssignableFrom(getKeyType())) {
	    return getKey() != null ? AbstractEntity.class.cast(getKey()).getDesc() : "";
	}
	return desc;
    }

    @Observable
    public AbstractEntity<K> setDesc(final String desc) {
	this.desc = desc;
	return this;
    }

    public K getKey() {
	return key;
    }

    @NotNull
    @Observable
    public AbstractEntity<K> setKey(final K key) {
	this.key = key;
	updateKeyMetaProperty();
	return this;
    }

    /**
     * At the instantiation time it is not possible to determine key type, because the actual value may not be available. Therefore, this method was introduced to ensure correct
     * type assignment to the meta-property associated with <code>key</code>.
     */
    private void updateKeyMetaProperty() {
	final MetaProperty property = getProperty(KEY);
	if (property != null && this.key != null) {
	    property.setType(getKeyType());
	}
    }

    /**
     * Hashing is based on the business key implementation.
     */
    @Override
    public final int hashCode() {
	return (getKey() != null ? getKey().hashCode() : 0) * 23;
    }

    /**
     * Equality is based on the business key implementation.
     */
    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!(obj instanceof AbstractEntity)) {
	    return false;
	}
	// let's ensure that types match
	final AbstractEntity that = (AbstractEntity) obj;
	if (this.getType() != that.getType()) {
	    return false;
	}
	// now can compare key values
	final Object thatKey = that.getKey();
	return getKey() != null && getKey().equals(thatKey) || getKey() == null && thatKey == null;
    }

    @Override
    public String toString() {
	return getKey() != null ? getKey().toString() : null;
    }

    /**
     * Implements comparison based on the business key implementation.
     */
    public final int compareTo(final AbstractEntity<K> cmpTo) {
	return getKey().compareTo(cmpTo.getKey());
    }

    /**
     * A convenient method for obtaining entity key type.
     *
     * @return
     */
    public final Class<K> getKeyType() {
	return keyType;
    }

    /**
     * Registers property change listener.<br>
     * <br>
     * Note : Please, refer also to {@link PropertyChangeOrIncorrectAttemptListener} JavaDocs.
     *
     * @param propertyName
     * @param listener
     */
    public final synchronized void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
	if (listener == null) {
	    throw new IllegalArgumentException("PropertyChangeListener cannot be null.");
	}
	if (!DynamicEntityQueryCriteria.class.isAssignableFrom(this.getType()) && !isObservable(propertyName)) {
	    throw new IllegalArgumentException("Cannot register PropertyChangeListener with non-observable property '" + propertyName + "'.");
	}
	changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Determines whether class was enhanced in order to get the correct entity class.
     *
     * @return
     */
    public final Class<?> getType() {
	return actualEntityType;
    }

    /**
     * Removes property change listener.
     */
    public final synchronized void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
	changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public final PropertyChangeSupportEx getChangeSupport() {
	return changeSupport;
    }

    protected final void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
	final PropertyChangeSupport aChangeSupport = this.changeSupport;
	if (aChangeSupport == null) {
	    return;
	}
	aChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Determines whether the specified property is observable.
     * <p>
     * The main purpose for this method is to identify at early runtime situations where property change listeners are bound to non-observable properties.
     *
     * @param startWithClass
     * @param propertyName
     * @return
     * @throws Exception
     */
    public final boolean isObservable(final String propertyName) {
	try {
	    //	    final Class<?> propertyType = PropertyTypeDeterminator.determinePropertyType(getType(), propertyName);
	    //	    final Method setter = Reflector.getMethod(/* getType() */this, "set" + propertyName.toUpperCase().charAt(0) + propertyName.substring(1), propertyType);
	    final Class<?> type = getType();
	    final Method setter = Reflector.obtainPropertySetter(type, propertyName);
	    return setter.isAnnotationPresent(Observable.class);
	} catch (final NoSuchMethodException e) {
	    try {
		final Method setter = Reflector.obtainPropertySetter(getType(), propertyName);//
		return setter.isAnnotationPresent(Observable.class);

	    } catch (final NoSuchMethodException ex) {
	    }
	}
	return false;
    }

    /**
     * Dynamic getter for accessing property value.
     *
     * @param propertyName
     * @return
     */
    public Object get(final String propertyName) {
	try {
	    return Finder.findFieldValueByName(this, propertyName);
	} catch (final Exception e) {
	    throw new IllegalArgumentException("Could not get the value for property " + propertyName + " for instance " + this + "@" + getType().getName(), e);
	}
    }

    /**
     * Dynamic setter for setting property value.
     *
     * @param propertyName
     * @param value
     */
    public void set(final String propertyName, final Object value) {
	try {
	    final Class<?> propertyType = Finder.findFieldByName(getType(), propertyName).getType();
	    final Method setter = Reflector.getMethod(/* getType() */this, "set" + propertyName.toUpperCase().charAt(0) + propertyName.substring(1), propertyType);
	    Object valueToInvokeOn = this;
	    if (!setter.getDeclaringClass().isAssignableFrom(getType()) && AbstractUnionEntity.class.isAssignableFrom(getType())) {
		valueToInvokeOn = ((AbstractUnionEntity) this).activeEntity();
	    }
	    // making method accessible if it isn't
	    final boolean isAccessible = setter.isAccessible();
	    setter.setAccessible(true);
	    setter.invoke(valueToInvokeOn, value);
	    // reverting changes to 'accessible' property of Method class
	    setter.setAccessible(isAccessible);
	} catch (final Exception e) {
	    throw new IllegalStateException(e.getCause());
	}
    }

    /**
     * Returns property by name.
     *
     * @param name
     * @return
     */
    public final MetaProperty getProperty(final String name) {
	return properties.get(name);
    }

    public final IMetaPropertyFactory getPropertyFactory() {
	return metaPropertyFactory;
    }

    /**
     * This setter is responsible for meta-property creation. It is envisaged that IPropertyFactory will be provided as an injection. An thus, meta-property instantiation should
     * happen straight after entity creation.
     *
     * @param metaPropertyFactory
     */
    @Inject
    protected void setMetaPropertyFactory(final IMetaPropertyFactory metaPropertyFactory) {
	///logger.debug("Starting meta construction with factory " + metaPropertyFactory + " for type " + getType());
	// if meta-property factory has already been assigned it should not change
	if (this.metaPropertyFactory != null) {
	    logger.error("Property factory can be assigned only once.");
	    throw new IllegalStateException("Property factory can be assigned only once.");
	}
	this.metaPropertyFactory = metaPropertyFactory;
	final List<Field> keyMembers = Finder.getKeyMembers(getType());

	// obtain field annotated as properties
	final List<Field> fields = Finder.findRealProperties(getClass());
	//logger.debug("Iterating through " + fields.size() + " properties for building corresponding meta-properties.");
	for (final Field field : fields) { // for each property field
	    //logger.debug("Property " + field.getName());
	    try {
		// ensure that there is an accessor -- with out it field is not a property
		// throws exception if method does not exists
		Reflector.obtainPropertyAccessor(getType(), field.getName());
		// determine property type and adjacent virtues
		final Class<?> type = determineType(field);
		//logger.debug("TYPE (" + field.getName() + ") : " + type);
		final boolean isKey = keyMembers.contains(field);
		//logger.debug("IS_KEY (" + field.getName() + ") : " + isKey);
		final boolean isCollectional = Collection.class.isAssignableFrom(type);
		//logger.debug("IS_COLLECTIONAL (" + field.getName() + ") : " + isCollectional);
		final Class<?> propertyAnnotationType = field.getAnnotation(IsProperty.class).value();
		final boolean isUpperCase = field.isAnnotationPresent(UpperCase.class);
		//logger.debug("IS_UPPERCASE (" + field.getName() + ") : " + isUpperCase);
		// perform some early runtime validation whether property was defined correctly
		if ((isCollectional || PropertyDescriptor.class.isAssignableFrom(type)) && (propertyAnnotationType == Void.class || propertyAnnotationType == null)) {
		    final String error = "Property " + field.getName() + " in " + getType()
		    + " is collectional, but has missing collectional type, which should be specified as part of annotation IsProperty.";
		    logger.error(error);
		    throw new IllegalStateException(error);
		}

		// if setter is annotated then try to instantiate specified validator
		//logger.debug("Collecting validators for " + field.getName());
		final Map<ValidationAnnotation, Map<IBeforeChangeEventHandler, Result>> validators = collectValidators(metaPropertyFactory, field, type, isCollectional);
		// create meta definer
		//logger.debug("Initiating meta-property definer for " + field.getName());
		final IMetaPropertyDefiner definer = metaPropertyFactory.create(this, field.getName());
		// create meta-property
		//logger.debug("Creating meta-property for " + field.getName());
		final MetaProperty metaProperty = new MetaProperty(this, field, type, isKey, isCollectional, propertyAnnotationType, field.isAnnotationPresent(Calculated.class), isUpperCase, validators, definer, extractDependentProperties(field, fields));
		// define meta-property properties used most commonly for UI construction: required, editable, title and desc //
		//logger.debug("Initialising meta-property for " + field.getName());
		initProperty(keyMembers, field, metaProperty);
		// put meta-property in the map associating it with a corresponding property name
		properties.put(field.getName(), metaProperty);
	    } catch (final Exception e) {
		logger.error("Entity instantiation failed.", e);
		throw new IllegalStateException(e); // entity without properly generated met-properties is considered to be in illegal state
	    }
	}
	//logger.debug("Finished meta construction for type " + getType());
    }

    /**
     * Determines property type.
     *
     * @param field
     * @return
     */
    private Class<?> determineType(final Field field) {
	if (KEY.equals(field.getName())) {
	    return keyType;
	} else {
	    return PropertyTypeDeterminator.stripIfNeeded(field.getType());
	}
    }

    /**
     * Analyses mutators and their annotations to collect and instantiate all property validators.
     *
     * @param metaPropertyFactory
     * @param field
     * @param type
     * @param isCollectional
     * @return map of validators
     * @throws Exception
     */
    private Map<ValidationAnnotation, Map<IBeforeChangeEventHandler, Result>> collectValidators(final IMetaPropertyFactory metaPropertyFactory, final Field field, final Class<?> type, final boolean isCollectional)
    throws Exception {
	//logger.debug("Start collecting validators for property " + field.getName() + "...");
	try {
	    final Map<ValidationAnnotation, Map<IBeforeChangeEventHandler, Result>> validators = new EnumMap<ValidationAnnotation, Map<IBeforeChangeEventHandler, Result>>(ValidationAnnotation.class);
	    // Get corresponding mutators to pick all specified validators in case of a collectional property there can be up to three mutators --
	    // removeFrom[property name], addTo[property name] and set[property name]
	    final Set<Annotation> propertyValidationAnotations = extractValidationAnnotationForProperty(field, type, isCollectional);
	    for (final Annotation annotation : propertyValidationAnotations) {
		final ValidationAnnotation validationAnnotation = ValidationAnnotation.getValueByType(annotation);
		// if property factory cannot instantiate a validator for the specified annotation then null is returned;
		final IBeforeChangeEventHandler[] annotationValidators = metaPropertyFactory.create(annotation, this, field.getName(), type);
		if (annotationValidators.length > 0) {
		    final Map<IBeforeChangeEventHandler, Result> handlersAndResults = new HashMap<IBeforeChangeEventHandler, Result>();
		    for (final IBeforeChangeEventHandler handler : annotationValidators) {
			handlersAndResults.put(handler, null);
		    }
		    validators.put(validationAnnotation, handlersAndResults);
		}
	    }
	    // logger.debug("Finished collecting validators for property " + field.getName() + ".");
	    return validators;
	} catch (final Exception ex) {
	    logger.error("Exception during collection of validators for property " + field.getName() + ".", ex);
	    throw ex;
	}
    }

    /**
     * Initialises meta-property properties used most commonly for UI construction such as required, editable, title and desc.
     *
     * @param keyMembers
     * @param field
     * @param metaProperty
     */
    private void initProperty(final List<Field> keyMembers, final Field field, final MetaProperty metaProperty) {
	if (KEY.equals(field.getName())) {
	    metaProperty.setVisible(!(KEY.equals(field.getName()) && keyMembers.size() > 1)); // if entity is composite then "key" should be inactive
	    metaProperty.setEditable(!AnnotationReflector.isAnnotationPresent(KeyReadonly.class, getType()));
	    metaProperty.setRequired(true);
	    if (AnnotationReflector.isAnnotationPresent(KeyTitle.class, getType())) {
		final KeyTitle title = AnnotationReflector.getAnnotation(KeyTitle.class, getType());
		metaProperty.setTitle(title.value());
		metaProperty.setDesc(StringUtils.isEmpty(title.desc()) ? title.value() : title.desc());
	    }
	} else if (DESC.equals(field.getName())) {
	    metaProperty.setEditable(!AnnotationReflector.isAnnotationPresent(DescReadonly.class, getType()));
	    metaProperty.setRequired(AnnotationReflector.isAnnotationPresent(DescRequired.class, getType()));
	    if (AnnotationReflector.isAnnotationPresent(DescTitle.class, getType())) {
		final DescTitle title = AnnotationReflector.getAnnotation(DescTitle.class, getType());
		metaProperty.setTitle(title.value());
		metaProperty.setDesc(StringUtils.isEmpty(title.desc()) ? title.value() : title.desc());
	    }
	} else {
	    metaProperty.setVisible(!field.isAnnotationPresent(Invisible.class));
	    metaProperty.setEditable(!field.isAnnotationPresent(Readonly.class));
	    metaProperty.setRequired(field.isAnnotationPresent(Required.class) || field.isAnnotationPresent(CompositeKeyMember.class));
	    if (field.isAnnotationPresent(Title.class)) {
		final Title title = field.getAnnotation(Title.class);
		metaProperty.setTitle(title.value());
		metaProperty.setDesc(StringUtils.isEmpty(title.desc()) ? title.value() : title.desc());
	    }
	}
    }

    /**
     * Extracts and validates the array of dependent propertyNames related to the 'field' in the case if it is annotated with @Dependent. Throws RuntimeException if there is no
     * property with the specified propertyNames, or if the self-dependence was specified.
     *
     * @param field
     * @param allFields
     * @return
     */
    private String[] extractDependentProperties(final Field field, final List<Field> allFields) {
	if (field.isAnnotationPresent(Dependent.class)) {
	    final List<String> allFieldsNames = new ArrayList<String>();
	    for (final Field f : allFields) {
		allFieldsNames.add(f.getName());
	    }
	    final String[] dependentPropertyNames = field.getAnnotation(Dependent.class).value();
	    for (final String propName : dependentPropertyNames) {
		if (!allFieldsNames.contains(propName)) {
		    throw new IllegalArgumentException("There is no dependent property [" + propName + "].");
		} else if (propName.equals(field.getName())) {
		    throw new IllegalArgumentException("The property [" + propName + "] cannot be dependent to itself.");
		}
	    }
	    return dependentPropertyNames;
	}
	return null;
    }

    /**
     * Creates a set of all validation annotations specified for property mutators.
     *
     * @param field
     * @param type
     * @param isCollectional
     * @return
     * @throws NoSuchMethodException
     */
    public Set<Annotation> extractValidationAnnotationForProperty(final Field field, final Class<?> type, final boolean isCollectional) {
	final Set<Annotation> propertyValidationAnotations = new HashSet<Annotation>();
	// try to obtain setter
	propertyValidationAnotations.addAll(extractSetterAnnotations(field, type));

	// if field represents a collectional property then it may have other mutators
	if (isCollectional) {
	    // try to obtain incrementor
	    propertyValidationAnotations.addAll(extractIncrementorAnnotations(field, type));
	    // try to obtain decrementor
	    propertyValidationAnotations.addAll(extractDecrementorAnnotations(field, type));
	}
	return propertyValidationAnotations;
    }

    /**
     * Attempts to obtain collectional field decrementor and extract its annotations for further processing. If there is no decrementor defined then an empty set of annotations is
     * returned.
     *
     * @param field
     * @param type
     * @return
     */
    private Set<Annotation> extractDecrementorAnnotations(final Field field, final Class<?> type) {
	final Set<Annotation> propertyValidationAnotations = new HashSet<Annotation>();
	try {
	    final Method decrementor = Reflector.getMethod(/* getType() */this, "removeFrom" + field.getName().toUpperCase().charAt(0) + field.getName().substring(1), type);
	    final Set<Annotation> annotations = AnnotationReflector.getValidationAnnotations(decrementor);
	    if (annotations.size() > 0 && decrementor.getAnnotation(Observable.class) == null) {
		throw new IllegalStateException("Property " + field.getName() + " in " + getType()
			+ " requires validation, but corresponding decrementor is not observable (no Observable annotation).");
	    }
	    propertyValidationAnotations.addAll(annotations);
	} catch (final NoSuchMethodException e) {
	    // do nothing if decrementor does not exist
	}
	return propertyValidationAnotations;
    }

    /**
     * Attempts to obtain collectional field incrementor and extract its annotations for further processing. If there is no incrementor defined then an empty set of annotations is
     * returned.
     *
     * @param field
     * @param type
     * @return
     */
    private Set<Annotation> extractIncrementorAnnotations(final Field field, final Class<?> type) {
	final Set<Annotation> propertyValidationAnotations = new HashSet<Annotation>();
	try {
	    final Method incremetor = Reflector.getMethod(/* getType() */this, "addTo" + field.getName().toUpperCase().charAt(0) + field.getName().substring(1), type);
	    final Set<Annotation> annotations = AnnotationReflector.getValidationAnnotations(incremetor);
	    if (annotations.size() > 0 && incremetor.getAnnotation(Observable.class) == null) {
		throw new IllegalStateException("Property " + field.getName() + " in " + getType()
			+ " requires validation, but corresponding incremetor is not observable (no Observable annotation).");
	    }
	    propertyValidationAnotations.addAll(annotations);
	} catch (final NoSuchMethodException e1) {
	    // do nothing if incrementor does not exist
	}
	return propertyValidationAnotations;
    }

    /**
     * Attempts to obtain field setter and extract its annotations for further processing. If there is no setter defined then an empty set of annotations is returned.
     *
     * @param field
     * @param type
     * @return
     */
    private Set<Annotation> extractSetterAnnotations(final Field field, final Class<?> type) {
	//logger.debug("Extracting validation annotations for property " + field.getName() + ".");
	final Set<Annotation> propertyValidationAnotations = new HashSet<Annotation>();
	try {
	    final Method setter = Reflector.getMethod(this, "set" + field.getName().toUpperCase().charAt(0) + field.getName().substring(1), type);
	    if (setter.getAnnotation(Observable.class) == null) {
		final String errorMsg = "Setter " + setter.getName() + " for property " + field.getName() + " is not observable (no Observable annotation).";
		logger.error(errorMsg);
		throw new IllegalStateException(errorMsg);
	    }
	    final Set<Annotation> annotations = AnnotationReflector.getValidationAnnotations(setter);
	    //logger.debug("Number of validation annotations for property " + field.getName() + ": " + annotations.size());
	    propertyValidationAnotations.addAll(annotations);
	} catch (final NoSuchMethodException e1) {
	    // do nothing if setter does not exist
	    logger.debug("There is no setter for property " + field.getName() + ".");
	}
	return propertyValidationAnotations;
    }

    /**
     * Returns unmodifiable map of properties.
     *
     * @return
     */
    public final Map<String, MetaProperty> getProperties() {
	return Collections.unmodifiableMap(properties);
    }

    /**
     * Increases <code>lockCount</code> by one in a thread safe manner.
     */
    public final void lock() {
	lock.lock();
	lockCount++;
	lock.unlock();
    }

    /**
     * Decreases <code>lockCount</code> by one in a thread safe manner. Signals lock condition <code>validationInProgress</code> when <code>lockCount</code> reaches value zero.
     */
    public final void unlock() {
	lock.lock();
	lockCount = lockCount > 0 ? lockCount - 1 : 0;
	if (lockCount == 0) {
	    validationInProgress.signal();
	}
	lock.unlock();
    }

    /**
     * This method should be used to check whether entity is valid.
     * <p>
     * Supports locking mechanism to ensure that property validation finishes before checking its results.
     *
     * @return
     */
    public final Result isValid() {
	// employ locking
	lock.lock();
	try {
	    while (lockCount != 0) {
		try {
		    validationInProgress.await();
		} catch (final InterruptedException e) {
		    // no need to handle
		}
	    }

	    // invoke validation logic
	    return validate();

	} finally {
	    lock.unlock();
	}
    }

    /**
     * This method is provided to implement entity specific validation.
     * <p>
     * The default implementation checks fields declared as property (see {@link IsProperty}) for validity. It is envisaged that descendants will invoke super implementation and
     * provide some specific logic.
     * <p>
     *
     * @return validation result
     */
    protected Result validate() {
	Result firstFailure = null;
	// iterate over properties in search of the first invalid one
	for (final MetaProperty property : properties.values()) {
	    // if invalid return first error
	    if (!property.isValidWithRequiredCheck() && firstFailure == null) { // 1. process isValid() that triggers requiredness checking. 2. saves the first failure
		firstFailure = property.getFirstFailure();
	    }
	}
	return firstFailure == null ? new Result(this, "Entity " + this + " is valid.") : firstFailure; // returns first failure if exists or successful result if there was no
	// failure.
    }

    /**
     * @return {@link EntityFactory} which created this {@link AbstractEntity}
     */
    public final EntityFactory getEntityFactory() {
	return entityFactory;
    }

    /**
     * Sets reference to new {@link EntityFactory} instance. However this method should be called only once (probably in {@link EntityFactory}), because during runtime there is
     * only one {@link EntityFactory} instance
     *
     * @param entityFactory
     */
    protected final void setEntityFactory(final EntityFactory entityFactory) {
	this.entityFactory = entityFactory;
    }

    /**
     * A convenience method for determining whether this entity has been persisted.
     *
     * @return true if this entity is persisted in the database (i.e. id is not null), false otherwise
     */
    public final boolean isPersisted() {
	return getId() != null;
    }

    /**
     * Checks whether this is an enhanced entity instance.
     *
     * @return
     */
    public final boolean isEnhanced() {
	return !getType().getName().equals(getClass().getName());
    }

    public final boolean isInitialising() {
	return initialising;
    }

    public final void setInitialising(final boolean initialising) {
	this.initialising = initialising;
    }

    public final boolean hasCompositeKey() {
	return compositeKey;
    }

    /**
     * This is an experimental support for checking whether an entity instance is dirty -- was modified, but changes has not been yet persisted. The envisage scenario is this: any
     * change to a property leads to isDirty; once entity is persisted the dirty state is reset to false.
     *
     * @return
     */
    public final boolean isDirty() {
	return getDirtyProperties().size() != 0 || !isPersisted();
    }

    public final void setDirty(final boolean dirty) {
	// reset dirty state for properties in case where entity becomes not dirty
	if (!dirty) {
	    for (final MetaProperty prop : getDirtyProperties()) {
		prop.setDirty(false);
	    }
	}
    }

    /**
     * A utility method for accessing dirty properties.
     *
     * @return
     */
    public final List<MetaProperty> getDirtyProperties() {
	final List<MetaProperty> dirtyProperties = new ArrayList<MetaProperty>();
	for (final MetaProperty prop : getProperties().values()) {
	    if (!prop.isCalculated() && prop.isDirty()) {
		dirtyProperties.add(prop);
	    }
	}
	return dirtyProperties;
    }

    public final void resetMetaState() {
	for (final MetaProperty property : properties.values()) {
	    property.resetState();
	}
    }

    public final void resetMetaValue() {
	for (final MetaProperty property : properties.values()) {
	    property.resetValues();
	}
    }

    /**
     * Indicates whether this entity instance can be changed. By default returns a sucessfull {@link Result} indicating editability of the entity.
     * <p>
     * This method should be overridden if some custom logic needs to be provided.
     * <p>
     * For example, some entity instance should not be changed when its certain property has some specific value.
     *
     * @return
     */
    public Result isEditable() {
	return Result.successful(null);
    }

    protected void setVersion(final Long ver) {
	version = ver;
    }

    public Long getVersion() {
	return version;
    }

    @Override
    public Class<?> getPropertyType(final String propertyName) {
	return getProperty(propertyName).getType();
    }

    /**
     * Restores state of all properties to original, which includes setting original values and removal of all validation errors.
     */
    public AbstractEntity<K> restoreToOriginal() {
	setInitialising(true);
	try {
	    for (final MetaProperty property : getProperties().values()) {
		property.restoreToOriginal();
	    }
	} finally {
	    setInitialising(false);
	}
	return this;
    }

    /**
     * Creates a new instance of the given type and copies all properties including ID from this instance into the created one.
     * <p>
     * <i>IMPORTANT: the provided type and the type of the instance copy is invoked on does not have to be even on the same branch of hierarchy and may not have the same
     * properties.</i>
     *
     * @param <COPY>
     * @param type
     * @return
     */
    public <COPY extends AbstractEntity> COPY copy(final Class<COPY> type) {
	final COPY copy = createCopyInstance(type);
	copy.setInitialising(true);
	for (final String propName : getProperties().keySet()) {
	    if (AbstractEntity.KEY.equals(propName) && copy.getKeyType().equals(getKeyType()) && DynamicEntityKey.class.isAssignableFrom(getKeyType())) {
		copy.setKey(new DynamicEntityKey(copy));
	    } else {
		try {
		    copy.set(propName, get(propName));
		} catch (final Exception e) {
		    logger.trace("Setter for property " + propName + " did not succeed during coping.");
		}
	    }
	}
	copy.setInitialising(false);
	return copy;
    }

    /**
     * Creates a new instance of the given type and copies ID from this instance into the created one.
     *
     * @param <COPY>
     * @param type
     * @return
     */
    protected <COPY extends AbstractEntity> COPY createCopyInstance(final Class<COPY> type) {
	return getEntityFactory().newEntity(type, getId());
    }
}
