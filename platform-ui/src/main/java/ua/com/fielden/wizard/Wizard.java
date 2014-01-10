package ua.com.fielden.wizard;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.matcher.IValueMatcherFactory;
import ua.com.fielden.platform.error.Result;
import ua.com.fielden.platform.swing.actions.BlockingLayerCommand;
import ua.com.fielden.platform.swing.actions.Command;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.ei.development.MasterPropertyBinder;
import ua.com.fielden.platform.swing.ei.editors.development.ILightweightPropertyBinder;
import ua.com.fielden.platform.swing.ei.editors.development.IPropertyEditor;
import ua.com.fielden.platform.swing.utils.Dialogs;
import ua.com.fielden.platform.swing.utils.SwingUtilitiesEx;
import ua.com.fielden.platform.swing.view.BasePanel;

public class Wizard<T extends AbstractEntity<?>> extends BasePanel {

    private static final long serialVersionUID = 1L;
    private final String title;
    private final String info;
    private final T model;
    private final ILightweightPropertyBinder<T> propBinder;
    private final Map<String, IPropertyEditor> editors;

    private IWizState<T> currState;
    private IWizState<T> startState;

    private final JPanel holdingPanel = new JPanel(new MigLayout("fill", "[fill, grow]", "[c,grow,fill][fill,c]"));
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pagePanel = new JPanel(cardLayout);
    private final JPanel navPanel = new JPanel(new MigLayout("fill, insets 0", "[:50:]20:push[:50:][:50:]", "[c]"));
    private final BlockingIndefiniteProgressLayer blockingLayer = new BlockingIndefiniteProgressLayer(holdingPanel, "");

    private final Command<IWizState<T>> next;
    private final Command<IWizState<T>> prev;
    private final Command<IWizState<T>> cancel;
    private final JButton nextButton;

    public Wizard(//
	    final String title,
    /*	  */final String info,//
	    final T model, //
	    final IValueMatcherFactory valueMatcherFactory, //
	    final List<IWizState<T>> states) {
	super.setLayout(new MigLayout("fill, insets 0", "[c,fill,grow]", "[c,grow,fill]"));
	super.add(blockingLayer);

	holdingPanel.add(pagePanel, "wrap");
	holdingPanel.add(navPanel);

	this.title = title;
	this.info = info;
	this.model = model;
	this.propBinder = MasterPropertyBinder.<T> createPropertyBinderWithoutLocatorSupport(valueMatcherFactory);
	this.editors = buildEditors(model, propBinder);

	next = createNextCommand();
	prev = createPrevCommand();
	cancel = createCancelCommand();

	navPanel.add(new JButton(cancel));
	navPanel.add(new JButton(prev));
	navPanel.add(nextButton = new JButton(next));

	// initialise all pages by building their UI
	for (final IWizState<T> state : states) {
	    final AbstractWizPage<T> page = state.view();
	    page.buildUi(this);
	    pagePanel.add(page, state.name());
	}
	startState = states.get(0);
	setCurrState(startState);
    }

    protected Command<IWizState<T>> createNextCommand() {
	final Command<IWizState<T>> command = new BlockingLayerCommand<IWizState<T>>("Next", blockingLayer) {

	    @Override
	    protected boolean preAction() {
		setMessage("Validating...");
		final Result result = model.isValid();
		if (!result.isSuccessful()) {
		    if (JOptionPane.NO_OPTION == Dialogs.showYesNoDialog(Wizard.this, "<html><b>There are errors:</b><br/><p/>" + result.getMessage()
			    + "<br/><p/>Would you line to proceed?", "Going to next page warning.")) {
			return false;
		    }
		}
		return super.preAction();
	    }

	    @Override
	    protected IWizState<T> action(final ActionEvent e) throws Exception {
		final IWizState<T> prevState = currState;
		if (currState instanceof IWizStartState) {
		    setMessage("Next...");
		    setCurrState(((IWizStartState<T>) currState).next());
		} else if (currState instanceof IWizTransState) {
		    setMessage("Next...");
		    setCurrState(((IWizTransState<T>) currState).next());
		} else if (currState instanceof IWizFinalState) {
		    setMessage("Finishing...");
		    setCurrState(((IWizFinalState<T>) currState).finish());
		}
		if (currState != null) {
		    currState.setTransitionedFrom(prevState);
		}
		return currState;
	    }

	    @Override
	    protected void postAction(final IWizState<T> value) {
		if (currState != null) {
		    cardLayout.show(pagePanel, currState.name());
		} else {
		    model.restoreToOriginal();
		    propBinder.rebind(editors, model);
		    setCurrState(startState);
		    cardLayout.show(pagePanel, currState.name());
		}
		super.postAction(value);
	    }
	};
	return command;
    }

