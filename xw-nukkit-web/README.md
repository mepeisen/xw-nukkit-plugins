# xw-nukkit-web
Embedded webserver for nukkit. Internal name: "xw-web".

# current version
release: NONE
snapshot: 0.0.1-SNAPSHOT

# maven identification
group-id: eu.xworlds.nukkit
artifact-id: xw-nukkit-web

# use/download
See the tutorials at [www.xworlds.eu/dev/nukkit](http://www.xworlds.eu/dev/nukkit) on how to install the nexus repository.
You may download the latest version from there.

# description
This plugin enabled an embedded webserver within nukkit. Simply add the jar to the plugins folder or follow the tutorial to create a maven based server.

After installing and running the first time you must edit the config. For the security reasons the maintenance mode is enabled by default. Simply set this configuration option to false. After restarting nukkit navigate to the web page (f.e. "http://localhost:19133"). You will see a simple welcome page.

The web requests are built by the following scheme:

    http://<ip>:<port>/<plugin-name>/<page-name>

See the information page at http://localhost:19133/xw-web/info.json
It will give you some json output of general server information.

# built your own web-extension
Add a maven dependency to this plugin. Ensure that it is not contained in your resulting jar file.
Within your plugin main class in method "onEnable" add the following code:

    ((WebserverPlugin)getServer().getPluginManager().getPlugin("xw-web")).registerFactory(this, new MyHandlerFactory());

Within method "onDisable" add the following code:

    ((WebserverPlugin)getServer().getPluginManager().getPlugin("xw-web")).unregisterFactory(this);

See the sources of [class LocalFactory](src/main/java/eu/xworlds/nukkit/web/LocalFactory.java) for a sample of your own factory.