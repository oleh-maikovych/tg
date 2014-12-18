package ua.com.fielden.platform.web.interfaces;

import ua.com.fielden.platform.web.component.AbstractWebComponent;

/**
 * The web layout manager contract.
 *
 * @author TG Team
 *
 * @param <T>
 */
public interface ILayout<T> extends IRenderable{

    /**
     * Adds the {@link AbstractWebComponent} to the layout manager.
     *
     * @param component - a component to be added to layout manager.
     * @return
     */
    ILayout<T> add(AbstractWebComponent component);

    /**
     * Adds the {@link AbstractWebComponent} to the layout manager.
     *
     * @param component - a component to be added to layout manager
     * @param constraints - additional layout configuration parameters for the component.
     * @return
     */
    ILayout<T> add(AbstractWebComponent component, T constraints);
}