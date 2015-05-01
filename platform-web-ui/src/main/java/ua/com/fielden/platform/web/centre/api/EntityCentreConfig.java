package ua.com.fielden.platform.web.centre.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import ua.com.fielden.platform.basic.IValueMatcherWithCentreContext;
import ua.com.fielden.platform.basic.autocompleter.FallbackValueMatcherWithCentreContext;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.fetch.IFetchProvider;
import ua.com.fielden.platform.utils.Pair;
import ua.com.fielden.platform.web.centre.api.actions.EntityActionConfig;
import ua.com.fielden.platform.web.centre.api.context.CentreContextConfig;
import ua.com.fielden.platform.web.centre.api.crit.defaults.assigners.IValueAssigner;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.MultiCritBooleanValueMnemonic;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.MultiCritStringValueMnemonic;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.RangeCritDateValueMnemonic;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.RangeCritOtherValueMnemonic;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.SingleCritDateValueMnemonic;
import ua.com.fielden.platform.web.centre.api.crit.defaults.mnemonics.SingleCritOtherValueMnemonic;
import ua.com.fielden.platform.web.centre.api.query_enhancer.IQueryEnhancer;
import ua.com.fielden.platform.web.centre.api.resultset.ICustomPropsAssignmentHandler;
import ua.com.fielden.platform.web.centre.api.resultset.IRenderingCustomiser;
import ua.com.fielden.platform.web.centre.api.resultset.PropDef;
import ua.com.fielden.platform.web.layout.FlexLayout;

/**
 *
 * Represents a final structure of an entity centre as produced by means of using Entity Centre DSL.
 *
 * @author TG Team
 *
 */
public class EntityCentreConfig<T extends AbstractEntity<?>> {
    /////////////////////////////////////////////
    ///////////// TOP LEVEL ACTIONS /////////////
    /////////////////////////////////////////////

    /**
     * A list of top level actions represented as pairs of action configurations as keys and optional group names as values.
     * <p>
     * If an action belongs to a group then a corresponding value in the pair contains the name of that group. Otherwise, the pair value is empty. The order of actions in the list
     * is important and should be honoured when building their UI representation.
     */
    private final List<Pair<EntityActionConfig, Optional<String>>> topLevelActions = new ArrayList<>();

    /////////////////////////////////////////////
    ////////////// SELECTION CRIT ///////////////
    /////////////////////////////////////////////

    /**
     * A list of properties that have been added to selection criteria in the added sequential order.
     * <p>
     * It is not important whether a property was added as multi-valued, single-valued or range criterion simply because an appropriate selection criteria kind gets determined
     * automatically from property declaration at the entity type level.
     * <p>
     * The part of Entity Centre DSL that provides developer with ability to pick the kind (i.e. <code>multi()</code>, <code>single()</code> or <code>range()</code>)is there only
     * to facilitate definition fluency and readability.
     */
    private final List<String> selectionCriteria = new ArrayList<>();

    /**
     * Default value assigner for various kind and types of selection criteria.
     */
    private final Map<String, Class<? extends IValueAssigner<MultiCritStringValueMnemonic, T>>> defaultMultiValueAssignersForEntityAndStringSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<MultiCritBooleanValueMnemonic, T>>> defaultMultiValueAssignersForBooleanSelectionCriteria = new HashMap<>();

    private final Map<String, Class<? extends IValueAssigner<RangeCritDateValueMnemonic, T>>> defaultRangeValueAssignersForDateSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<Integer>, T>>> defaultRangeValueAssignersForIntegerSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<BigDecimal>, T>>> defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria = new HashMap<>();

    private final Map<String, Class<? extends IValueAssigner<? extends SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>, T>>> defaultSingleValueAssignersForEntitySelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<String>, T>>> defaultSingleValueAssignersForStringSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Boolean>, T>>> defaultSingleValueAssignersForBooleanSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Integer>, T>>> defaultSingleValueAssignersForIntegerSelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<BigDecimal>, T>>> defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria = new HashMap<>();
    private final Map<String, Class<? extends IValueAssigner<SingleCritDateValueMnemonic, T>>> defaultSingleValueAssignersForDateSelectionCriteria = new HashMap<>();

    /**
     * Default values. At the DSL level default values and assigners are mutually exclusive.
     */
    private final Map<String, MultiCritStringValueMnemonic> defaultMultiValuesForEntityAndStringSelectionCriteria = new HashMap<>();
    private final Map<String, MultiCritBooleanValueMnemonic> defaultMultiValuesForBooleanSelectionCriteria = new HashMap<>();

