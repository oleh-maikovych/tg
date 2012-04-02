package ua.com.fielden.platform.swing.review.development;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import ua.com.fielden.platform.domaintree.centre.ICentreDomainTreeManager.ICentreDomainTreeManagerAndEnhancer;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.swing.actions.BlockingLayerCommand;
import ua.com.fielden.platform.swing.components.blocking.BlockingIndefiniteProgressLayer;
import ua.com.fielden.platform.swing.review.report.events.ReviewEvent;
import ua.com.fielden.platform.swing.review.report.events.ReviewEvent.ReviewAction;
import ua.com.fielden.platform.swing.review.report.interfaces.IReview;
import ua.com.fielden.platform.swing.review.report.interfaces.IReviewEventListener;

public abstract class AbstractEntityReview<T extends AbstractEntity<?>, CDTME extends ICentreDomainTreeManagerAndEnhancer> extends SelectableBasePanel implements IReview {

    private static final long serialVersionUID = -8984113615241551583L;

    private final AbstractEntityReviewModel<T, CDTME> model;

    private final BlockingIndefiniteProgressLayer progressLayer;

    private final Action configureAction/*, saveAction, saveAsAction, saveAsDefaultAction, loadDefaultAction, removeAction*/;

    public AbstractEntityReview(final AbstractEntityReviewModel<T, CDTME> model, final BlockingIndefiniteProgressLayer progressLayer){
	this.model = model;
	this.progressLayer = progressLayer;
	this.configureAction = createConfigureAction();
	//	this.saveAction = createSaveAction();
	//	this.saveAsAction = createSaveAsAction();
	//	this.saveAsDefaultAction = createSaveAsDefaultAction();
	//	this.loadDefaultAction = createLoadDefaultAction();
	//	this.removeAction = createRemoveAction();

    }

    public final Action getConfigureAction(){
	return configureAction;
    }

    //    public final Action getSaveAction(){
    //	return saveAction;
    //    }
    //
    //    public final Action getLoadDefaultAction(){
    //	return loadDefaultAction;
    //    }
    //
    //    public final Action getSaveAsDefaultAction() {
    //	return saveAsDefaultAction;
    //    }
    //
    //    public final Action getSaveAsAction(){
    //	return saveAsAction;
    //    }
    //
    //    public final Action getRemoveAction() {
    //	return removeAction;
    //    }

    public BlockingIndefiniteProgressLayer getProgressLayer() {
	return progressLayer;
    }


    @Override
    public void addReviewEventListener(final IReviewEventListener l) {
	listenerList.add(IReviewEventListener.class, l);
    }

    @Override
    public void removeReviewEventListener(final IReviewEventListener l) {
	listenerList.remove(IReviewEventListener.class, l);
    }

    @Override
    public String getInfo() {
	return "Entity centre";
    }

    /**
     * Returns the {@link AbstractEntityReviewModel} for this entity review.
     *
     * @return
     */
    public AbstractEntityReviewModel<T, CDTME> getModel() {
	return model;
    }

    //    protected Action createRemoveAction() {
    //	return createReviewAction("Delete", "Delete current report", ReviewAction.PRE_REMOVE, ReviewAction.REMOVE, ReviewAction.POST_REMOVE);
    //    }
    //
    //    protected Action createLoadDefaultAction() {
    //	return createReviewAction("Load default", "Loads default locator configuration and updates local configuration", ReviewAction.PRE_LOAD_DEFAULT, ReviewAction.LOAD_DEFAULT, ReviewAction.POST_LOAD_DEFAULT);
    //    }
    //
    //    protected Action createSaveAsDefaultAction() {
    //	return createReviewAction("Save as default", "Saves the locator as default and updates local configuration", ReviewAction.PRE_SAVE_AS_DEFAULT, ReviewAction.SAVE_AS_DEFAULT, ReviewAction.POST_SAVE_AS_DEFAULT);
    //    }
    //
    //    protected Action createSaveAsAction() {
    //	return createReviewAction("Save as", "Save an entity centre copy", ReviewAction.PRE_SAVE_AS, ReviewAction.SAVE_AS, ReviewAction.POST_SAVE_AS);
    //    }
    //
    //    protected Action createSaveAction() {
    //	return createReviewAction("Save", "Saves the entity centre", ReviewAction.PRE_SAVE, ReviewAction.SAVE, ReviewAction.POST_SAVE);
    //    }

    protected Action createConfigureAction(){
	return createReviewAction("Configure", "Configure entity centre", ReviewAction.PRE_CONFIGURE, ReviewAction.CONFIGURE, ReviewAction.POST_CONFIGURE, ReviewAction.CONFIGURE_FAILED);
    }

    protected boolean notifyReviewAction(final ReviewEvent ev) {
	// Process the listeners first to last, notifying
	// those that are interested in this event
	boolean result = true;

	for (final IReviewEventListener listener : getListeners(IReviewEventListener.class)) {
	    result &= listener.configureActionPerformed(ev);
	}
	return result;
    }

    /**
     * Creates on of the review action: configure, save, save as or remove.
     *
     * @param name - the caption for action.
     * @param preAction
     * @param action
     * @param postAction
     * @param actionFailed
     * @return
     */
    private Action createReviewAction(final String name, final String shortDescription, final ReviewAction preAction, final ReviewAction action, final ReviewAction postAction, final ReviewAction actionFailed){
	return new BlockingLayerCommand<Void>(name, progressLayer){

	    private static final long serialVersionUID = 4502256665545168359L;

	    {
		putValue(Action.SHORT_DESCRIPTION, shortDescription);
	    }

	    @Override
	    protected boolean preAction() {
		final boolean result = super.preAction();
		if(!result){
		    return false;
		}
		return notifyReviewAction(new ReviewEvent(AbstractEntityReview.this, preAction));
	    }

	    @Override
	    protected Void action(final ActionEvent e) throws Exception {
		notifyReviewAction(new ReviewEvent(AbstractEntityReview.this, action));
		return null;
	    }

	    @Override
	    protected void postAction(final Void value) {
		super.postAction(value);
		notifyReviewAction(new ReviewEvent(AbstractEntityReview.this, postAction));
	    }

	    @Override
	    protected void handlePreAndPostActionException(final Throwable ex) {
		super.handlePreAndPostActionException(ex);
		notifyReviewAction(new ReviewEvent(AbstractEntityReview.this, actionFailed));
	    }

	};
    }

}
