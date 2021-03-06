package ua.com.fielden.platform.web.centre;

import com.google.inject.Inject;

import ua.com.fielden.platform.dao.CommonEntityDao;
import ua.com.fielden.platform.entity.annotation.EntityType;
import ua.com.fielden.platform.entity.query.IFilter;

/** 
 * DAO implementation for companion object {@link ISortingProperty}.
 * 
 * @author Developers
 *
 */
@EntityType(SortingProperty.class)
public class SortingPropertyDao extends CommonEntityDao<SortingProperty> implements ISortingProperty {
    @Inject
    public SortingPropertyDao(final IFilter filter) {
        super(filter);
    }

}