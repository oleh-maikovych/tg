package ua.com.fielden.platform.report.query.generation;

import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.from;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ua.com.fielden.platform.dao.QueryExecutionModel;
import ua.com.fielden.platform.domaintree.IDomainTreeEnhancer;
import ua.com.fielden.platform.domaintree.centre.ICentreDomainTreeManager.ICentreDomainTreeManagerAndEnhancer;
import ua.com.fielden.platform.domaintree.centre.IOrderingRepresentation.Ordering;
import ua.com.fielden.platform.domaintree.centre.analyses.IAnalysisDomainTreeManager;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity.query.model.ExpressionModel;
import ua.com.fielden.platform.swing.review.DynamicFetchBuilder;
import ua.com.fielden.platform.swing.review.DynamicOrderingBuilder;
import ua.com.fielden.platform.swing.review.DynamicQueryBuilder;
import ua.com.fielden.platform.swing.review.development.EntityQueryCriteriaUtils;
import ua.com.fielden.platform.utils.Pair;

public class ChartAnalysisQueryGenerator<T extends AbstractEntity<?>> implements IReportQueryGeneration<T> {

    private final Class<T> root;
    private final ICentreDomainTreeManagerAndEnhancer cdtme;
    private final IAnalysisDomainTreeManager adtm;

    public ChartAnalysisQueryGenerator(final Class<T> root, final ICentreDomainTreeManagerAndEnhancer cdtme, final IAnalysisDomainTreeManager adtm){
	this.root = root;
	this.cdtme = cdtme;
	this.adtm = adtm;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<QueryExecutionModel<T, EntityResultQueryModel<T>>> generateQueryModel() {
	final IDomainTreeEnhancer enhancer = cdtme.getEnhancer();
	final Class<T> managedType = (Class<T>)enhancer.getManagedType(root);
	final List<String> distributionProperties = adtm.getFirstTick().usedProperties(root);
	final List<String> aggregationProperties = adtm.getSecondTick().usedProperties(root);

	final EntityResultQueryModel<T> subQueryModel = DynamicQueryBuilder.createQuery(managedType, ReportQueryGenerationUtils.createQueryProperties(root, cdtme)).model();

	final List<Pair<String, ExpressionModel>> aggregation = getPropertyExpressionPair(aggregationProperties);

	final List<String> yieldProperties = new ArrayList<String>();
	yieldProperties.addAll(distributionProperties);
	yieldProperties.addAll(aggregationProperties);

	final EntityResultQueryModel<T> queryModel = DynamicQueryBuilder.createAggregationQuery(subQueryModel, ReportQueryGenerationUtils.createQueryProperties(root, cdtme), distributionProperties, aggregation).modelAsEntity(managedType);

	final List<Pair<String, Ordering>> orderingProperties = new ArrayList<>(adtm.getSecondTick().orderedProperties(root));
	final List<Pair<Object, Ordering>> orderingPairs = EntityQueryCriteriaUtils.getOrderingList(root, //
		orderingProperties, //
		enhancer);

	if(orderingPairs.isEmpty()){
	    for(final String groupOrder : distributionProperties){
		orderingPairs.add(new Pair<Object, Ordering>(groupOrder, Ordering.ASCENDING));
	    }
	}

	final QueryExecutionModel<T, EntityResultQueryModel<T>> resultQuery = from(queryModel)
	.with(DynamicOrderingBuilder.createOrderingModel(managedType, orderingPairs))//
	.with(DynamicFetchBuilder.createFetchModel(managedType, new HashSet<String>(yieldProperties))).model();

	final List<QueryExecutionModel<T, EntityResultQueryModel<T>>> result = new ArrayList<>();
	result.add(resultQuery);
	return result;
    }



    /**
     * Returns the list of property name and it's expression model pairs.
     *
     * @param propertyForExpression
     * @return
     */
    private List<Pair<String, ExpressionModel>> getPropertyExpressionPair(final List<String> propertyForExpression){
        final IDomainTreeEnhancer enhancer = cdtme.getEnhancer();
        final List<Pair<String, ExpressionModel>> propertyExpressionPair = new ArrayList<>();
        for (final String property : propertyForExpression) {
            final ExpressionModel expression = EntityQueryCriteriaUtils.getExpressionForProp(root, property, enhancer);
            propertyExpressionPair.add(new Pair<>(property, expression));
        }
        return propertyExpressionPair;
    }
}