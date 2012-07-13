package ua.com.fielden.platform.swing.review.report.configuration;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;
import ua.com.fielden.platform.error.Result;
import ua.com.fielden.platform.swing.actions.BlockingLayerCommand;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.review.development.SelectableAndLoadBasePanel;
import ua.com.fielden.platform.swing.review.report.ReportMode;
import ua.com.fielden.platform.swing.review.report.events.AbstractConfigurationViewEvent;
import ua.com.fielden.platform.swing.review.report.events.AbstractConfigurationViewEvent.AbstractConfigurationViewEventAction;
import ua.com.fielden.platform.swing.review.report.events.LoadEvent;
import ua.com.fielden.platform.swing.review.report.interfaces.IAbstractConfigurationViewEventListener;
import ua.com.fielden.platform.swing.review.report.interfaces.ILoadListener;
import ua.com.fielden.platform.swing.review.report.interfaces.IReview;
import ua.com.fielden.platform.swing.review.report.interfaces.IWizard;
import ua.com.fielden.platform.swing.utils.SwingUtilitiesEx;

/**
 * The holder for wizard and view panels. Provides functionality that allows one to switch view between report and wizard modes.
 *
 * @author TG Team
 *
 * @param <VT>
 * @param <WT>
 */
public abstract class AbstractConfigurationView<VT extends SelectableAndLoadBasePanel & IReview, WT extends SelectableAndLoadBasePanel & IWizard> extends SelectableAndLoadBasePanel{

    private static final long serialVersionUID = 362789325125491283L;

    private final AbstractConfigurationModel model;
    private final BlockingIndefiniteProgressLayer progressLayer;

    /**
     * The action that is responsible for opening configuration view.
     */
    private final Action openAction;

    /**
     * Holds the previous wizard and view of the report.
     */
    private WT previousWizard = null;
    private VT previousView = null;

    private boolean wasResized;
    private boolean wasChildLoaded;

    /**
     * Initiates this {@link AbstractConfigurationView} with associated {@link AbstractConfigurationModel}.
     *
     * @param model
     */
    public AbstractConfigurationView(final AbstractConfigurationModel model, final BlockingIndefiniteProgressLayer progressLayer){
	super(new MigLayout("fill, insets 0", "[fill, grow]", "[fill, grow]"));
	this.model = model;
	this.progressLayer = progressLayer;
	this.openAction = createOpenAction();
	wasResized = false;
	wasChildLoaded = false;
	addComponentListener(createComponentWasResized());
	model.addPropertyChangeListener(createModeChangeListener());
    }

    /**
     * Registers the specified {@link IAbstractConfigurationViewEventListener}.
     *
     * @param l
     */
    public void addOpenEventListener(final IAbstractConfigurationViewEventListener l){
	listenerList.add(IAbstractConfigurationViewEventListener.class, l);
    }

    /**
     * Unregisters the specified {@link IAbstractConfigurationViewEventListener}.
     *
     * @param l
     */
    public void removeOpenEventListener(final IAbstractConfigurationViewEventListener l){
	listenerList.remove(IAbstractConfigurationViewEventListener.class, l);
    }

    /**
     * Returns the previous configurable review. If this configuration panel is in the report mode then this method returns currently visible entity review.
     *
     * @return
     */
    public VT getPreviousView() {
	return previousView;
    }

    /**
     * Returns the previous wizard view. If this configuration panel is in the wizard mode then this method returns currently visible wizard.
     *
     * @return
     */
    public WT getPreviousWizard() {
	return previousWizard;
    }

    /**
     * Returns the associated {@link AbstractConfigurationModel}.
     *
     * @return
     */
    public AbstractConfigurationModel getModel() {
	return model;
    }

    @Override
    public String getInfo() {
	return "Abstract configuration panel";
    }

    /**
     * Returns the progress layer for the associated {@link AbstractConfigurationView}.
     *
     * @return
     */
    public final BlockingIndefiniteProgressLayer getProgressLayer() {
	return progressLayer;
    }

    /**
     * Opens this {@link AbstractConfigurationView}. First it tries to open this in {@link ReportMode#REPORT} mode, if it fails, then it opens in {@link ReportMode#WIZARD} mode.
     */
    public final void open(){
	openAction.actionPerformed(null);
    }

    /**
     * Returns the value that indicates whether this view was loaded or not.
     *
     * @return
     */
    public boolean isLoaded(){
	return wasResized && wasChildLoaded;
    }

    @Override
    public void close() {
	setSize(new Dimension(0, 0));
	getModel().setMode(ReportMode.NOT_SPECIFIED);
	wasResized = false;
	wasChildLoaded = false;
	super.close();
    }

    /**
     * Override this to provide custom report view.
     *
     * @param configurableView - view to configure.
     * @return
     */
    abstract protected VT createConfigurableView();

    /**
     * Override this to provide custom wizard to configure report.
     *
     * @param wizardView - wizard view to configure
     * @return
     */
    abstract protected WT createWizardView();