    private final Map<String, RangeCritDateValueMnemonic> defaultRangeValuesForDateSelectionCriteria = new HashMap<>();
    private final Map<String, RangeCritOtherValueMnemonic<Integer>> defaultRangeValuesForIntegerSelectionCriteria = new HashMap<>();
    private final Map<String, RangeCritOtherValueMnemonic<BigDecimal>> defaultRangeValuesForBigDecimalAndMoneySelectionCriteria = new HashMap<>();

    private final Map<String, SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>> defaultSingleValuesForEntitySelectionCriteria = new HashMap<>();
    private final Map<String, SingleCritOtherValueMnemonic<String>> defaultSingleValuesForStringSelectionCriteria = new HashMap<>();
    private final Map<String, SingleCritOtherValueMnemonic<Boolean>> defaultSingleValuesForBooleanSelectionCriteria = new HashMap<>();
    private final Map<String, SingleCritOtherValueMnemonic<Integer>> defaultSingleValuesForIntegerSelectionCriteria = new HashMap<>();
    private final Map<String, SingleCritOtherValueMnemonic<BigDecimal>> defaultSingleValuesForBigDecimalAndMoneySelectionCriteria = new HashMap<>();
    private final Map<String, SingleCritDateValueMnemonic> defaultSingleValuesForDateSelectionCriteria = new HashMap<>();

    /**
     * A map between selection criteria properties and their custom value matchers. If a matcher for some criterion is not provided then a default instance of type
     * {@link FallbackValueMatcherWithCentreContext} should be used.
     */
    private final Map<String, Pair<Class<? extends IValueMatcherWithCentreContext<? extends AbstractEntity<?>>>, Optional<CentreContextConfig>>> valueMatchersForSelectionCriteria = new HashMap<>();

    /**
     * Represents the layout settings for selection criteria.
     */
    private final FlexLayout selectionCriteriaLayout;

    /////////////////////////////////////////////
    ////////////////// RESULT SET ///////////////
    /////////////////////////////////////////////

    /**
     * A list of result set property definitions, presented in the same order a specified using Entity Centre DSL. Natural (persistent or calculated) properties are intertwined
     * with custom properties.
     */
    private final List<ResultSetProp> resultSetProperties = new ArrayList<>();

    /**
     * A convenient structure to capture result set property definition. It includes either a property name that represents a natural (persistent or calculated) property, or a
     * custom property definition. The structure guarantees that natural and custom properties are mutually exclusive.
     * <p>
     * In any of those cases, a custom action can be provided. The custom action value is optional and can be empty if there is no need to provide custom actions specific for
     * represented in the result set properties. However, the default actions would still get associated with all properties without a custom action. In order to skip even the
     * default action, a <code>no action</code> configuration needs to set as custom property action.
     */
    public static class ResultSetProp {
        public final Optional<String> propName;
        public final Optional<PropDef<?>> propDef;
        public final Optional<EntityActionConfig> propAction;

        public static ResultSetProp propByName(final String propName, final EntityActionConfig propAction) {
            return new ResultSetProp(propName, null, propAction);
        }

        public static ResultSetProp propByDef(final PropDef<?> propDef, final EntityActionConfig propAction) {
            return new ResultSetProp(null, propDef, propAction);
        }

        private ResultSetProp(
                final String propName,
                final PropDef<?> propDef,
                final EntityActionConfig propAction) {

            if (propName != null && propDef != null) {
                throw new IllegalArgumentException("Only one of property name or property definition should be provided.");
            }

            if (StringUtils.isEmpty(propName) && propDef == null) {
                throw new IllegalArgumentException("Either property name or property definition should be provided.");
            }

            this.propName = Optional.ofNullable(propName);
            this.propDef = Optional.ofNullable(propDef);
            this.propAction = Optional.ofNullable(propAction);
        }

    }

    /**
     * A map between properties to order by and the ordering direction. The order of elements in this map corresponds to the ordering sequence. That is, the first listed property
     * should be the first in the resultant order statement, the second -- second, and so on.
     */
    private final LinkedHashMap<String, OrderDirection> resultSetOrdering = new LinkedHashMap<>();

    /**
     * This is just a helper enumeration to express result set ordering.
     */
    public static enum OrderDirection {
        DESC, ASC;
    }

