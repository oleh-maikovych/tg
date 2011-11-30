package ua.com.fielden.platform.swing.review.report.centre;

import javax.swing.JComponent;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.review.report.analysis.grid.configuration.GridConfigurationModel;
import ua.com.fielden.platform.swing.review.report.analysis.grid.configuration.GridConfigurationPanel;

public class SingleAnalysisEntityCentre<T extends AbstractEntity> extends EntityCentre<T> {

    private static final long serialVersionUID = -4025190200012481751L;

    public SingleAnalysisEntityCentre(final EntityCentreModel<T> model, final BlockingIndefiniteProgressLayer progressLayer) {
	super(model, progressLayer);
    }

    @Override
    protected JComponent createReview() {
	final GridConfigurationModel<T> configModel = getModel().createMainDetailsModel();
	final BlockingIndefiniteProgressLayer progressLayer = new BlockingIndefiniteProgressLayer(null, "");
	final GridConfigurationPanel<T> gridConfigView = new GridConfigurationPanel<T>(configModel, progressLayer);
	progressLayer.setView(gridConfigView);
	gridConfigView.open();
	configModel.select();
	return progressLayer;
    }
}
