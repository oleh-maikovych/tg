package ua.com.fielden.platform.domaintree.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ua.com.fielden.platform.domaintree.ICalculatedProperty;
import ua.com.fielden.platform.domaintree.ICalculatedProperty.CalculatedPropertyAttribute;
import ua.com.fielden.platform.domaintree.IDomainTreeEnhancer;
import ua.com.fielden.platform.entity.annotation.Calculated;
import ua.com.fielden.platform.entity.annotation.Title;
import ua.com.fielden.platform.entity.annotation.factory.CalculatedAnnotation;
import ua.com.fielden.platform.reflection.AnnotationReflector;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.reflection.PropertyTypeDeterminator;
import ua.com.fielden.platform.reflection.Reflector;
import ua.com.fielden.platform.reflection.asm.api.NewProperty;
import ua.com.fielden.platform.reflection.asm.impl.DynamicEntityClassLoader;
import ua.com.fielden.platform.reflection.asm.impl.DynamicTypeNamingService;
import ua.com.fielden.platform.serialisation.api.ISerialiser;
import ua.com.fielden.platform.serialisation.impl.TgKryo;
import ua.com.fielden.platform.utils.EntityUtils;
import ua.com.fielden.platform.utils.Pair;

/**
 * A domain manager implementation with all sufficient logic for domain modification / loading. <br><br>
 *
 * <b>Implementation notes:</b><br>
 * 1. After the modifications have been applied manager consists of a map of (entityType -> real enhanced entityType).
 * To play correctly with any type information with enhanced domain you need to use ({@link #getManagedType(Class)} of entityType; dotNotationName) instead of (entityType; dotNotationName).<br>
 * 2. The current version of manager after some modifications (calcProperty has been added/removed/changed) holds a full list of calculated properties for all types.
 * This list should be applied or discarded using {@link #apply()} or {@link #discard()} interface methods.<br>
 * 3.
 *
 * @author TG Team
 *
 */
public final class DomainTreeEnhancer extends AbstractDomainTree implements IDomainTreeEnhancer {
    private static final long serialVersionUID = -7996646149855822266L;
    private static final Logger logger = Logger.getLogger(DomainTreeEnhancer.class);

    /** Holds a set of root types to work with. */
    private final Set<Class<?>> rootTypes;
    /** Holds byte arrays of <b>enhanced</b> (and only <b>enhanced</b>) types mapped to their original root types. The first item in the list is "enhanced root type's" array. */
    private final Map<Class<?>, Map<String, ByteArray>> originalAndEnhancedRootTypesArrays;
    /** Holds current domain differences from "standard" domain (all calculated properties for all root types). */
    private final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties;
    /** Holds a current (and already applied / loaded) snapshot of domain -- consists of a pairs of root types: [original -> real] (or [original -> original] in case of not enhanced type) */
    private final transient Map<Class<?>, Class<?>> originalAndEnhancedRootTypes;

    public static class ByteArray {
	private final byte[] array;

	protected ByteArray() {
	    array = null;
	}

	public ByteArray(final byte[] array) {
	    this.array = array;
	}

	public byte[] getArray() {
	    return array;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + Arrays.hashCode(array);
	    return result;
	}

	@Override
	public boolean equals(final Object obj) {
	    if (this == obj) {
		return true;
	    }
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final ByteArray other = (ByteArray) obj;
	    if (!Arrays.equals(array, other.array)) {
		return false;
	    }
	    return true;
	}
    }

    /**
     * Constructs a new instance of domain enhancer with clean not enhanced domain.
     *
     * @param rootTypes -- root types
     */
    public DomainTreeEnhancer(final ISerialiser serialiser, final Set<Class<?>> rootTypes) {
	this(serialiser, rootTypes, new HashMap<Class<?>, Map<String, ByteArray>>(), null);
    }