    /**
     * A primary entity action configuration that is associated with every retrieved and present in the result set entity. It can be <code>null</code> if no primary entity action
     * is needed.
     */
    private final EntityActionConfig resultSetPrimaryEntityAction;

    /**
     * A list of secondary action configurations that are associated with every retrieved and present in the result set entity. It can be empty if no secondary action are
     * necessary.
     */
    private final List<EntityActionConfig> resultSetSecondaryEntityActions = new ArrayList<>();

    /**
     * Represents a type of a contract that is responsible for customisation of rendering for entities and their properties.
     */
    private final Class<? extends IRenderingCustomiser<? extends AbstractEntity<?>, ?>> resultSetRenderingCustomiserType;

    /**
     * Represents a type of a contract that is responsible for assigning values to custom properties as part of the data retrieval process.
     */
    private final Class<? extends ICustomPropsAssignmentHandler<? extends AbstractEntity<?>>> resultSetCustomPropAssignmentHandlerType;

    ////////////////////////////////////////////////
    ///////// QUERY ENHANCER AND FETCH /////////////
    ////////////////////////////////////////////////

    private final Pair<Class<? extends IQueryEnhancer<T>>, Optional<CentreContextConfig>> queryEnhancerConfig;
    private final IFetchProvider<T> fetchProvider;

