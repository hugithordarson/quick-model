package quickmodel;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.project.DataNodeDescriptor;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.CayenneRuntime;

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

		final CayenneRuntime runtime = createRuntime();

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
	 * @return A new runtime backed by an in-memory H2 database
	 */
	public static CayenneRuntime createRuntime() {
		final HikariConfig hikariConf = new HikariConfig();
		hikariConf.setDriverClassName( "org.h2.Driver" );
		hikariConf.setJdbcUrl( "jdbc:h2:mem:testerbest" );

		final DataNodeDescriptor dataNode = DataNodeDescriptor
				.of( "testerbest" )
				.dataSource( new HikariDataSource( hikariConf ) )
				.createSchemaIfNeeded()
				.build();

		return CayenneRuntime
				.of( "QuickModel" )
				.addConfig( "cayenne/cayenne-testProject.xml" )
				.defaultDataNode( dataNode )
				.build();
	}
}