    /**
     * Constructs a new instance of domain enhancer with an enhanced domain (provided using byte arrays of <b>enhanced</b> (and only <b>enhanced</b>) types mapped to their original types).
     *
     * @param rootTypes -- root types
     * @param originalAndEnhancedRootTypesArrays -- a map of pair [original => enhanced class byte array] root types
     *
     */
    public DomainTreeEnhancer(final ISerialiser serialiser, final Set<Class<?>> rootTypes, final Map<Class<?>, Map<String, ByteArray>> originalAndEnhancedRootTypesArrays) {
	this(serialiser, rootTypes, originalAndEnhancedRootTypesArrays, null);
    }

    /**
     * Constructs a new instance of domain enhancer with an <b>enhanced</b> (and only <b>enhanced</b>) types and current calculated properties (which can differ from accepted enhanced domain).
     *
     * @param rootTypes -- root types
     * @param originalAndEnhancedRootTypesArrays -- a map of pair [original => enhanced class byte array] root types
     * @param calculatedProperties -- current version of calculated properties
     */
    public DomainTreeEnhancer(final ISerialiser serialiser, final Set<Class<?>> rootTypes, final Map<Class<?>, Map<String, ByteArray>> originalAndEnhancedRootTypesArrays, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties) {
	super(serialiser);
	this.rootTypes = new HashSet<Class<?>>();
	this.rootTypes.addAll(rootTypes);

	this.originalAndEnhancedRootTypesArrays = new HashMap<Class<?>, Map<String, ByteArray>>();
	this.originalAndEnhancedRootTypesArrays.putAll(originalAndEnhancedRootTypesArrays);

	// Initialise a map with enhanced (or not) types. A new instance of classLoader is needed for loading enhanced "byte arrays".
	this.originalAndEnhancedRootTypes = new HashMap<Class<?>, Class<?>>();
	for (final Class<?> rootType : this.rootTypes) {
	    this.originalAndEnhancedRootTypes.put(rootType, rootType);
	}
	final DynamicEntityClassLoader classLoader = new DynamicEntityClassLoader(ClassLoader.getSystemClassLoader());
	for (final Entry<Class<?>, Map<String, ByteArray>> entry : this.originalAndEnhancedRootTypesArrays.entrySet()) {
	    final Map<String, ByteArray> arrays = new HashMap<String, ByteArray>(entry.getValue());
	    if (!arrays.isEmpty()) {
		final ByteArray mainArray = arrays.get("");
		this.originalAndEnhancedRootTypes.put(entry.getKey(), classLoader.defineClass(mainArray.getArray()));
		arrays.remove("");
		for (final ByteArray array : arrays.values()) {
		    classLoader.defineClass(array.getArray());
		}
	    }
	}

	this.calculatedProperties = new HashMap<Class<?>, List<ICalculatedProperty>>();
	this.calculatedProperties.putAll(calculatedProperties == null ? extractAll(this) : calculatedProperties);

	for (final List<ICalculatedProperty> calcProps : this.calculatedProperties.values()) {
	    for (final ICalculatedProperty calcProp : calcProps) {
		final CalculatedProperty cp = (CalculatedProperty) calcProp;
		cp.setEnhancer(this);
		CalculatedProperty.repeatSettingOfImportantStuff(cp);
	    }
	}

    }

    private static Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> createOriginalAndEnhancedRootTypesFromRootTypes(final Set<Class<?>> rootTypes) {
	final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypes = new HashMap<Class<?>, Pair<Class<?>, Map<String, ByteArray>>>();
	for (final Class<?> rootType : rootTypes) {
	    originalAndEnhancedRootTypes.put(rootType, new Pair<Class<?>, Map<String, ByteArray>>(rootType, new HashMap<String, ByteArray>()));
	}
	return originalAndEnhancedRootTypes;
    }

    @Override
    public Class<?> getManagedType(final Class<?> type) {
	final Class<?> mutatedType = originalAndEnhancedRootTypes.get(type);
	return mutatedType == null ? type : mutatedType;
    }

    @Override
    public List<ByteArray> getManagedTypeArrays(final Class<?> type) {
	final Map<String, ByteArray> byteArrays = originalAndEnhancedRootTypesArrays.get(type);
	return byteArrays == null ? Collections.<ByteArray>emptyList() : new ArrayList<ByteArray>(byteArrays.values());
    }