    /**
     * Creates the {@link HierarchyListener} that determines when the component was shown and it's size was determined.
     * Also if child component was also loaded then it fires the load event.
     *
     * @return
     */
    private ComponentListener createComponentWasResized() {
	return new ComponentAdapter() {

	    @Override
	    public void componentResized(final ComponentEvent e) {
		synchronized (AbstractConfigurationView.this) {
		    // should "component resized" event be handled?
		    if (!wasResized) {
			// yes, so this one is first, lets handle it and set flag
			// to indicate that we won't handle any more
			// "component resized" event
			wasResized = true;

			//The component was resized so lets see whether child was loaded if that is true then fire
			//event that this component was loaded.
			if(wasChildLoaded){
			    fireLoadEvent(new LoadEvent(AbstractConfigurationView.this));
			}
		    }
		}
	    }

	};
    }

    /**
     * Creates the open action see {@link #openAction} for more details.
     *
     * @return
     */
    private Action createOpenAction() {
	return new BlockingLayerCommand<List<Result>>("Open", getProgressLayer()) {

	    private static final long serialVersionUID = 6165292815580260412L;

	    @Override
	    protected boolean preAction() {
		final boolean superResult = super.preAction();
		if(!superResult){
		    return false;
		}
		for(final Result result : fireOpenEvent(new AbstractConfigurationViewEvent(AbstractConfigurationView.this, AbstractConfigurationViewEventAction.PRE_OPEN))){
		    if(!result.isSuccessful()){
			return false;
		    }
		}
		return true;
	    }

	    @Override
	    protected List<Result> action(final ActionEvent e) throws Exception {
		return fireOpenEvent(new AbstractConfigurationViewEvent(AbstractConfigurationView.this, AbstractConfigurationViewEventAction.OPEN));
	    }

	    @Override
	    protected void postAction(final List<Result> value) {
		super.postAction(value);
		fireOpenEvent(new AbstractConfigurationViewEvent(AbstractConfigurationView.this, AbstractConfigurationViewEventAction.POST_OPEN));
		for(final Result valueRes : value){
		    if(!valueRes.isSuccessful()){
			getModel().setMode(ReportMode.WIZARD);
			return;
		    }
		}
		getModel().setMode(ReportMode.REPORT);
	    }

	    @Override
	    protected void handlePreAndPostActionException(final Throwable ex) {
		super.handlePreAndPostActionException(ex);
		fireOpenEvent(new AbstractConfigurationViewEvent(AbstractConfigurationView.this, AbstractConfigurationViewEventAction.OPEN_FAILED));
	    }
	};
    }

    /**
     * Creates listener that listens mode changed event.
     *
     * @return
     */
    private PropertyChangeListener createModeChangeListener(){
	return new PropertyChangeListener() {

	    @Override
	    public void propertyChange(final PropertyChangeEvent evt) {
		if("mode".equals(evt.getPropertyName())){
		    final ReportMode mode = (ReportMode)evt.getNewValue();
		    switch(mode){
		    case WIZARD:
			previousWizard = createWizardView();
			setView(previousWizard);
			break;
		    case REPORT:
			previousView = createConfigurableView();
			setView(previousView);
			break;
		    case NOT_SPECIFIED:
			setView(null);
			break;
		    }
		}

	    }

	};
    }

    /**
     * Set the current view for this panel: wizard or configurable review.
     *
     * @param component
     */
    private void setView(final SelectableAndLoadBasePanel component){
	removeAll();
	if(component != null){
	    addLoadListenerTo(component);
	    add(component);
	    component.select();
	} else {
	    fireChildNullLoaded();
	}
	invalidate();
	validate();
	repaint();
    }

    /**
     * Fires component load event if this component was resized.
     */
    private synchronized void fireChildNullLoaded(){
	if (!wasChildLoaded) {
	    wasChildLoaded = true;
	    if(wasResized){
		fireLoadEvent(new LoadEvent(AbstractConfigurationView.this));
	    }
	}
    }

    /**
     * Adds the {@link ILoadListener} to the specified component. That load listener determines when the specified component was loaded.
     * Also if this component wasn't loaded yet it fires load event for this {@link AbstractConfigurationView} instance.
     *
     * @param component
     */
    private void addLoadListenerTo(final SelectableAndLoadBasePanel component) {
	component.addLoadListener(new ILoadListener() {

	    @Override
	    public void viewWasLoaded(final LoadEvent event) {
		synchronized (AbstractConfigurationView.this) {
		    // should child load event be handled?
		    if (!wasChildLoaded) {
			// yes, so this one is first, lets handle it and set flag
			// to indicate that we won't handle any more
			// child load events.
			wasChildLoaded = true;

			//The child was loaded so lets see whether this component was resized if that is true then fire
			//event that this was loaded.
			if(wasResized){
			    fireLoadEvent(new LoadEvent(AbstractConfigurationView.this));
			}
			// after this handler end its execution, lets remove it
			// from component because it is already not-useful
			final ILoadListener refToThis = this;
			SwingUtilitiesEx.invokeLater(new Runnable() {
			    public void run() {
				component.removeLoadListener(refToThis);
			    }
			});
		    }
		}
	    }
	});
    }