    ///////////////////////////////////
    ///////// CONSTRUCTOR /////////////
    ///////////////////////////////////
    public EntityCentreConfig(
            final List<Pair<EntityActionConfig, Optional<String>>> topLevelActions,
            final List<String> selectionCriteria,
            final Map<String, Class<? extends IValueAssigner<MultiCritStringValueMnemonic, T>>> defaultMultiValueAssignersForEntityAndStringSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<MultiCritBooleanValueMnemonic, T>>> defaultMultiValueAssignersForBooleanSelectionCriteria,

            final Map<String, Class<? extends IValueAssigner<RangeCritDateValueMnemonic, T>>> defaultRangeValueAssignersForDateSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<Integer>, T>>> defaultRangeValueAssignersForIntegerSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<BigDecimal>, T>>> defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria,

            final Map<String, Class<? extends IValueAssigner<? extends SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>, T>>> defaultSingleValueAssignersForEntitySelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<String>, T>>> defaultSingleValueAssignersForStringSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Boolean>, T>>> defaultSingleValueAssignersForBooleanSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Integer>, T>>> defaultSingleValueAssignersForIntegerSelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<BigDecimal>, T>>> defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria,
            final Map<String, Class<? extends IValueAssigner<SingleCritDateValueMnemonic, T>>> defaultSingleValueAssignersForDateSelectionCriteria,

            final Map<String, MultiCritStringValueMnemonic> defaultMultiValuesForEntityAndStringSelectionCriteria,
            final Map<String, MultiCritBooleanValueMnemonic> defaultMultiValuesForBooleanSelectionCriteria,

            final Map<String, RangeCritDateValueMnemonic> defaultRangeValuesForDateSelectionCriteria,
            final Map<String, RangeCritOtherValueMnemonic<Integer>> defaultRangeValuesForIntegerSelectionCriteria,
            final Map<String, RangeCritOtherValueMnemonic<BigDecimal>> defaultRangeValuesForBigDecimalAndMoneySelectionCriteria,

            final Map<String, SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>> defaultSingleValuesForEntitySelectionCriteria,
            final Map<String, SingleCritOtherValueMnemonic<String>> defaultSingleValuesForStringSelectionCriteria,
            final Map<String, SingleCritOtherValueMnemonic<Boolean>> defaultSingleValuesForBooleanSelectionCriteria,
            final Map<String, SingleCritOtherValueMnemonic<Integer>> defaultSingleValuesForIntegerSelectionCriteria,
            final Map<String, SingleCritOtherValueMnemonic<BigDecimal>> defaultSingleValuesForBigDecimalAndMoneySelectionCriteria,
            final Map<String, SingleCritDateValueMnemonic> defaultSingleValuesForDateSelectionCriteria,

            final Map<String, Pair<Class<? extends IValueMatcherWithCentreContext<? extends AbstractEntity<?>>>, Optional<CentreContextConfig>>> valueMatchersForSelectionCriteria,
            final FlexLayout selectionCriteriaLayout,
            final List<ResultSetProp> resultSetProperties,
            final LinkedHashMap<String, OrderDirection> resultSetOrdering,
            final EntityActionConfig resultSetPrimaryEntityAction,
            final List<EntityActionConfig> resultSetSecondaryEntityActions,
            final Class<? extends IRenderingCustomiser<? extends AbstractEntity<?>, ?>> resultSetRenderingCustomiserType,
            final Class<? extends ICustomPropsAssignmentHandler<? extends AbstractEntity<?>>> resultSetCustomPropAssignmentHandlerType,
            final Pair<Class<? extends IQueryEnhancer<T>>, Optional<CentreContextConfig>> queryEnhancerConfig,
            final IFetchProvider<T> fetchProvider) {
        this.topLevelActions.addAll(topLevelActions);
        this.selectionCriteria.addAll(selectionCriteria);
        this.defaultMultiValueAssignersForEntityAndStringSelectionCriteria.putAll(defaultMultiValueAssignersForEntityAndStringSelectionCriteria);
        this.defaultMultiValueAssignersForBooleanSelectionCriteria.putAll(defaultMultiValueAssignersForBooleanSelectionCriteria);

        this.defaultRangeValueAssignersForDateSelectionCriteria.putAll(defaultRangeValueAssignersForDateSelectionCriteria);
        this.defaultRangeValueAssignersForIntegerSelectionCriteria.putAll(defaultRangeValueAssignersForIntegerSelectionCriteria);
        this.defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria.putAll(defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria);

        this.defaultSingleValueAssignersForEntitySelectionCriteria.putAll(defaultSingleValueAssignersForEntitySelectionCriteria);
        this.defaultSingleValueAssignersForStringSelectionCriteria.putAll(defaultSingleValueAssignersForStringSelectionCriteria);
        this.defaultSingleValueAssignersForBooleanSelectionCriteria.putAll(defaultSingleValueAssignersForBooleanSelectionCriteria);
        this.defaultSingleValueAssignersForIntegerSelectionCriteria.putAll(defaultSingleValueAssignersForIntegerSelectionCriteria);
        this.defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria.putAll(defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria);
        this.defaultSingleValueAssignersForDateSelectionCriteria.putAll(defaultSingleValueAssignersForDateSelectionCriteria);

        this.defaultMultiValuesForEntityAndStringSelectionCriteria.putAll(defaultMultiValuesForEntityAndStringSelectionCriteria);
        this.defaultMultiValuesForBooleanSelectionCriteria.putAll(defaultMultiValuesForBooleanSelectionCriteria);

        this.defaultRangeValuesForDateSelectionCriteria.putAll(defaultRangeValuesForDateSelectionCriteria);
        this.defaultRangeValuesForIntegerSelectionCriteria.putAll(defaultRangeValuesForIntegerSelectionCriteria);
        this.defaultRangeValuesForBigDecimalAndMoneySelectionCriteria.putAll(defaultRangeValuesForBigDecimalAndMoneySelectionCriteria);

        this.defaultSingleValuesForEntitySelectionCriteria.putAll(defaultSingleValuesForEntitySelectionCriteria);
        this.defaultSingleValuesForStringSelectionCriteria.putAll(defaultSingleValuesForStringSelectionCriteria);
        this.defaultSingleValuesForBooleanSelectionCriteria.putAll(defaultSingleValuesForBooleanSelectionCriteria);
        this.defaultSingleValuesForIntegerSelectionCriteria.putAll(defaultSingleValuesForIntegerSelectionCriteria);
        this.defaultSingleValuesForBigDecimalAndMoneySelectionCriteria.putAll(defaultSingleValuesForBigDecimalAndMoneySelectionCriteria);
        this.defaultSingleValuesForDateSelectionCriteria.putAll(defaultSingleValuesForDateSelectionCriteria);

        this.valueMatchersForSelectionCriteria.putAll(valueMatchersForSelectionCriteria);
        this.selectionCriteriaLayout = selectionCriteriaLayout;
        this.resultSetProperties.addAll(resultSetProperties);
        this.resultSetOrdering.putAll(resultSetOrdering);
        this.resultSetPrimaryEntityAction = resultSetPrimaryEntityAction;
        this.resultSetSecondaryEntityActions.addAll(resultSetSecondaryEntityActions);
        this.resultSetRenderingCustomiserType = resultSetRenderingCustomiserType;
        this.resultSetCustomPropAssignmentHandlerType = resultSetCustomPropAssignmentHandlerType;
        this.queryEnhancerConfig = queryEnhancerConfig;
        this.fetchProvider = fetchProvider;
    }