    @Override
    public void apply() {
	//////////// Performs migration [calculatedProperties => originalAndEnhancedRootTypes] ////////////
	final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> freshOriginalAndEnhancedRootTypes = generateHierarchy(originalAndEnhancedRootTypes.keySet(), calculatedProperties);
	originalAndEnhancedRootTypes.clear();
	originalAndEnhancedRootTypesArrays.clear();
	for (final Entry<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> entry : freshOriginalAndEnhancedRootTypes.entrySet()) {
	    originalAndEnhancedRootTypes.put(entry.getKey(), entry.getValue().getKey());
	    originalAndEnhancedRootTypesArrays.put(entry.getKey(), new HashMap<String, ByteArray>(entry.getValue().getValue()));
	}
    }

    /**
     * Fully generates a new hierarchy of "originalAndEnhancedRootTypes" that conform to "calculatedProperties".
     *
     * @param rootTypes
     * @param calculatedProperties
     * @return
     */
    protected static Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> generateHierarchy(final Set<Class<?>> rootTypes, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties) {
	// single classLoader instance is needed for single "apply" transaction
	final DynamicEntityClassLoader classLoader = new DynamicEntityClassLoader(ClassLoader.getSystemClassLoader());

	final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypes = createOriginalAndEnhancedRootTypesFromRootTypes(rootTypes);

	final Map<Class<?>, Map<String, Map<String, ICalculatedProperty>>> groupedCalculatedProperties = groupByPaths(calculatedProperties);

	// iterate through calculated property places (e.g. Vehicle.class+"" or WorkOrder.class+"veh.status") with no care about order
	for (final Entry<Class<?>, Map<String, Map<String, ICalculatedProperty>>> entry : groupedCalculatedProperties.entrySet()) {
	    final Class<?> originalRoot = entry.getKey();
	    // generate predefined root type name for all calculated properties
	    final String predefinedRootTypeName = new DynamicTypeNamingService().nextTypeName(originalRoot.getName());
	    for (final Entry<String, Map<String, ICalculatedProperty>> placeAndProps : entry.getValue().entrySet()) {
		final Map<String, ICalculatedProperty> props = placeAndProps.getValue();
		if (props != null && !props.isEmpty()) {
		    final Class<?> realRoot = originalAndEnhancedRootTypes.get(originalRoot).getKey();
		    // a path to calculated properties
		    final String path = placeAndProps.getKey();

		    final NewProperty[] newProperties = new NewProperty[props.size()];
		    int i = 0;
		    for (final Entry<String, ICalculatedProperty> nameWithProp : props.entrySet()) {
			final ICalculatedProperty prop = nameWithProp.getValue();
			final Annotation calcAnnotation = new CalculatedAnnotation().contextualExpression(prop.getContextualExpression()).rootTypeName(predefinedRootTypeName).contextPath(prop.getContextPath()).origination(prop.getOriginationProperty()).attribute(prop.getAttribute()).category(prop.category()).newInstance();
			newProperties[i++] = new NewProperty(nameWithProp.getKey(), prop.resultType(), false, prop.getTitle(), prop.getDesc(), calcAnnotation);
		    }
		    // determine a "real" parent type:
		    final Class<?> realParentToBeEnhanced = StringUtils.isEmpty(path) ? realRoot : PropertyTypeDeterminator.determinePropertyType(realRoot, path);
		    try {
			final Map<String, ByteArray> existingByteArrays = new HashMap<String, ByteArray>(originalAndEnhancedRootTypes.get(originalRoot).getValue());

			// generate & load new type enhanced by calculated properties
			final Class<?> realParentEnhanced = classLoader.startModification(realParentToBeEnhanced.getName()).addProperties(newProperties).endModification();
			// propagate enhanced type to root
			final Pair<Class<?>, Map<String, ByteArray>> rootAfterPropagationAndAdditionalByteArrays = propagateEnhancedTypeToRoot(realParentEnhanced, realRoot, path, classLoader);
			final Class<?> rootAfterPropagation = rootAfterPropagationAndAdditionalByteArrays.getKey();
			// insert new byte arrays into beginning (the first item is an array of root type)
			existingByteArrays.putAll(rootAfterPropagationAndAdditionalByteArrays.getValue());
//			if (existingByteArrays.isEmpty()) {
//			    existingByteArrays.addAll(rootAfterPropagationAndAdditionalByteArrays.getValue());
//			} else {
//			    existingByteArrays.addAll(0, rootAfterPropagationAndAdditionalByteArrays.getValue());
//			}
			// replace relevant root type in cache
			originalAndEnhancedRootTypes.put(originalRoot, new Pair<Class<?>, Map<String, ByteArray>>(rootAfterPropagation, existingByteArrays));
		    } catch (final ClassNotFoundException e) {
			e.printStackTrace();
			logger.error(e);
			throw new RuntimeException(e);
		    }
		}
	    }
	    try {
		// modify root type name with predefinedRootTypeName
		final Pair<Class<?>, Map<String, ByteArray>> current = originalAndEnhancedRootTypes.get(originalRoot);
		final Class<?> rootWithPredefinedName = classLoader.startModification(current.getKey().getName()).modifyTypeName(predefinedRootTypeName).endModification();
		final Map<String, ByteArray> byteArraysWithRenamedRoot = new HashMap<String, ByteArray>();

		byteArraysWithRenamedRoot.putAll(current.getValue());
		byteArraysWithRenamedRoot.put("", new ByteArray(classLoader.getCachedByteArray(rootWithPredefinedName.getName())));

//		byteArraysWithRenamedRoot.add(classLoader.getCachedByteArray(rootWithPredefinedName.getName()));
//		byteArraysWithRenamedRoot.addAll(current.getValue());
		final Pair<Class<?>, Map<String, ByteArray>> neww = new Pair<Class<?>, Map<String, ByteArray>>(rootWithPredefinedName, byteArraysWithRenamedRoot);
		originalAndEnhancedRootTypes.put(originalRoot, neww);
	    } catch (final ClassNotFoundException e) {
		e.printStackTrace();
		logger.error(e);
		throw new RuntimeException(e);
	    }
	}
	return originalAndEnhancedRootTypes;
    }