    protected Command<IWizState<T>> createPrevCommand() {
	final Command<IWizState<T>> command = new BlockingLayerCommand<IWizState<T>>("Prev", blockingLayer) {

	    @Override
	    protected IWizState<T> action(final ActionEvent e) throws Exception {
		if (currState instanceof IWizTransState) {
		    setMessage("Previous...");
		    setCurrState(((IWizTransState<T>) currState).prev());
		} else if (currState instanceof IWizFinalState) {
		    setMessage("Previous...");
		    setCurrState(((IWizFinalState<T>) currState).prev());
		}
		return currState;
	    }

	    @Override
	    protected void postAction(final IWizState<T> state) {
		if (state != null) {
		    cardLayout.show(pagePanel, state.name());
		}
		super.postAction(state);
		setCurrState(state);
		nextButton.requestFocusInWindow();
	    }
	};
	return command;
    }

    protected Command<IWizState<T>> createCancelCommand() {
	final Command<IWizState<T>> command = new BlockingLayerCommand<IWizState<T>>("Cancel", blockingLayer) {

	    @Override
	    protected IWizState<T> action(final ActionEvent e) throws Exception {
		setMessage("Cancelling...");
		if (currState instanceof IWizTransState) {
		    setCurrState(((IWizTransState<T>) currState).cancel());
		} else if (currState instanceof IWizFinalState) {
		    setCurrState(((IWizFinalState<T>) currState).cancel());
		}
		return currState;
	    }

	    @Override
	    protected void postAction(final IWizState<T> state) {
		if (state != null) {
		    model.restoreToOriginal();
		    propBinder.rebind(editors, model);
		    cardLayout.show(pagePanel, state.name());
		}
		super.postAction(state);
		setCurrState(state);
		nextButton.requestFocusInWindow();
	    }
	};
	return command;
    }

    protected void setCurrState(final IWizState<T> state) {
	this.currState = state;

	SwingUtilitiesEx.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		if (currState instanceof IWizStartState) {
		    cancel.setEnabled(false);
		    next.setEnabled(true);
		    prev.setEnabled(false);
		} else if (currState instanceof IWizTransState) {
		    cancel.setEnabled(true);
		    next.setEnabled(true);
		    prev.setEnabled(true);
		} else if (currState instanceof IWizFinalState) {
		    cancel.setEnabled(true);
		    next.setEnabled(true);
		    prev.setEnabled(true);
		}
	    }
	});
    }

    public void lock() {
	blockingLayer.setLocked(true);
    }

    /**
     * Unlocks the panel. Must be invoked on EDT.
     */
    public void unlock() {
	blockingLayer.setLocked(false);
    }

    /**
     * Sets message on the blocking layer, which is displayed while panel is locked.
     *
     * @param msg
     */
    public void setBlockingMessage(final String msg) {
	blockingLayer.setText(msg);
    }

    public Map<String, IPropertyEditor> getEditors() {
	return Collections.unmodifiableMap(editors);
    }

    protected Map<String, IPropertyEditor> buildEditors(final T entity, final ILightweightPropertyBinder<T> propertyBinder) {
	return propertyBinder.bind(entity);
    }

    @Override
    public String getInfo() {
	return info;
    }

    @Override
    public String toString() {
        return title;
    }

}