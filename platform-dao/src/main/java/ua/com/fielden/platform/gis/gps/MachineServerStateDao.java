package ua.com.fielden.platform.gis.gps;

import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.fetchAll;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.from;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;
import ua.com.fielden.platform.entity.query.fluent.fetch;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;

import java.util.Map;

import ua.com.fielden.platform.pagination.IPage;
import ua.com.fielden.platform.dao.CommonEntityDao;
import ua.com.fielden.platform.swing.review.annotations.EntityType;
import ua.com.fielden.platform.entity.query.IFilter;
import ua.com.fielden.platform.gis.gps.IMachineServerState;
import ua.com.fielden.platform.gis.gps.MachineServerState;
import ua.com.fielden.platform.gis.gps.mixin.MachineServerStateMixin;
import ua.com.fielden.platform.dao.annotations.SessionRequired;

import com.google.inject.Inject;

/** 
 * DAO implementation for companion object {@link IMachineServerState}.
 * 
 * @author Developers
 *
 */
@EntityType(MachineServerState.class)
public class MachineServerStateDao extends CommonEntityDao<MachineServerState> implements IMachineServerState {
    
    private final MachineServerStateMixin mixin;
    
    @Inject
    public MachineServerStateDao(final IFilter filter) {
        super(filter);
        
        mixin = new MachineServerStateMixin(this);
    }
    
}