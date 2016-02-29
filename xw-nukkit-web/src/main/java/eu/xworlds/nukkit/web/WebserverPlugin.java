/*
    This file is part of "nukkit xWorlds plugin".

    "nukkit xWorlds plugin" is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    "nukkit xWorlds plugin" is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with "nukkit xWorlds plugin". If not, see <http://www.gnu.org/licenses/>.

 */

package eu.xworlds.nukkit.web;

import java.io.File;
import java.util.LinkedHashMap;

import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import eu.xworlds.nukkit.web.tasks.WebRequestContext;
import eu.xworlds.nukkit.web.tasks.WebpageHandler;
import eu.xworlds.nukkit.web.tasks.WebpageHandlerFactory;
import eu.xworlds.nukkit.web.tasks.WebserverTask;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.util.CharsetUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Plugin main class
 * @author mepeisen
 */
public class WebserverPlugin extends PluginBase {

	private WebserverTask webserver;

	@Override
	public void onLoad() {
		this.getLogger().info(TextFormat.WHITE + "xWorlds plugin has been loaded!");
	}

	@Override
	public void onEnable() {
		this.getLogger().info(TextFormat.DARK_GREEN + "xWorlds plugin has been enabled!");
		
		// create data folder on demand
		final File dataFolder = this.getDataFolder();
		if (dataFolder.exists()) {
			this.getLogger().debug("Data folder already exists.");
		}
		else if (dataFolder.mkdirs()) {
			this.getLogger().info(TextFormat.DARK_GREEN + "data folder was created.");
		}
		else {
			this.getLogger().error(TextFormat.RED + "data folder could not be created.");
		}
		
		//Config reading and writing
        final Config config = new Config(
                new File(this.getDataFolder(), "config.yml"),
                Config.YAML,
                //Default values (not necessary)
                new LinkedHashMap<String, Object>() {
                    /**
					 * 
					 */
					private static final long serialVersionUID = -2839943089863075680L;

					{
                        put(WebserverTask.CONFIG_KEY_ENABLED, true);
                        put(WebserverTask.CONFIG_KEY_PORT, 19133);
                        put(WebserverTask.CONFIG_KEY_MAINTENANCE, true);
                    }
                });
        
        if (config.getBoolean(WebserverTask.CONFIG_KEY_ENABLED)) {
        	this.getLogger().info(TextFormat.DARK_GREEN + "web server is enabled on port " + config.getInt(WebserverTask.CONFIG_KEY_PORT));
        }
        
        // save config
        config.save();
        
        // start the webapp server task
        this.webserver = new WebserverTask(this, config);
        this.webserver.registerFactory(this.getName(), new LocalFactory());
        this.getServer().getScheduler().scheduleRepeatingTask(this.webserver, 500);
	}
	
	@Override
	public void onDisable() {
		this.getLogger().info(TextFormat.DARK_GREEN + "Ready to shutdown web server!");
		this.webserver.shutdown();
		this.getLogger().info(TextFormat.DARK_GREEN + "Web server shutdown complete!");
	}
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	this.getLogger().debug("Server command: " + command.getName());
    	switch (command.getName()) {
			case "xwebstatus":
				sender.sendMessage("webserver state: " + this.webserver.getCurrentState()); // TODO NLS
				sender.sendMessage("webserver port (active): " + this.webserver.getCurrentPort()); // TODO NLS
				sender.sendMessage("webserver port (next start): " + this.webserver.getPort()); // TODO NLS
				sender.sendMessage("webserver maintenance: " + String.valueOf(this.webserver.isMaintenance())); // TODO NLS
				break;
			case "xwebstart":
				this.webserver.setStartupIssued(true);
				sender.sendMessage("(async) webserver startup issued"); // TODO NLS
				break;
			case "xwebstop":
				this.webserver.setStartupIssued(true);
				sender.sendMessage("(async) webserver stop issued"); // TODO NLS
				break;
			case "xwebmt":
				if (args != null && args.length == 1) {
					if ("on".equalsIgnoreCase(args[0])) {
						this.webserver.setMaintenance(true);
					}
					else if ("off".equalsIgnoreCase(args[0])) {
						this.webserver.setMaintenance(false);
					} 
					else {
						sender.sendMessage("invalid argument; on/off expected"); // TODO NLS
					}
				}
				else {
					sender.sendMessage("invalid or missing arguments"); // TODO NLS
				}
				break;
    		case "xwebport":
				if (args != null && args.length == 1) {
					try {
						final int port = Integer.parseInt(args[0]);
						if (port < 1 || port > 65535) throw new NumberFormatException();
						this.webserver.setPort(port);
					}
					catch (NumberFormatException ex) {
						sender.sendMessage("invalid argument; number between 1 and 65535 expected"); // TODO NLS
					}
				}
				else {
					sender.sendMessage("invalid or missing arguments"); // TODO NLS
				}
    			break;
//    		case "xhello":
//    			try {
//                    this.getLogger().info(Utils.readFile(new File(this.getDataFolder(), "string.txt")) + " " + sender.getName());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                break;
        }
        return true;
    }
    
    /**
     * Registers a new factory
     * @param plugin the plugin that registers the factory
     * @param factory the webserver factory to be registered
     */
    public void registerFactory(Plugin plugin, WebpageHandlerFactory factory) {
    	this.webserver.registerFactory(plugin.getName(), factory);
    }
    
    /**
     * Unregisters an existing factory
     * @param plugin the plugin that is disabled
     */
    public void unregisterFactory(Plugin plugin) {
    	this.webserver.unregisterFactory(plugin.getName());
    }
    
    /**
     * Builds a http 404 response.
     */
    public static final WebpageHandler _404 = new WebpageHandler(){

		@Override
		public FullHttpResponse handleRequest(WebRequestContext ctx, Server server) {
			return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
		}
	};
    
    /**
     * Builds a http 500 response.
     */
    public static final WebpageHandler _500 = new WebpageHandler(){

		@Override
		public FullHttpResponse handleRequest(WebRequestContext ctx, Server server) {
			return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	};
    
    /**
     * Returns the index page to welcome users.
     */
    public static final WebpageHandler INDEX = new WebpageHandler(){

		@Override
		public FullHttpResponse handleRequest(WebRequestContext ctx, Server server) {
			final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					Unpooled.copiedBuffer("Hello World! This is the wild wild web interface for nukkit server.", CharsetUtil.UTF_8));
			response.headers().set(Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			return response;
		}
	};
    
    /**
     * Returns the maintenance page (http 503).
     */
    public static final WebpageHandler MAINTENANCE = new WebpageHandler(){

		@Override
		public FullHttpResponse handleRequest(WebRequestContext ctx, Server server) {
			return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);
		}
	};

}
