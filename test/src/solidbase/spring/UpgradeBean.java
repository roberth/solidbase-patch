/*--
 * Copyright 2011 Ren� M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import solidbase.core.ConnectionAttributes;
import solidbase.core.Runner;
import solidbase.core.SystemException;
import solidstack.io.InputStreamResource;
import solidstack.io.MemoryResource;
import solidstack.io.URIResource;

/**
 * An upgrade Spring bean.
 *
 * @author Ren� M. de Bloois
 */
public class UpgradeBean
{
	/**
	 * The database driver class. Gets overruled by datasource.
	 */
	private String driver;

	/**
	 * The URL of the database. Gets overruled by datasource.
	 */
	private String url;

	/**
	 * The datasource. Overrules driver and URL.
	 */
	private DataSource datasource;

	/**
	 * The user name to use for connecting to the database.
	 */
	private String username;

	/**
	 * Password for the user.
	 */
	private String password;

	/**
	 * The configured upgrade file.
	 */
	private Resource upgradefile;

	/**
	 * The configured target.
	 */
	private String target;

	/**
	 * The configured downgrade allowed option.
	 */
	protected boolean downgradeallowed;

	/**
	 * The secondary connections.
	 */
	private List< ConnectionAttributes > secondary = new ArrayList< ConnectionAttributes >();

	/**
	 * Returns the database driver class name.
	 *
	 * @return The database driver class name.
	 */
	public String getDriver()
	{
		return this.driver;
	}

	/**
	 * Sets the database driver class name.
	 *
	 * @param driver The database driver class name.
	 */
	public void setDriver( String driver )
	{
		this.driver = driver;
	}

	/**
	 * Returns the database URL.
	 *
	 * @return The database URL.
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * Sets the database URL.
	 *
	 * @param url The database URL.
	 */
	public void setUrl( String url )
	{
		this.url = url;
	}

	/**
	 * Returns the data source.
	 *
	 * @return The data source.
	 */
	public DataSource getDatasource()
	{
		return this.datasource;
	}

	/**
	 * Sets the data source.
	 *
	 * @param datasource The data source.
	 */
	public void setDatasource( DataSource datasource )
	{
		this.datasource = datasource;
	}

	/**
	 * Returns the user name that is used to connect to the database.
	 *
	 * @return The user name.
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Sets the user name to use to connect to the database.
	 *
	 * @param username The user name.
	 */
	public void setUsername( String username )
	{
		this.username = username;
	}

	/**
	 * Returns the password for the user.
	 *
	 * @return The password.
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * Sets the password for the user.
	 *
	 * @param password The password.
	 */
	public void setPassword( String password )
	{
		this.password = password;
	}

	/**
	 * Returns the upgrade file.
	 *
	 * @return The upgrade file.
	 */
	public Resource getUpgradefile()
	{
		return this.upgradefile;
	}

	/**
	 * Sets the upgrade file.
	 *
	 * @param upgradefile The upgrade file.
	 */
	public void setUpgradefile( Resource upgradefile )
	{
		this.upgradefile = upgradefile;
	}

	/**
	 * Returns the target to upgrade the database to.
	 *
	 * @return The target.
	 */
	public String getTarget()
	{
		return this.target;
	}

	/**
	 * Sets the target to upgrade the database to.
	 *
	 * @param target The target.
	 */
	public void setTarget( String target )
	{
		this.target = target;
	}

	/**
	 * Returns the secondary connections.
	 *
	 * @return The secondary connections.
	 */
	public List< ConnectionAttributes > getSecondary()
	{
		return this.secondary;
	}

	/**
	 * Sets the secondary connections.
	 *
	 * @param secondary The secondary connections.
	 */
	public void setSecondary( List< ConnectionAttributes > secondary )
	{
		this.secondary = secondary;
	}

	/**
	 * Validates the configuration of the upgrade bean.
	 */
	protected void validate()
	{
		if( this.datasource == null )
		{
			Assert.hasText( this.driver, "Missing 'datasource' or 'driver' for " + getClass().getName() );
			Assert.hasText( this.url, "Missing 'datasource' or 'url' for " + getClass().getName() );
			Assert.notNull( this.username, "Missing 'username' for " + getClass().getName() );
			Assert.notNull( this.password, "Missing 'password' for " + getClass().getName() );
		}

		for( ConnectionAttributes connection : this.secondary )
			if( connection.getDatasource() == null )
			{
				Assert.hasText( connection.getName(), "Missing 'name' for " + connection.getClass().getName() );
				Assert.isTrue( !connection.getName().equals( "default" ), "The connection name 'default' is reserved" );
				Assert.notNull( connection.getUsername(), "Missing 'username' for " + connection.getClass().getName() );
				Assert.notNull( connection.getPassword(), "Missing 'password' for " + connection.getClass().getName() );
			}
	}

	/**
	 * Upgrades the database.
	 */
	public void upgrade()
	{
		validate();

		ProgressLogger progress = new ProgressLogger();

		Runner runner = new Runner();
		runner.setProgressListener( progress );

		if( this.datasource != null )
			runner.setConnectionAttributes( "default", this.datasource, this.username, this.password );
		else
			runner.setConnectionAttributes( "default", this.driver, this.url, this.username, this.password );

		for( ConnectionAttributes secondary : this.secondary )
			if( secondary.getDatasource() != null )
				runner.setConnectionAttributes( secondary.getName(), secondary.getDatasource(), secondary.getUsername(), secondary.getPassword() );
			else
				runner.setConnectionAttributes( secondary.getName(), secondary.getDriver(), secondary.getUrl(), secondary.getUsername(), secondary.getPassword() );

		try
		{
			if( this.upgradefile instanceof ByteArrayResource )
				runner.setUpgradeFile( new MemoryResource( ( (ByteArrayResource)this.upgradefile ).getByteArray() ) );
			else if( this.upgradefile.isOpen() )
				// Spring resource isOpen means that the resource cannot be reopened. Thats why we get the inputstream.
				runner.setUpgradeFile( new InputStreamResource( this.upgradefile.getInputStream() ) );
			else
				runner.setUpgradeFile( new URIResource( this.upgradefile.getURI() ) );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}

		runner.setUpgradeTarget( this.target );
		runner.setDowngradeAllowed( this.downgradeallowed );

		runner.upgrade();
	}
}
