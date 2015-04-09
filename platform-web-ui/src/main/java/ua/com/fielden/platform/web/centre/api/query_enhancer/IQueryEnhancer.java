package ua.com.fielden.platform.web.centre.api.query_enhancer;

import java.util.Optional;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.annotation.CritOnly;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.ICompleted;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.IWhere0;
import ua.com.fielden.platform.web.centre.CentreContext;

/**
 *
 * A contract for enhancing an entity centre with custom conditions.
 * It could be conveniently used to make an entity centre context dependent, where the nature of dependency can be expressed by providing additional
 * EQL <code>where</code> clauses.
 * <p>
 * This contract can also be used to specify conditions based on invisible to users {@link CritOnly} parameters.
 *
 * @author TG Team
 *
 * @param <T>
 */
public interface IQueryEnhancer<T extends AbstractEntity<?>> {

    ICompleted<T> enhanceQuery(final IWhere0<T> where, final Optional<CentreContext<T, ?>> context);
}