    /**
     * Groups calc props into the map by its domain paths.
     *
     * @param calculatedProperties
     * @return
     */
    private static Map<Class<?>, Map<String, Map<String, ICalculatedProperty>>> groupByPaths(final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties) {
	final Map<Class<?>, Map<String, Map<String, ICalculatedProperty>>> grouped = new HashMap<Class<?>, Map<String, Map<String, ICalculatedProperty>>>();
	for (final Entry<Class<?>, List<ICalculatedProperty>> entry : calculatedProperties.entrySet()) {
	    final List<ICalculatedProperty> props = entry.getValue();
	    if (props != null && !props.isEmpty()) {
		final Class<?> root = entry.getKey();
		if (!grouped.containsKey(root)) {
		    grouped.put(root, new HashMap<String, Map<String, ICalculatedProperty>>());
		}
		for (final ICalculatedProperty prop : props) {
		    final String path = prop.path();
		    if (!grouped.get(root).containsKey(path)) {
			grouped.get(root).put(path, new HashMap<String, ICalculatedProperty>());
		    }
		    grouped.get(root).get(path).put(prop.name(), prop);
		}
	    }
	}
	return grouped;
    }

    /**
     * Propagates recursively the <code>enhancedType</code> from place [root; path] to place [root; ""].
     *
     * @param enhancedType -- the type to replace the current type of property "path" in "root" type
     * @param root
     * @param path
     * @param classLoader
     * @return
     */
    protected static Pair<Class<?>, Map<String, ByteArray>> propagateEnhancedTypeToRoot(final Class<?> enhancedType, final Class<?> root, final String path, final DynamicEntityClassLoader classLoader) {
	final Map<String, ByteArray> additionalByteArrays = new HashMap<String, ByteArray>();
	// add a byte array corresponding to "enhancedType"
	additionalByteArrays.put(path, new ByteArray(classLoader.getCachedByteArray(enhancedType.getName())));

	if (StringUtils.isEmpty(path)) { // replace current root type with new one
	    return new Pair<Class<?>, Map<String, ByteArray>>(enhancedType, additionalByteArrays);
	}
	final Pair<Class<?>, String> transformed = PropertyTypeDeterminator.transform(root, path);

	final String nameOfTheTypeToAdapt = transformed.getKey().getName();
	final String nameOfThePropertyToAdapt = transformed.getValue();
	try {
	    // change type if simple field and change signature in case of collectional field
	    final boolean isCollectional = Collection.class.isAssignableFrom(PropertyTypeDeterminator.determineClass(transformed.getKey(), transformed.getValue(), true, false));
	    final NewProperty propertyToBeModified = !isCollectional ? NewProperty.changeType(nameOfThePropertyToAdapt, enhancedType) : NewProperty.changeTypeSignature(nameOfThePropertyToAdapt, enhancedType);
	    final Class<?> nextEnhancedType = classLoader.startModification(nameOfTheTypeToAdapt).modifyProperties(propertyToBeModified).endModification();
	    final String nextProp = PropertyTypeDeterminator.isDotNotation(path) ? PropertyTypeDeterminator.penultAndLast(path).getKey() : "";
	    final Pair<Class<?>, Map<String, ByteArray>> lastTypeThatIsRootAndPropagatedArrays = propagateEnhancedTypeToRoot(nextEnhancedType, root, nextProp, classLoader);
	    additionalByteArrays.putAll(lastTypeThatIsRootAndPropagatedArrays.getValue());

	    return new Pair<Class<?>, Map<String, ByteArray>>(lastTypeThatIsRootAndPropagatedArrays.getKey(), additionalByteArrays);
	} catch (final ClassNotFoundException e) {
	    e.printStackTrace();
	    logger.error(e);
	    throw new RuntimeException(e);
	}
    }

