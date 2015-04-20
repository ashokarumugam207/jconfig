# Overview #

While working on a project I was asked to develop a configuration manager for that project that had to be highly customizable, need no app restart on changes.
So I started working on that requirementd and this project is the natural result of that.

The main area that the project affects could be listed in:
  * configuration file
  * java beans holding configuration data
  * custom configuration plugin reader
  * custom configuration change listener

We will see each of them in detail.

# Configuration File #

Configurations are xml based files with no dtd linked to.
Each configuration files must have a starting tag named **configurations** that can contains one or more **configuration** tag and/or one or more **import** tag.
Configuration contains two attributes:
  * id - a unique identifier of that particular configuration
  * plugin - a full qualified class name of a plugin responsible to read that particular xml fragment

Import contains one attribute:
  * resource - the path of the configuration file to be imported. It's path is relative to the current configuration path

```
<configurations>
	<import resource="" />
	<import resource="" />

	<configuration id="" plugin="" >
		...
	</configuration>
	<configuration id="" plugin="" >
		...
	</configuration>
</configurations>
```

Ok so what we have to write inside the configuration tag? Well it's up to you to decide what to write !!!
For example we have to write a configuration for a list of servers.
We can achive this in different manner, all depending on how you may be more comfortable with.

We can have a configuration like this:

```
<configurations>
	<configuration id="server_list" plugin="" >
		<servers host="www.server1.com" port="80" />
		<servers host="www.server2.com" port="80" />
	</configuration>
</configurations>
```

Or like this:

```
<configurations>
	<configuration id="server_list" plugin="" >
		<server>
			<host>www.server1.com</server>
			<port>80</port>
		</server>
		<server>
			<host>www.server2.com</server>
			<port>80</port>
		</server>
	</configuration>
</configurations>
```

Or even this:

```
<configurations>
	<configuration id="server_list" plugin="" >
		<servers>www.server1.com:80,www.server2.com:80</servers>
	</configuration>
</configurations>
```

Of course you need to implement a plugin class that can manage your data. We'll see that later.

About the import tag, let's see an example.
Imagine that you have the main configuration file under c:\configuration and the conf file we want import under the folder inner (c:\configuration\inner).
Our import tag we'll look like this:

```
<!-- c:\configuration\my_main_conf.xml -->
<configurations>
	<import resource="inner\my_inner_conf.xml" />

	<configuration id="" plugin="" >
		...
	</configuration>
</configurations>

<!-- c:\configuration\inner\my_inner_conf.xml -->
<configurations>
	<configuration id="" plugin="" >
		...
	</configuration>
</configurations>
```

# Custom Configuration Plugin Reader #

Ok so we have our configuration file. Now we need to code a little, creating a custom plugin able to read and process our xml fragment.
Let's assume that we have this configuration:

```
<configurations>
	<configuration id="server_list" plugin="" >
		<servers host="www.server1.com" port="80" />
		<servers host="www.server2.com" port="80" />
	</configuration>
</configurations>
```

First of all we need to decide how we would represent this configuration in the java world.
You could create a java bean containing the list of servers or choose to use a java.util.List directly or aother structures.

Second we have to code the plugin that will read that xml configuration fragment and will return our configuration.
The plugin must implements the _com.google.code.jconfig.reader.plugins.IConfigurationPlugin_ interface.

```
// the bean model representing a single server
public class ServerBean {
	private String host;
	private Integer port;
	
	public ServerBean(String host, Integer port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public Integer getPort() {
		return port;
	}
}

// the plugin class
public class ServerConfigurationPlugin implements IConfigurationPlugin<List<ServerBean>> {

	public List<ServerBean> readConfiguration(IHierarchicalReader reader) {
		
		IHierarchicalReader serversNode = reader.getChildren().get(0);
		List<ServerBean> servers = new ArrayList<ServerBean>();
		for (IHierarchicalReader child : serversNode.getChildren()) {
			servers.add(new ServerBean(child.getAttributeValue("name"), Integer.parseInt(child.getAttributeValue("port"))));
		}
		
		return servers;
	}
}
```

As you can see the interfase makes use of generics thus giving you the ability to work directly with your own data.
In the plugin class you don't manage xml directly, instead you will face with a **IHierarchicalReader** object, that can be seen has a tree model of your xml fragment.
Remember that the IHierarchicalReader contains only the xml fragment of a particular configuration nothing more and nothing less.

If you won't use a java.util.List to contains your data and instead you have craeted a bean to contain the configuration you could have something like this:

```
// the bean model representing a single server
public class ServerBean {
	private String host;
	private Integer port;
	
	// getter/setter and constructor like the example above
}

// the bean container
public class ServerConfiguration {
	private List<ServerBean> servers;
	
	// getter/setter
}

// the plugin class
public class ServerConfigurationPlugin implements IConfigurationPlugin<ServerConfiguration> {

	public ServerConfiguration readConfiguration(IHierarchicalReader reader) {
		
		IHierarchicalReader serversNode = reader.getChildren().get(0);
		List<ServerBean> servers = new ArrayList<ServerBean>();
		for (IHierarchicalReader child : serversNode.getChildren()) {
			servers.add(new ServerBean(child.getAttributeValue("name"), Integer.parseInt(child.getAttributeValue("port"))));
		}
		
		ServerConfiguration myConf = new ServerConfiguration();
		myConf.setServers(servers);
		return myConf;
	}
}
```

