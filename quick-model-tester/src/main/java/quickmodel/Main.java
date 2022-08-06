package quickmodel;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.query.ObjectSelect;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import quick.model.Person;
import quickmodel.QuickModelGenerator.Config;

public class Main {

	public static void main( String[] args ) {
		final Config conf = new Config();
		conf.projectName = "testProject";
		conf.mapName = "testMap";
		conf.javaPackageName = "quick.model";
		conf.destinationDirectory = "/Users/hugi/git/quick-model-tester/src/main/resources/cayenne";
		conf.entities = new ArrayList<>();
		conf.entities.add( new Entity( "Person", List.of( new Attribute( "name", String.class ) ) ) );
		conf.entities.add( new Entity( "Division", List.of( new Attribute( "name", String.class ) ) ) );
		new QuickModelGenerator( conf ).run();

		// Give the eclipse compiler a moment to catch up
		try {
			Thread.sleep( 3000 );
		}
		catch( InterruptedException e ) {
			e.printStackTrace();
		}

		final ServerRuntime runtime = createServerRuntime();

		ObjectContext oc = runtime.newContext();

		Person p1 = oc.newObject( Person.class );
		p1.setName( "Hugi Þórðarson" );

		Person p2 = oc.newObject( Person.class );
		p2.setName( "Ósk Gunnlaugsdóttir" );

		oc.commitChanges();

		List<Person> result = ObjectSelect
				.query( Person.class )
				.select( oc );

		for( Person person : result ) {
			System.out.println( person.getName() );
		}
	}

	/**
	 * @return A new serverRuntime initialized with DB connection information loaded from the given properties
	 */
	public static ServerRuntime createServerRuntime() {
		final ServerRuntimeBuilder builder = ServerRuntime.builder( "QuickModel" );
		builder.addConfig( "cayenne/cayenne-testProject.xml" );
		builder.addModule( b -> b.bind( SchemaUpdateStrategy.class ).to( CreateIfNoSchemaStrategy.class ) );
		builder.addModule( b -> b.bind( DataSourceFactory.class ).toInstance( new QuickModelDataSourceFactory() ) );
		builder.addModule( b -> b.bind( DataDomain.class ).toProviderInstance( new QuickModelDataDomainProvider() ) );
		builder.addModule( new ProjectModule() );
		return builder.build();
	}

	public static class QuickModelDataSourceFactory implements DataSourceFactory {

		@Override
		public DataSource getDataSource( DataNodeDescriptor nodeDescriptor ) throws Exception {
			final HikariConfig hikariConf = new HikariConfig();
			hikariConf.setDriverClassName( "org.h2.Driver" );
			hikariConf.setJdbcUrl( "jdbc:h2:mem:testerbest" );
			return new HikariDataSource( hikariConf );
		}
	}

	public static class QuickModelDataDomainProvider extends DataDomainProvider {

		@Override
		protected DataChannelDescriptor loadDescriptor() {
			final DataChannelDescriptor dataChannelDescriptor = super.loadDescriptor();

			final DataNodeDescriptor dataNode = new DataNodeDescriptor( "testerbest" );
			dataChannelDescriptor.getNodeDescriptors().add( dataNode );
			dataNode.getDataMapNames().add( "testMap" );
			dataNode.setSchemaUpdateStrategyType( CreateIfNoSchemaStrategy.class.getName() );

			return dataChannelDescriptor;
		}
	}
}