    @Override
    public void discard() {
	//////////// Performs migration [originalAndEnhancedRootTypes => calculatedProperties] ////////////
	calculatedProperties.clear();
	calculatedProperties.putAll(extractAll(this));
    }

    /**
     * Extracts all calculated properties from enhanced root types.
     *
     * @param originalAndEnhancedRootTypes
     * @param dte
     * @return
     */
    protected static Map<Class<?>, List<ICalculatedProperty>> extractAll(final DomainTreeEnhancer dte) {
	final Map<Class<?>, List<ICalculatedProperty>> newCalculatedProperties = new HashMap<Class<?>, List<ICalculatedProperty>>();
	for (final Entry<Class<?>, Class<?>> originalAndEnhanced : dte.originalAndEnhancedRootTypes.entrySet()) {
	    final List<ICalculatedProperty> calc = reload(originalAndEnhanced.getValue(), originalAndEnhanced.getKey(), "", dte);
	    for (final ICalculatedProperty calculatedProperty : calc) {
		addCalculatedProperty(calculatedProperty, newCalculatedProperties, dte.originalAndEnhancedRootTypes);
	    }
	}
	return newCalculatedProperties;
    }

    /**
     * Extracts recursively <code>calculatedProperties</code> from enhanced domain <code>type</code>.
     *
     * @param type -- enhanced type to load properties
     * @param root -- not enhanced root type
     * @param path -- the path to loaded calculated props
     * @param dte
     */
    private static List<ICalculatedProperty> reload(final Class<?> type, final Class<?> root, final String path, final DomainTreeEnhancer dte) {
	final List<ICalculatedProperty> newCalcProperties = new ArrayList<ICalculatedProperty>();
	if (!DynamicEntityClassLoader.isEnhanced(type)) {
	    return newCalcProperties;
	} else {
	    // add all first level calculated properties if any exist
	    for (final Field calculatedField : Finder.findRealProperties(type, Calculated.class)) {
		final Calculated calcAnnotation = calculatedField.getAnnotation(Calculated.class);
		if (calcAnnotation != null && !StringUtils.isEmpty(calcAnnotation.value()) && AnnotationReflector.isContextual(calcAnnotation)) {
		    final Title titleAnnotation = calculatedField.getAnnotation(Title.class);
		    final String title = titleAnnotation == null ? "" : titleAnnotation.value();
		    final String desc = titleAnnotation == null ? "" : titleAnnotation.desc();
		    final ICalculatedProperty calculatedProperty = CalculatedProperty.createWithoutValidation/*createAndValidate*/(dte.getFactory(), root, calcAnnotation.contextPath(), calcAnnotation.value(), title, desc, calcAnnotation.attribute(), calcAnnotation.origination(), dte);
		    newCalcProperties.add(calculatedProperty);
		}
	    }
	    // reload all "entity-typed" and "collectional entity-typed" sub-properties if they are enhanced
	    for (final Field prop : Finder.findProperties(type)) {
		if (EntityUtils.isEntityType(prop.getType()) || EntityUtils.isCollectional(prop.getType())) {
		    final Class<?> propType = PropertyTypeDeterminator.determinePropertyType(type, prop.getName());
		    final String newPath = StringUtils.isEmpty(path) ? prop.getName() : (path + "." + prop.getName());
		    newCalcProperties.addAll(reload(propType, root, newPath, dte));
		}
	    }
	    return newCalcProperties;
	}
    }

