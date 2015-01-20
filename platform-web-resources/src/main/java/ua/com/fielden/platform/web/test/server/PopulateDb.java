package ua.com.fielden.platform.web.test.server;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import ua.com.fielden.platform.algorithm.search.ISearchAlgorithm;
import ua.com.fielden.platform.algorithm.search.bfs.BreadthFirstSearch;
import ua.com.fielden.platform.basic.config.IApplicationSettings;
import ua.com.fielden.platform.devdb_support.DomainDrivenDataPopulation;
import ua.com.fielden.platform.devdb_support.SecurityTokenAssociator;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.sample.domain.ITgPerson;
import ua.com.fielden.platform.sample.domain.TgPersistentEntityWithProperties;
import ua.com.fielden.platform.sample.domain.TgPerson;
import ua.com.fielden.platform.security.ISecurityToken;
import ua.com.fielden.platform.security.provider.SecurityTokenNode;
import ua.com.fielden.platform.security.provider.SecurityTokenProvider;
import ua.com.fielden.platform.security.user.IUserDao;
import ua.com.fielden.platform.security.user.SecurityRoleAssociation;
import ua.com.fielden.platform.security.user.User;
import ua.com.fielden.platform.security.user.UserAndRoleAssociation;
import ua.com.fielden.platform.security.user.UserRole;
import ua.com.fielden.platform.test.IDomainDrivenTestCaseConfiguration;
import ua.com.fielden.platform.types.Money;

/**
 * This is a convenience class for (re-)creation of the development database and its population for Web UI Testing Server.
 *
 * It contains the <code>main</code> method and can be executed whenever the target database needs to be (re-)set.
 * <p>
 *
 * <b>IMPORTANT: </b><i>One should be careful not to run this code against the deployment or production databases, which would lead to the loss of all data.</i>
 *
 * <p>
 *
 * @author TG Team
 *
 */
public class PopulateDb extends DomainDrivenDataPopulation {

    private final ApplicationDomain applicationDomainProvider = new ApplicationDomain();

    private PopulateDb(final IDomainDrivenTestCaseConfiguration config) {
        super(config);
    }

    public static void main(final String[] args) throws Exception {
        final String configFileName = args.length == 1 ? args[0] : "src/main/resources/application.properties";
        final FileInputStream in = new FileInputStream(configFileName);
        final Properties props = IDomainDrivenTestCaseConfiguration.hbc;
        props.load(in);
        in.close();

        // override/set some of the Hibernate properties in order to ensure (re-)creation of the target database
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.hbm2ddl.auto", "create");

        final IDomainDrivenTestCaseConfiguration config = new DataPopulationConfig();

        final PopulateDb popDb = new PopulateDb(config);
        popDb.createAndPopulate();

    }

    @Override
    protected void populateDomain() {
        System.out.println("Creating and populating the development database...");
        final ITgPerson aoPerson = (ITgPerson) ao(TgPerson.class);
        aoPerson.populateNew("Super", "User", "Super User", User.system_users.SU.name(), IDomainDrivenTestCaseConfiguration.hbc.getProperty("private-key"));
        aoPerson.populateNew("Demo", "User", "Demo User", "DEMO", IDomainDrivenTestCaseConfiguration.hbc.getProperty("private-key"));

        final UserRole admin = save(new_(UserRole.class, "ADMINISTRATION", "A role, which has a full access to the the system and should be used only for users who need administrative previligies."));

        final User su = ((IUserDao) ao(User.class)).findByKey(User.system_users.SU.name());
        save(new_composite(UserAndRoleAssociation.class, su, admin));

        // populate testing entities
        final TgPersistentEntityWithProperties ent1 = save(new_(TgPersistentEntityWithProperties.class, "KEY1").setIntegerProp(43));
        System.out.println("ent1.getId() == " + ent1.getId());
        final TgPersistentEntityWithProperties ent2 = save(new_(TgPersistentEntityWithProperties.class, "KEY2").setIntegerProp(14));
        System.out.println("ent2.getId() == " + ent2.getId());
        final TgPersistentEntityWithProperties ent3 = save(new_(TgPersistentEntityWithProperties.class, "KEY3").setIntegerProp(15));
        System.out.println("ent3.getId() == " + ent3.getId());
        save(ent2.setEntityProp(ent3));

        final TgPersistentEntityWithProperties moneyEnt1 = save(new_(TgPersistentEntityWithProperties.class, "KEY4").setMoneyProp(new Money(new BigDecimal(23.0))));
        System.out.println("moneyEnt1.getId() == " + moneyEnt1.getId());

        final TgPersistentEntityWithProperties bigDecimalEnt1 = save(new_(TgPersistentEntityWithProperties.class, "KEY5").setBigDecimalProp(new BigDecimal(23.0)));
        System.out.println("bigDecimalEnt1.getId() == " + bigDecimalEnt1.getId());

        final TgPersistentEntityWithProperties stringEnt1 = save(new_(TgPersistentEntityWithProperties.class, "KEY6").setStringProp("ok"));
        System.out.println("stringEnt1.getId() == " + stringEnt1.getId());

        try {
            final IApplicationSettings settings = config.getInstance(IApplicationSettings.class);
            final SecurityTokenProvider provider = new SecurityTokenProvider(settings.pathToSecurityTokens(), settings.securityTokensPackageName()); //  IDomainDrivenTestCaseConfiguration.hbc.getProperty("tokens.path"), IDomainDrivenTestCaseConfiguration.hbc.getProperty("tokens.package")
            final SortedSet<SecurityTokenNode> topNodes = provider.getTopLevelSecurityTokenNodes();
            final SecurityTokenAssociator predicate = new SecurityTokenAssociator(admin, ao(SecurityRoleAssociation.class));
            final ISearchAlgorithm<Class<? extends ISecurityToken>, SecurityTokenNode> alg = new BreadthFirstSearch<Class<? extends ISecurityToken>, SecurityTokenNode>();
            for (final SecurityTokenNode securityNode : topNodes) {
                alg.search(securityNode, predicate);
            }

            System.out.println("Completed database creation and population.");
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected List<Class<? extends AbstractEntity<?>>> domainEntityTypes() {
        return applicationDomainProvider.entityTypes();
    }

}