Quite simple no?

After having coded the plugin we have to link together the xml fragment and this plugin:

```
<configurations>
	<configuration id="server_list" plugin="the_full_package.ServerConfigurationPlugin" >
		<servers host="www.server1.com" port="80" />
		<servers host="www.server2.com" port="80" />
	</configuration>
</configurations>
```


---

**2.3.0 new feature:**
From release 2.3.0 you have now the capability to turn IHierarchicalReader node back to xml string and then use your favorite library to deserialize it to your custom beans withouth any further effort.
In the following sample I've used the Xstream library:
```

// teh confinguration
<configurations>
	<configuration id="server_list" plugin="" >
		<servers>
			<server name="server 1" port="1000"/>
			<server name="server 2" port="1010"/>
			<server name="server 3" port="1020"/>
		</servers>
	</configuration>
</configurations>

// the plugin class
public class ServerConfigurationPlugin implements IConfigurationPlugin<List<ServerBean>> {

	public List<ServerBean> readConfiguration(IHierarchicalReader reader) {
		
		// so I get only the <servers> tag and its children
		IHierarchicalReader rootNode = reader.getChildren().get(0);
		List<ServerBean> config = null;
		try {
			String xml = NodeTransformer.newInstance().doTransformation(rootNode);
			XStream xstream = new XStream();
			xstream.alias("servers", new ArrayList<ServerBean>().getClass());
			xstream.alias("server", ServerBean.class);
			xstream.aliasAttribute("name", "host");
			xstream.aliasAttribute("port", "port");
			xstream.useAttributeFor(ServerBean.class, "host");
			xstream.useAttributeFor(ServerBean.class, "port");
			config = (List<ServerBean>) xstream.fromXML(xml);
		} catch (NodeTransformationException e) {
			e.printStackTrace();
			config = null;
		}
		
		return config;
	}
}
```


# Custom Configuration Change Listener #

What to do if want to be notified upon configuration changes?
We need to create a custom configuration change listener that will be invoked when the configuration is loaded for the first time and then every time the configuration change.

```
// a simple listener that will print to system out the actual situation
private class ServerConfigurationChangeListener implements IConfigurationChangeListener {

		public <T> void loadConfiguration(T configuration) {
			List<ServerBean> conf = (List<ServerBean>)configuration;
			for (ServerBean server : conf) {
				System.out.println(server);
			}
		}
	}
}
```

A more realistic example could be as follow:

```
public class ServerConfigurationContainer implements IConfigurationChangeListener {

	private List<ServerBean> configuration;
	// a simple and brutal lock avoiding concurrency between different thread consumers of this container properties
	private AtomicBoolean suspend = new AtomicBoolean(true);
	private static ServerConfigurationContainer instance;
	
	private ServerConfigurationContainer() { }
	
	public static synchronized ServerConfigurationContainer getInstance() {
		if(instance == null) {
			instance = new ServerConfigurationContainer();
		}
		
		return instance;
	}
	
	public List<ServerBean> getServers() {
		while(suspend.get());
		return configuration;
	}
	
	// more useful methods
	
	@Override
	public <T> void loadConfiguration(T configuration) {
		while(suspend.compareAndSet(false, true));

		List<ServerBean> newConfiguration = (List<ServerBean>)configuration;
		this.configuration = newConfiguration;
		suspend.compareAndSet(true, false);
	}
}
```

Of course if you used the `ServerConfiguration` class in the above example you need to sustitute all the occurence of `List<ServerBean>` with `ServerConfiguration`
**Note that you have to implements all the necessary logics to prevents concurrency.**

Now we have to do a last work. We need to instructs the library to use this listener:

```
// here I register the listener and load/watch my configuration
String configPath = "absolute config path";
Map<String, IConfigurationChangeListener> listeners = new HashMap<String, IConfigurationChangeListener>();
listeners.put("server_list", new ServerConfigurationContainer());
ConfigurationManager.configureAndWatch(listeners, configPath, 200L);
```

Usually this piece of code will be done at application startup.


---


jConfig provide a default plugin **PropertyConfigurationPlugin** that can be used if you want a configuration map like.
This plugin will return a **BasicConfiguration** object. On that object you could perform this action:
  * getProperty(String key) - String      : Returns the value associated to key or null if the key doesn't exist or there's no property for this configuration
  * getProperties() - Map<String, String> : Returns all the properties of this configuration

And here an example of a configuration:

```
<configurations>
	<configuration id="my_props" plugin="com.google.code.jconfig.reader.plugins.PropertyConfigurationPlugin" >
		<property key="props_1" value="value 1" />
		<property key="props_2" value="value 2" />
		<property key="props_3" value="value 3" />
		<property key="props_4" value="value 4" />
	</configuration>
</configurations>
```