    @Override
    public ICalculatedProperty validateCalculatedPropertyKey(final ICalculatedProperty calculatedProperty, final String pathAndName) {
	return validateCalculatedPropertyKey1(calculatedProperty, pathAndName, calculatedProperties, originalAndEnhancedRootTypes);
    }

    /**
     * Validates the calculated property key (see {@link #validatePropertyKey0(Class, String, Map)}) and checks whether another property with the same name exists (calculated or not).
     * If exists -- throws {@link IncorrectCalcPropertyKeyException}.
     *
     * @param calculatedPropertyToCheck
     * @param newPathAndName
     * @param calculatedProperties
     * @param originalAndEnhancedRootTypes
     * @return
     */
    private static ICalculatedProperty validateCalculatedPropertyKey1(final ICalculatedProperty calculatedPropertyToCheck, final String newPathAndName, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties, final Map<Class<?>, Class<?>> originalAndEnhancedRootTypes) {
	final Class<?> root = calculatedPropertyToCheck.getRoot();
	validatePropertyKey0(root, newPathAndName, originalAndEnhancedRootTypes);

	final ICalculatedProperty calculatedProperty = calculatedProperty(root, newPathAndName, calculatedProperties);
	if (calculatedProperty != null) {
	    if (calculatedProperty == calculatedPropertyToCheck) {
		// this is the same property!
	    } else {
		throw new IncorrectCalcPropertyKeyException("The calculated property with name [" + newPathAndName + "] already exists.");
	    }
	}
	try {
	    PropertyTypeDeterminator.determinePropertyType(root, newPathAndName);
	    // if (AbstractDomainTreeRepresentation.isCalculated(root, pathAndName)) {
	    //     return null; // the property with a suggested name exists in original domain, but it is "calculated", which is correct
	    // }
	} catch (final Exception e) {
	    return null; // the property with a suggested name does not exist in original domain, which is correct
	}
	throw new IncorrectCalcPropertyKeyException("The property with the name [" + newPathAndName + "] already exists in original domain (inside " + root.getSimpleName() + " root). Please try another name for calculated property.");
    }

    /**
     * Validates calculated property key [root + pathAndName] to check if 1) root exists in the domain 2) pathAndName is not empty 3) pathAndName parent exists.
     *
     * @param root
     * @param pathAndName
     * @param originalAndEnhancedRootTypes
     */
    private static void validatePropertyKey0(final Class<?> root, final String pathAndName, final Map<Class<?>, Class<?>> originalAndEnhancedRootTypes) {
	if (StringUtils.isEmpty(pathAndName)) {
	    throw new IncorrectCalcPropertyKeyException("The calculated property pathAndName cannot be empty.");
	}
	// throw exception when the place is not in the context of root type
	if (!originalAndEnhancedRootTypes.keySet().contains(root)) {
	    throw new IncorrectCalcPropertyKeyException("The calculated property [" + pathAndName + "] is not in the context of any root type.");
	}

	final Pair<String, String> pathAndName1 = PropertyTypeDeterminator.isDotNotation(pathAndName) ? PropertyTypeDeterminator.penultAndLast(pathAndName) : new Pair<String, String>("", pathAndName);
	final String path = pathAndName1.getKey();
	validatePath(root, path, "The place [" + path + "] in type [" + root.getSimpleName() + "] of calculated property does not exist.");
    }

