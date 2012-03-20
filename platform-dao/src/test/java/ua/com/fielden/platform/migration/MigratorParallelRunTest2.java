package ua.com.fielden.platform.migration;

import java.sql.Connection;
import java.util.List;

import ua.com.fielden.platform.migration.dao.MigrationHistoryDao2;
import ua.com.fielden.platform.sample.domain.controller.ITgVehicleModel2;
import ua.com.fielden.platform.test.DbDrivenTestCase2;


/**
 * Test the basic data migration logic.
 *
 * @author TG team
 *
 */
public class MigratorParallelRunTest2 extends DbDrivenTestCase2 {
    private Connection conn = injector.getInstance(Connection.class);


    @Override
    public void setUp() throws Exception {
        super.setUp();
	conn.createStatement().execute("DROP ALL OBJECTS;");
	conn.createStatement().execute("RUNSCRIPT FROM 'src/test/resources/db/legacy-db.ddl'");
    }

    @Override
    public void tearDown() throws Exception {
	conn.close();
        super.tearDown();
    }

    public void test_migrating_of_entity_with_another_entity_association() throws Exception {
	hibernateUtil.getSessionFactory().getCurrentSession().close();

	final DataMigrator2 dm = new DataMigrator2(injector, hibernateUtil, entityFactory, "test", 2, tMakeRetriever2.class, tModelRetriever2.class);

	dm.populateData();
	hibernateUtil.getSessionFactory().getCurrentSession().close();

	assertEquals("Incorrect number of migrated entities.", 14, injector.getInstance(ITgVehicleModel2.class).getPage(0, 100).data().size());
	final List<MigrationHistory> migrationHistories = injector.getInstance(MigrationHistoryDao2.class).getPage(0, 100).data();
	assertEquals("Incorrect number of retrieved items for first thread", 0, 7 - migrationHistories.get(1).getRetrievedCount());
	assertEquals("Incorrect number of inserted items for first thread", 0, 7 - migrationHistories.get(1).getInsertedCount());
	assertEquals("Incorrect number of retrieved items for second thread", 0, 7 - migrationHistories.get(2).getRetrievedCount());
	assertEquals("Incorrect number of inserted items for second thread", 0, 7 - migrationHistories.get(2).getInsertedCount());
    }

    @Override
    protected String[] getDataSetPathsForInsert() {
	return new String[] { "src/test/resources/data-files/empty-data.flat.xml" };
    }
}