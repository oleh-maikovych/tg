package ua.com.fielden.platform.swing.review.report.centre.factory;

import ua.com.fielden.platform.dao.IEntityDao;
import ua.com.fielden.platform.domaintree.centre.ICentreDomainTreeManager.ICentreDomainTreeManagerAndEnhancer;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.review.development.EntityQueryCriteria;
import ua.com.fielden.platform.swing.review.report.analysis.pivot.configuration.PivotAnalysisConfigurationModel;
import ua.com.fielden.platform.swing.review.report.analysis.pivot.configuration.PivotAnalysisConfigurationView;
import ua.com.fielden.platform.swing.review.report.centre.AbstractEntityCentre;

public class DefaultPivotAnalysisFactory<T extends AbstractEntity<?>> implements IAnalysisFactory<T, PivotAnalysisConfigurationView<T>> {

    @Override
    public PivotAnalysisConfigurationView<T> createAnalysis(final AbstractEntityCentre<T, ICentreDomainTreeManagerAndEnhancer> owner, //
	    final EntityQueryCriteria<ICentreDomainTreeManagerAndEnhancer, T, IEntityDao<T>> criteria, //
	    final String name, //
	    final BlockingIndefiniteProgressLayer progressLayer) {
	final PivotAnalysisConfigurationModel<T> analysisModel = new PivotAnalysisConfigurationModel<T>(criteria, name);
	return new PivotAnalysisConfigurationView<T>(analysisModel, owner, progressLayer);
    }

}