    /**
     * Validates the calculated property key (see {@link #validatePropertyKey0(Class, String, Map)}) and checks whether calculated property with the suggested name exists.
     * If not -- throws {@link IncorrectCalcPropertyKeyException}.
     *
     * @param root
     * @param pathAndName
     * @param calculatedProperties
     * @param originalAndEnhancedRootTypes
     * @return
     */
    private static ICalculatedProperty validateCalculatedPropertyKey2(final Class<?> root, final String pathAndName, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties, final Map<Class<?>, Class<?>> originalAndEnhancedRootTypes) {
	validatePropertyKey0(root, pathAndName, originalAndEnhancedRootTypes);

	final ICalculatedProperty calculatedProperty = calculatedProperty(root, pathAndName, calculatedProperties);
	if (calculatedProperty == null) {
	    throw new IncorrectCalcPropertyKeyException("The calculated property with name [" + pathAndName + "] does not exist.");
	}
	return calculatedProperty;
    }

    protected static void validatePath(final Class<?> root, final String path, final String message) {
	if (path == null) {
	    throw new IncorrectCalcPropertyKeyException(message);
	}
	if (!"".equals(path)) { // validate path
	    try {
		PropertyTypeDeterminator.determinePropertyType(root, path); // throw exception when the place does not exist
	    } catch (final Exception e) {
		throw new IncorrectCalcPropertyKeyException(message);
	    }
	}
    }

    /**
     * Iterates through the set of calculated properties to find appropriate calc property.
     *
     * @param root
     * @param pathAndName
     * @param calculatedProperties
     * @return
     */
    private static ICalculatedProperty calculatedProperty(final Class<?> root, final String pathAndName, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties) {
	final List<ICalculatedProperty> calcProperties = calculatedProperties.get(root);
	if (calcProperties != null) {
	    for (final ICalculatedProperty prop : calcProperties) {
		if (pathAndName.equals(prop.pathAndName())) {
		    return prop;
		}
	    }
	}
	return null;
    }

    /**
     * Validates and adds calc property to a calculatedProperties.
     *
     * @param calculatedProperty
     * @param calculatedProperties
     * @param originalAndEnhancedRootTypes
     */
    private static void addCalculatedProperty(final ICalculatedProperty calculatedProperty, final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties, final Map<Class<?>, Class<?>> originalAndEnhancedRootTypes) {
	final Class<?> root = calculatedProperty.getRoot();
	validateCalculatedPropertyKey1(calculatedProperty, calculatedProperty.pathAndName(), calculatedProperties, originalAndEnhancedRootTypes);

	if (!calculatedProperties.containsKey(root)) {
	    calculatedProperties.put(root, new ArrayList<ICalculatedProperty>());
	}
	final boolean added = calculatedProperties.get(root).add(calculatedProperty);
	if (!added) {
	    logger.warn("The calculated property [" + calculatedProperty.getTitle() + "] with name [" + calculatedProperty.pathAndName() + "] is already present and is trying to be added again.");
	}
    }

    @Override
    public void addCalculatedProperty(final ICalculatedProperty calculatedProperty) {
	addCalculatedProperty(calculatedProperty, calculatedProperties, originalAndEnhancedRootTypes);
    }

    @Override
    public void addCalculatedProperty(final Class<?> root, final String contextPath, final String contextualExpression, final String title, final String desc, final CalculatedPropertyAttribute attribute, final String originationProperty) {
	final CalculatedProperty created = CalculatedProperty.createAndValidate(getFactory(), root, contextPath, contextualExpression, title, desc, attribute, originationProperty, this);
	addCalculatedProperty(created);
    }