    ///////////////////////////////////////////
    /////////////// GETTERS ///////////////////
    ///////////////////////////////////////////
    /**
     * Provides access to the layout settings of selection criteria.
     *
     * @return
     */
    public FlexLayout getSelectionCriteriaLayout() {
        return selectionCriteriaLayout;
    }

    public Optional<Pair<Class<? extends IQueryEnhancer<T>>, Optional<CentreContextConfig>>> getQueryEnhancerConfig() {
        return Optional.ofNullable(queryEnhancerConfig);
    }

    public Optional<IFetchProvider<T>> getFetchProvider() {
        return Optional.ofNullable(fetchProvider);
    }

    public Optional<EntityActionConfig> getResultSetPrimaryEntityAction() {
        return Optional.ofNullable(resultSetPrimaryEntityAction);
    }

    public Optional<List<EntityActionConfig>> getResultSetSecondaryEntityActions() {
        if (resultSetSecondaryEntityActions == null || resultSetSecondaryEntityActions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableList(resultSetSecondaryEntityActions));
    }

    public Optional<List<ResultSetProp>> getResultSetProperties() {
        if (resultSetProperties == null || resultSetProperties.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableList(resultSetProperties));
    }

    public Optional<Map<String, Pair<Class<? extends IValueMatcherWithCentreContext<? extends AbstractEntity<?>>>, Optional<CentreContextConfig>>>> getValueMatchersForSelectionCriteria() {
        if (valueMatchersForSelectionCriteria == null || valueMatchersForSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(valueMatchersForSelectionCriteria));
    }

