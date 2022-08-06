package quickmodel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickModelGenerator {

	private static final Logger logger = LoggerFactory.getLogger( QuickModelGenerator.class );

	private final Config _config;

	public static class Config {
		public String projectName;
		public String mapName;
		public String destinationDirectory;
		public String javaPackageName;
		public List<Entity> entities;
	}

	public QuickModelGenerator( Config config ) {
		_config = config;
	}

	void run() {
		final DataChannelDescriptor dataChannelDescriptor = new DataChannelDescriptor();
		dataChannelDescriptor.setName( config().projectName );
		final Project project = new Project( new ConfigurationTree<>( dataChannelDescriptor ) );

		// Current implementation generates only one map
		final DataMap map = new DataMap( config().mapName );
		map.setDefaultPackage( config().javaPackageName );
		map.setQuotingSQLIdentifiers( true );
		dataChannelDescriptor.getDataMaps().add( map );

		config().entities.forEach( entity -> {
			logger.info( "Generating entity {}", entity.name() );
			final DbEntity dbEntity = new DbEntity( entity.name() );
			map.addDbEntity( dbEntity );

			final ObjEntity objEntity = new ObjEntity( entity.name() );
			objEntity.setClassName( config().javaPackageName + "." + entity.name() );
			map.addObjEntity( objEntity );
			objEntity.setDbEntity( dbEntity );

			// FIXME: We should make this a UUID type instead
			final DbAttribute pkAttribute = new DbAttribute( "id" );
			dbEntity.addAttribute( pkAttribute );
			pkAttribute.setPrimaryKey( true );
			pkAttribute.setGenerated( true );
			pkAttribute.setType( Types.INTEGER );
			pkAttribute.setMandatory( true );

			entity.attributes().forEach( attribute -> {
				final DbAttribute dbAttribute = new DbAttribute( attribute.name() );
				dbEntity.addAttribute( dbAttribute );
				dbAttribute.setMandatory( false ); // FIXME: Change
				//				dbAttribute.setAttributePrecision( null ); // FIXME: Look into
				dbAttribute.setMaxLength( 100 ); // FIXME: Read from attribute definition
				dbAttribute.setType( Types.VARCHAR ); // FIXME: Read from Java type using some sort of a prototype mechanism

				final ObjAttribute objAttribute = new ObjAttribute( attribute.name() );
				objEntity.addAttribute( objAttribute );
				objAttribute.setDbAttributePath( attribute.name() );
				objAttribute.setType( attribute.clazz().getName() );
			} );
		} );

		saveAndValidate( project );
	}

	private void saveAndValidate( final Project project ) {
		final Injector injector = DIBootstrap.createInjector( List.of( new ServerModule(), new ProjectModule() ) );

		final URL url;

		try {
			url = new File( config().destinationDirectory ).toURI().toURL();
		}
		catch( MalformedURLException e ) {
			throw new IllegalArgumentException( "Unable to construct URL from file: " + config().destinationDirectory, e );
		}

		final Resource resource = new URLResource( url );
		injector.getInstance( ProjectSaver.class ).saveAs( project, resource );

		final ProjectValidator projectValidator = injector.getInstance( ProjectValidator.class );
		final ValidationResult validationResult = projectValidator.validate( project.getRootNode() );

		for( ValidationFailure validationFailure : validationResult.getFailures() ) {
			logger.error( "{} : {}", validationFailure.getDescription(), validationFailure.getSource() );
		}
	}

	private Config config() {
		return _config;
	}
}