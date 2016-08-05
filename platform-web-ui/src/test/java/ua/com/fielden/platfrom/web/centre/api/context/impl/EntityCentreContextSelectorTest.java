package ua.com.fielden.platfrom.web.centre.api.context.impl;

import static org.junit.Assert.assertEquals;
import static ua.com.fielden.platform.web.centre.api.context.impl.EntityCentreContextSelector.context;

import java.util.function.Function;

import org.junit.Test;

import ua.com.fielden.platform.entity.AbstractFunctionalEntityWithCentreContext;
import ua.com.fielden.platform.web.centre.api.context.CentreContextConfig;

/**
 * Ensures correct construction of the entity centre context configuration as the result of using Entity Centre DSL.
 *
 * @author TG Team
 *
 */
public class EntityCentreContextSelectorTest {

    @Test
    public void context_with_current_entity() {
       final CentreContextConfig config = context().withCurrentEntity().build();
       assertEquals(new CentreContextConfig(true, false, false, false, null), config);
    }

    @Test
    public void context_with_all_selected_entities() {
       final CentreContextConfig config = context().withSelectedEntities().build();
       assertEquals(new CentreContextConfig(false, true, false, false, null), config);
    }

    @Test
    public void context_with_selection_crit() {
       final CentreContextConfig config = context().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(false, false, true, false, null), config);
    }

    @Test
    public void context_with_master_entity() {
       final CentreContextConfig config = context().withMasterEntity().build();
       assertEquals(new CentreContextConfig(false, false, false, true, null), config);
    }

    @Test
    public void context_with_current_entity_and_selection_crit() {
       final CentreContextConfig config = context().withCurrentEntity().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(true, false, true, false, null), config);
    }

    @Test
    public void context_with_selection_crit_and_current_entity() {
       final CentreContextConfig config = context().withSelectionCrit().withCurrentEntity().build();
       assertEquals(new CentreContextConfig(true, false, true, false, null), config);
    }

    @Test
    public void context_with_current_entity_and_master_entity() {
       final CentreContextConfig config = context().withCurrentEntity().withMasterEntity().build();
       assertEquals(new CentreContextConfig(true, false, false, true, null), config);
    }

    @Test
    public void context_with_master_entity_and_current_entity() {
       final CentreContextConfig config = context().withMasterEntity().withCurrentEntity().build();
       assertEquals(new CentreContextConfig(true, false, false, true, null), config);
    }

    @Test
    public void context_with_selected_entities_and_selection_crit() {
       final CentreContextConfig config = context().withSelectedEntities().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(false, true, true, false, null), config);
    }

    @Test
    public void context_with_selection_crit_and_selected_entities() {
       final CentreContextConfig config = context().withSelectionCrit().withSelectedEntities().build();
       assertEquals(new CentreContextConfig(false, true, true, false, null), config);
    }

    @Test
    public void context_with_selected_entities_and_master_entity() {
       final CentreContextConfig config = context().withSelectedEntities().withMasterEntity().build();
       assertEquals(new CentreContextConfig(false, true, false, true, null), config);
    }

    @Test
    public void context_with_master_entity_and_selected_entities() {
       final CentreContextConfig config = context().withMasterEntity().withSelectedEntities().build();
       assertEquals(new CentreContextConfig(false, true, false, true, null), config);
    }

    @Test
    public void context_with_master_entity_and_selection_crit() {
       final CentreContextConfig config = context().withMasterEntity().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(false, false, true, true, null), config);
    }

    @Test
    public void context_with_selection_crit_and_master_entity() {
       final CentreContextConfig config = context().withSelectionCrit().withMasterEntity().build();
       assertEquals(new CentreContextConfig(false, false, true, true, null), config);
    }

    @Test
    public void context_with_master_entity_and_selection_crit_and_curr_entity() {
       final CentreContextConfig config = context().withMasterEntity().withSelectionCrit().withCurrentEntity().build();
       assertEquals(new CentreContextConfig(true, false, true, true, null), config);
    }

    @Test
    public void context_with_master_entity_and_selection_crit_and_selected_entities() {
       final CentreContextConfig config = context().withMasterEntity().withSelectionCrit().withSelectedEntities().build();
       assertEquals(new CentreContextConfig(false, true, true, true, null), config);
    }

    @Test
    public void context_with_selection_crit_and_master_entity_and_curr_entity() {
       final CentreContextConfig config = context().withSelectionCrit().withMasterEntity().withCurrentEntity().build();
       assertEquals(new CentreContextConfig(true, false, true, true, null), config);
    }

    @Test
    public void context_with_selection_crit_and_master_entity_and_selected_entities() {
       final CentreContextConfig config = context().withSelectionCrit().withMasterEntity().withSelectedEntities().build();
       assertEquals(new CentreContextConfig(false, true, true, true, null), config);
    }

    @Test
    public void context_with_current_entity_and_selection_crit_and_master_entity() {
       final CentreContextConfig config = context().withCurrentEntity().withSelectionCrit().withMasterEntity().build();
       assertEquals(new CentreContextConfig(true, false, true, true, null), config);
    }

    @Test
    public void context_with_current_entity_and_master_entity_and_selection_crit() {
       final CentreContextConfig config = context().withCurrentEntity().withMasterEntity().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(true, false, true, true, null), config);
    }

    @Test
    public void context_with_selected_entities_and_selection_crit_and_master_entity() {
       final CentreContextConfig config = context().withSelectedEntities().withSelectionCrit().withMasterEntity().build();
       assertEquals(new CentreContextConfig(false, true, true, true, null), config);
    }

    @Test
    public void context_with_selected_entities_and_master_entity_and_selection_crit() {
       final CentreContextConfig config = context().withSelectedEntities().withMasterEntity().withSelectionCrit().build();
       assertEquals(new CentreContextConfig(false, true, true, true, null), config);
    }

    @Test
    public void context_with_selected_entities_and_master_entity_and_selection_crit_and_computed_function() {
       final Function<AbstractFunctionalEntityWithCentreContext<?>, Object> computation = entity -> entity.getType();
       final CentreContextConfig config = context().withSelectedEntities().withMasterEntity().withSelectionCrit().withComputation(computation).build();
       assertEquals(new CentreContextConfig(false, true, true, true, computation), config);
    }
}
