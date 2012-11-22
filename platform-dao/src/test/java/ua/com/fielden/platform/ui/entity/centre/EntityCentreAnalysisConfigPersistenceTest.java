package ua.com.fielden.platform.ui.entity.centre;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.security.provider.UserController;
import ua.com.fielden.platform.security.user.IUserDao;
import ua.com.fielden.platform.security.user.User;
import ua.com.fielden.platform.test.AbstractDomainDrivenTestCase;
import ua.com.fielden.platform.test.PlatformTestDomainTypes;
import ua.com.fielden.platform.ui.config.EntityCentreAnalysisConfig;
import ua.com.fielden.platform.ui.config.EntityCentreAnalysisConfigDao;
import ua.com.fielden.platform.ui.config.EntityCentreConfig;
import ua.com.fielden.platform.ui.config.IEntityCentreAnalysisConfig;
import ua.com.fielden.platform.ui.config.MainMenuItem;
import ua.com.fielden.platform.ui.config.api.IEntityCentreConfigController;
import ua.com.fielden.platform.ui.config.api.IMainMenuItemController;
import ua.com.fielden.platform.ui.config.controller.EntityCentreConfigControllerDao;
import ua.com.fielden.platform.ui.config.controller.MainMenuItemControllerDao;
import ua.com.fielden.platform.utils.EntityUtils;

/**
 * This test case ensures correct persistence and retrieval of entities with properties of type byte[].
 *
 * @author TG Team
 *
 */
public class EntityCentreAnalysisConfigPersistenceTest extends AbstractDomainDrivenTestCase {
    private final IEntityCentreConfigController daoECC = getInstance(EntityCentreConfigControllerDao.class);
    private final IEntityCentreAnalysisConfig dao = getInstance(EntityCentreAnalysisConfigDao.class);
    private final IMainMenuItemController menuDao = getInstance(MainMenuItemControllerDao.class);
    private final IUserDao userDao = getInstance(UserController.class);

    @Test
    public void test_insertion_and_retrieval_of_data() {
	final EntityCentreConfig config = new_composite(EntityCentreConfig.class, userDao.findByKey("USER"), "CONFIG 1", menuDao.findByKey("type"));
	config.setConfigBody(new byte[]{1,2,3});
	final EntityCentreConfig config2 = daoECC.save(config);

	final EntityCentreAnalysisConfig analysis = new_composite(EntityCentreAnalysisConfig.class, config2, "ANALYSIS 1");
	dao.save(analysis);

	final List<EntityCentreAnalysisConfig> result = dao.getPage(0, 25).data();
	assertEquals("Incorrect number of retrieved configurations.", 1, result.size());
	assertTrue("Incorrectly saved binary property.", EntityUtils.equalsEx("ANALYSIS 1", result.get(0).getTitle()));
    }

    @Override
    protected void populateDomain() {
	save(new_(User.class, "USER", "DESC").setBase(true).setPassword("PASSWD"));
	save(new_(MainMenuItem.class, "type", "desc").setOrder(1));
    }

    @Override
    protected List<Class<? extends AbstractEntity<?>>> domainEntityTypes() {
	return PlatformTestDomainTypes.entityTypes;
    }
}