    public Optional<List<String>> getSelectionCriteria() {
        if (selectionCriteria == null || selectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableList(selectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<MultiCritStringValueMnemonic, T>>>> getDefaultMultiValueAssignersForEntityAndStringSelectionCriteria() {
        if (defaultMultiValueAssignersForEntityAndStringSelectionCriteria == null || defaultMultiValueAssignersForEntityAndStringSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultMultiValueAssignersForEntityAndStringSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<RangeCritDateValueMnemonic, T>>>> getDefaultRangeValueAssignersForDateSelectionCriteria() {
        if (defaultRangeValueAssignersForDateSelectionCriteria == null || defaultRangeValueAssignersForDateSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValueAssignersForDateSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<Integer>, T>>>> getDefaultRangeValueAssignersForIntegerSelectionCriteria() {
        if (defaultRangeValueAssignersForIntegerSelectionCriteria == null || defaultRangeValueAssignersForIntegerSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValueAssignersForIntegerSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<RangeCritOtherValueMnemonic<BigDecimal>, T>>>> getDefaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria() {
        if (defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria == null || defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValueAssignersForBigDecimalAndMoneySelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<? extends SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>, T>>>> getDefaultSingleValueAssignersForEntitySelectionCriteria() {
        if (defaultSingleValueAssignersForEntitySelectionCriteria == null || defaultSingleValueAssignersForEntitySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForEntitySelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<String>, T>>>> getDefaultSingleValueAssignersForStringSelectionCriteria() {
        if (defaultSingleValueAssignersForStringSelectionCriteria == null || defaultSingleValueAssignersForStringSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForStringSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Boolean>, T>>>> getDefaultSingleValueAssignersForBooleanSelectionCriteria() {
        if (defaultSingleValueAssignersForBooleanSelectionCriteria == null || defaultSingleValueAssignersForBooleanSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForBooleanSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<Integer>, T>>>> getDefaultSingleValueAssignersForIntegerSelectionCriteria() {
        if (defaultSingleValueAssignersForIntegerSelectionCriteria == null || defaultSingleValueAssignersForIntegerSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForIntegerSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<SingleCritOtherValueMnemonic<BigDecimal>, T>>>> getDefaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria() {
        if (defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria == null || defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForBigDecimalAndMoneySelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<SingleCritDateValueMnemonic, T>>>> getDefaultSingleValueAssignersForDateSelectionCriteria() {
        if (defaultSingleValueAssignersForDateSelectionCriteria == null || defaultSingleValueAssignersForDateSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValueAssignersForDateSelectionCriteria));
    }

    public Optional<Map<String, Class<? extends IValueAssigner<MultiCritBooleanValueMnemonic, T>>>> getDefaultMultiValueAssignersForBooleanSelectionCriteria() {
        if (defaultMultiValueAssignersForBooleanSelectionCriteria == null || defaultMultiValueAssignersForBooleanSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultMultiValueAssignersForBooleanSelectionCriteria));
    }

    public Optional<Map<String, MultiCritStringValueMnemonic>> getDefaultMultiValuesForEntityAndStringSelectionCriteria() {
        if (defaultMultiValuesForEntityAndStringSelectionCriteria == null || defaultMultiValuesForEntityAndStringSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultMultiValuesForEntityAndStringSelectionCriteria));
    }

    public Optional<Map<String, MultiCritBooleanValueMnemonic>> getDefaultMultiValuesForBooleanSelectionCriteria() {
        if (defaultMultiValuesForBooleanSelectionCriteria == null || defaultMultiValuesForBooleanSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultMultiValuesForBooleanSelectionCriteria));
    }

    public Optional<Map<String, RangeCritDateValueMnemonic>> getDefaultRangeValuesForDateSelectionCriteria() {
        if (defaultRangeValuesForDateSelectionCriteria == null || defaultRangeValuesForDateSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValuesForDateSelectionCriteria));
    }

    public Optional<Map<String, RangeCritOtherValueMnemonic<Integer>>> getDefaultRangeValuesForIntegerSelectionCriteria() {
        if (defaultRangeValuesForIntegerSelectionCriteria == null || defaultRangeValuesForIntegerSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValuesForIntegerSelectionCriteria));
    }

    public Optional<Map<String, RangeCritOtherValueMnemonic<BigDecimal>>> getDefaultRangeValuesForBigDecimalAndMoneySelectionCriteria() {
        if (defaultRangeValuesForBigDecimalAndMoneySelectionCriteria == null || defaultRangeValuesForBigDecimalAndMoneySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultRangeValuesForBigDecimalAndMoneySelectionCriteria));
    }

    public Optional<Map<String, SingleCritOtherValueMnemonic<? extends AbstractEntity<?>>>> getDefaultSingleValuesForEntitySelectionCriteria() {
        if (defaultSingleValuesForEntitySelectionCriteria == null || defaultSingleValuesForEntitySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForEntitySelectionCriteria));
    }

    public Optional<Map<String, SingleCritOtherValueMnemonic<String>>> getDefaultSingleValuesForStringSelectionCriteria() {
        if (defaultSingleValuesForStringSelectionCriteria == null || defaultSingleValuesForStringSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForStringSelectionCriteria));
    }

    public Optional<Map<String, SingleCritOtherValueMnemonic<Boolean>>> getDefaultSingleValuesForBooleanSelectionCriteria() {
        if (defaultSingleValuesForBooleanSelectionCriteria == null || defaultSingleValuesForBooleanSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForBooleanSelectionCriteria));
    }

    public Optional<Map<String, SingleCritOtherValueMnemonic<Integer>>> getDefaultSingleValuesForIntegerSelectionCriteria() {
        if (defaultSingleValuesForIntegerSelectionCriteria == null || defaultSingleValuesForIntegerSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForIntegerSelectionCriteria));
    }

    public Optional<Map<String, SingleCritOtherValueMnemonic<BigDecimal>>> getDefaultSingleValuesForBigDecimalAndMoneySelectionCriteria() {
        if (defaultSingleValuesForBigDecimalAndMoneySelectionCriteria == null || defaultSingleValuesForBigDecimalAndMoneySelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForBigDecimalAndMoneySelectionCriteria));
    }

    public Optional<Map<String, SingleCritDateValueMnemonic>> getDefaultSingleValuesForDateSelectionCriteria() {
        if (defaultSingleValuesForDateSelectionCriteria == null || defaultSingleValuesForDateSelectionCriteria.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(defaultSingleValuesForDateSelectionCriteria));
    }

    public Optional<Class<? extends IRenderingCustomiser<? extends AbstractEntity<?>, ?>>> getResultSetRenderingCustomiserType() {
        return Optional.ofNullable(resultSetRenderingCustomiserType);
    }

    public Optional<Map<String, OrderDirection>> getResultSetOrdering() {
        if (resultSetOrdering == null || resultSetOrdering.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableMap(resultSetOrdering));
    }

    public Optional<Class<? extends ICustomPropsAssignmentHandler<? extends AbstractEntity<?>>>> getResultSetCustomPropAssignmentHandlerType() {
        return Optional.ofNullable(resultSetCustomPropAssignmentHandlerType);
    }

    public Optional<List<Pair<EntityActionConfig, Optional<String>>>> getTopLevelActions() {
        if (topLevelActions == null || topLevelActions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableList(topLevelActions));
    }
}
