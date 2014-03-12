package ua.com.fielden.platform.javafx.dashboard2;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import ua.com.fielden.platform.dao.IComputationMonitor;
import ua.com.fielden.platform.dashboard.IDashboardItemResult;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.swing.actions.BlockingLayerCommand;
import ua.com.fielden.platform.swing.actions.Command;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.model.IUmViewOwner;
import ua.com.fielden.platform.swing.review.DynamicQueryBuilder.QueryProperty;
import ua.com.fielden.platform.swing.utils.SwingUtilitiesEx;

import com.google.inject.Inject;

/**
 * A general implementation for dashboard item with a capability of refreshing item result on background and other functionality.
 *
 * @author TG Team
 *
 * @param <RESULT>
 */
public abstract class AbstractDashboardItem <RESULT extends IDashboardItemResult, UI extends JFXPanel & IDashboardItemUi<RESULT> & IUmViewOwner> implements IDashboardItem<RESULT, UI> {
    private final BlockingIndefiniteProgressLayer mainLayer;
    private final UI ui;
    private final IDashboardParamsGetter paramsGetter;
    private final Class<? extends AbstractEntity<?>> mainType;
    private final Runnable runAndDisplayAction;
    private Timer refreshTimer;

    @Inject
    public AbstractDashboardItem(final IDashboardParamsGetter paramsGetter, final IComputationMonitor computationMonitor, final Class<? extends AbstractEntity<?>> mainType) {
	this.paramsGetter = paramsGetter;
	this.mainType = mainType;
	ui = createUi(runAndDisplayAction = new Runnable() {
	    @Override
	    public void run() {
		runAndDisplay(AbstractDashboardItem.this.paramsGetter.getCustomParams(AbstractDashboardItem.this.mainType));
	    }
	}, new Runnable() {
	    @Override
	    public void run() {
		configure();
	    }
	},
	new Runnable() {
	    @Override
	    public void run() {
		invokeErrorDetails();
	    }
	}, new Runnable() {
	    @Override
	    public void run() {
		invokeWarningDetails();
	    }
	}, new Runnable() {
	    @Override
	    public void run() {
		invokeRegularDetails();
	    }
	});
	mainLayer = new BlockingIndefiniteProgressLayer(ui, "Loading...", computationMonitor);
	ui.setUpperComponent(mainLayer);
    }

    /**
     * Creates specific dashboard UI instance.
     *
     * @param errorAction
     * @param warningAction
     * @param regularAction
     * @return
     */
    protected abstract UI createUi(final Runnable runAndDisplayAction, final Runnable configureAction, final Runnable errorAction, final Runnable warningAction, final Runnable regularAction);

    /**
     * Refreshes item information using custom parameters.
     *
     * @param customParameters
     * @return
     */
    protected abstract RESULT refresh(final List<QueryProperty> customParameters);

    /** This should be strictly on EDT. */
    @Override
    public final void runAndDisplay(final List<QueryProperty> customParameters) {
        final Command<RESULT> command = new BlockingLayerCommand<RESULT>("Run and display alert", getMainLayer()) {
	    private static final long serialVersionUID = 1L;

//	    @Override
//            protected boolean preAction() {
//                Dialogs.showMessageDialog(getMainLayer(), "Pre-action of the custom command.", "Custom command", Dialogs.INFORMATION_MESSAGE);
//                return super.preAction();
//            }

            @Override
            protected RESULT action(final ActionEvent e) {
                setMessage("Refreshing...");
                final RESULT result = refresh(customParameters);
                setMessage("Completed");
                return result;
            }

            @Override
            protected void postAction(final RESULT result) {
        	Platform.runLater(new Runnable() {
        	    @Override public void run() {
        		ui.update(result);
        	    }
        	});

            	reScheduleRefreshAction();
		super.postAction(result);
            }
        };
        command.setEnabled(true);
        command.actionPerformed(null);
    }

    @Override
    public UI getUi() {
        return ui;
    }

    public BlockingIndefiniteProgressLayer getMainLayer() {
	return mainLayer;
    }

    protected IDashboardParamsGetter getParamsGetter() {
	return paramsGetter;
    }

    @Override
    public Class<? extends AbstractEntity<?>> mainType() {
        return mainType;
    }

    protected Runnable getRunAndDisplayAction() {
	return runAndDisplayAction;
    }

    /**
     * Discards all existing tasks (if any) and schedules new Refresh task.
     */
    public void reScheduleRefreshAction() {
	if (refreshTimer != null) {
	    refreshTimer.cancel();
	    refreshTimer = null;
	}
	refreshTimer = new Timer();
	refreshTimer.schedule(new TimerTask() {
	    @Override
	    public void run() {
		refreshTimer = null;
		SwingUtilitiesEx.invokeLater(runAndDisplayAction);
	    }
	}, 600000); // 10 min
    }
}