    /**
     * Fires the specified open event.
     *
     * @param event
     */
    private List<Result> fireOpenEvent(final AbstractConfigurationViewEvent event){
	final List<Result> results = new ArrayList<Result>();
	for(final IAbstractConfigurationViewEventListener listener : listenerList.getListeners(IAbstractConfigurationViewEventListener.class)){
	    results.add(listener.abstractConfigurationViewEventPerformed(event));
	}
	return results;
    }

    /**
     * The action that changes current configuration view's mode to the {@link ReportMode#WIZARD} mode.
     *
     * @author TG Team
     *
     * @param <VT>
     * @param <WT>
     */
    public static abstract class ConfigureAction extends ChangeModeAction{

	private static final long serialVersionUID = 1090639998966452323L;

	/**
	 * Initialises this {@link ConfigureAction} with the specified {@link AbstractConfigurationView}.
	 *
	 * @param configurationView
	 */
	public ConfigureAction(final AbstractConfigurationView<?, ?> configurationView) {
	    super(configurationView, ReportMode.WIZARD, ReportMode.REPORT);
	}
    }

    /**
     * The action that accepts modification and changes current configuration view's mode to the {@link ReportMode#REPORT} mode.
     *
     * @author TG Team
     *
     * @param <VT>
     * @param <WT>
     */
    public static abstract class BuildAction extends ChangeModeAction{

	private static final long serialVersionUID = 1090639998966452323L;

	/**
	 * Initialises this {@link BuildAction} with the specified {@link AbstractConfigurationView}.
	 *
	 * @param configurationView
	 */
	public BuildAction(final AbstractConfigurationView<?, ?> configurationView) {
	    super(configurationView, ReportMode.REPORT, ReportMode.WIZARD);
	}
    }

    /**
     * The action that discards modification and changes current configuration view's mode to the {@link ReportMode#REPORT} mode.
     *
     * @author TG Team
     *
     * @param <VT>
     * @param <WT>
     */
    public static abstract class CancelAction extends ChangeModeAction{

	private static final long serialVersionUID = 1090639998966452323L;

	/**
	 * Initialises this {@link CancelAction} with the specified {@link AbstractConfigurationView}.
	 *
	 * @param configurationView
	 */
	public CancelAction(final AbstractConfigurationView<?, ?> configurationView) {
	    super(configurationView, ReportMode.REPORT, ReportMode.WIZARD);
	}
    }

    /**
     * The {@link BlockingLayerCommand} action that changes the report mode to the specified one.
     *
     * @author TG Team
     *
     * @param <VT>
     * @param <WT>
     */
    private static abstract class ChangeModeAction extends BlockingLayerCommand<Result>{

	private static final long serialVersionUID = 1090639998966452323L;

	private final AbstractConfigurationView<?, ?> configurationView;
	private final ReportMode reportMode;
	//The report mode that is used when during the action processing the exception wsa thrown.
	private final ReportMode restorationMode;

	/**
	 * Initialises this {@link ChangeModeAction} {@link AbstractConfigurationView} instance and specified report mode to which configuration view must be changed.
	 *
	 * @param configurationView
	 * @param reportMode
	 */
	public ChangeModeAction(final AbstractConfigurationView<?, ?> configurationView, final ReportMode reportMode, final ReportMode restorationMode) {
	    super("", configurationView.getProgressLayer());
	    this.configurationView = configurationView;
	    this.reportMode = reportMode;
	    this.restorationMode = restorationMode;
	}

	/**
	 * Returns the {@link AbstractConfigurationView} instance associated with this action.
	 *
	 * @return
	 */
	public AbstractConfigurationView<?, ?> getConfigurationView() {
	    return configurationView;
	}

	@Override
	protected boolean preAction() {
	    if(!super.preAction()){
		return false;
	    }
	    final Result result = getConfigurationView().getModel().canSetMode(reportMode);
	    if(!result.isSuccessful()){
		JOptionPane.showMessageDialog(getConfigurationView(), result.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
		return false;
	    }
	    return true;
	}

	@Override
	protected void postAction(final Result value) {
	    getConfigurationView().getModel().setMode(reportMode);
	    super.postAction(value);
	}

	/**
	 * Restore after exception
	 */
	abstract protected void restoreAfterError();

	@Override
	protected final void handlePreAndPostActionException(final Throwable ex) {
	    new BlockingLayerCommand<Void>("", getConfigurationView().getProgressLayer()) {

		private static final long serialVersionUID = 6591522199014576781L;

		@Override
		protected Void action(final ActionEvent e) throws Exception {
		    restoreAfterError();
		    return null;
		}

		@Override
		protected void postAction(final Void value) {
		    super.postAction(value);
		    getConfigurationView().getModel().setMode(restorationMode);
		}
	    }.actionPerformed(null);
	    super.handlePreAndPostActionException(ex);
	}
    }
}
