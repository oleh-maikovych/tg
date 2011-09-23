package ua.com.fielden.platform.criteria.generator;

import ua.com.fielden.platform.dao.IEntityDao;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.swing.review.EntityQueryCriteria;
import ua.com.fielden.platform.treemodel.rules.criteria.ICriteriaDomainTreeManager;

/**
 * A contract for criteria generator.
 * 
 * @author TG Team
 *
 */
public interface ICriteriaGenerator {

    /**
     * Generates and configures {@link EntityQueryCriteria} instance.
     * 
     * @param <T>
     * @param root
     * @param cdtm
     * @return
     */
    public <T extends AbstractEntity> EntityQueryCriteria<T, IEntityDao<T>> generateQueryCriteria(Class<T> root, ICriteriaDomainTreeManager cdtm);
}