    @Override
    public ICalculatedProperty getCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
	return validateCalculatedPropertyKey2(rootType, calculatedPropertyName, calculatedProperties, originalAndEnhancedRootTypes);
    }

    @Override
    public ICalculatedProperty copyCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
        return ((CalculatedProperty) getCalculatedProperty(rootType, calculatedPropertyName)).copy(getSerialiser());
    }

    @Override
    public void removeCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
	final ICalculatedProperty calculatedProperty = validateCalculatedPropertyKey2(rootType, calculatedPropertyName, calculatedProperties, originalAndEnhancedRootTypes);

	/////////////////
	final List<ICalculatedProperty> calcs = calculatedProperties.get(rootType);
	boolean existsInOtherExpressionsAsOriginationProperty = false;
	String containingExpression = null;
	for (final ICalculatedProperty calc : calcs) {
	    if (calc.getOriginationProperty().equals(Reflector.fromAbsotule2RelativePath(calc.getContextPath(), calculatedPropertyName))) {
		existsInOtherExpressionsAsOriginationProperty = true;
		containingExpression = calc.pathAndName();
		break;
	    }
	}
	if (existsInOtherExpressionsAsOriginationProperty) {
	    throw new IllegalArgumentException("Cannot remove a property that exists in other expressions as 'origination' property. See property [" + containingExpression + "].");
	}
	/////////////////

	final boolean removed = calculatedProperties.get(rootType).remove(calculatedProperty);

	if (!removed) {
	    throw new IllegalStateException("The property [" + calculatedPropertyName + "] has been validated but can not be removed.");
	}
	if (calculatedProperties.get(rootType).isEmpty()) {
	    calculatedProperties.remove(rootType);
	}
    }

    /**
     * A specific Kryo serialiser for {@link DomainTreeEnhancer}.
     *
     * @author TG Team
     *
     */
    public static class DomainTreeEnhancerSerialiser extends AbstractDomainTreeSerialiser<DomainTreeEnhancer> {
	public DomainTreeEnhancerSerialiser(final TgKryo kryo) {
	    super(kryo);
	}

	@Override
	public DomainTreeEnhancer read(final ByteBuffer buffer) {
	    final Set<Class<?>> rootTypes = readValue(buffer, HashSet.class);
	    final Map<Class<?>, Map<String, ByteArray>> originalAndEnhancedRootTypesArrays = readValue(buffer, HashMap.class);
	    final Map<Class<?>, List<ICalculatedProperty>> calculatedProperties = readValue(buffer, HashMap.class);
	    return new DomainTreeEnhancer(kryo(), rootTypes, originalAndEnhancedRootTypesArrays, calculatedProperties);
	}

	@Override
	public void write(final ByteBuffer buffer, final DomainTreeEnhancer domainTreeEnhancer) {
	    writeValue(buffer, domainTreeEnhancer.rootTypes);
	    writeValue(buffer, domainTreeEnhancer.originalAndEnhancedRootTypesArrays);
	    writeValue(buffer, domainTreeEnhancer.calculatedProperties);
	}
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((calculatedProperties == null) ? 0 : calculatedProperties.hashCode());
	result = prime * result + ((originalAndEnhancedRootTypesArrays == null) ? 0 : originalAndEnhancedRootTypesArrays.hashCode());
	result = prime * result + ((rootTypes == null) ? 0 : rootTypes.hashCode());
	return result;
    }

    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final DomainTreeEnhancer other = (DomainTreeEnhancer) obj;
	if (calculatedProperties == null) {
	    if (other.calculatedProperties != null) {
		return false;
	    }
	} else if (!calculatedProperties.equals(other.calculatedProperties)) {
	    return false;
	}
	if (originalAndEnhancedRootTypesArrays == null) {
	    if (other.originalAndEnhancedRootTypesArrays != null) {
		return false;
	    }
	} else if (!originalAndEnhancedRootTypesArrays.equals(other.originalAndEnhancedRootTypesArrays)) {
	    return false;
	}
	if (rootTypes == null) {
	    if (other.rootTypes != null) {
		return false;
	    }
	} else if (!rootTypes.equals(other.rootTypes)) {
	    return false;
	}
	return true;
    }

    protected Map<Class<?>, List<ICalculatedProperty>> calculatedProperties() {
        return calculatedProperties;
    }

    protected Map<Class<?>, Class<?>> getOriginalAndEnhancedRootTypes() {
        return originalAndEnhancedRootTypes;
    }
}
