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
package eu.xworlds.nukkit.web.tasks;

import cn.nukkit.Server;

/**
 * A factory to create webpage request handlers on demand.
 * 
 * @author mepeisen
 */
public interface WebpageHandlerFactory {
	
	/**
	 * Requests a new handler to server the given request.
	 * @param context web request context.
	 * @param server the nukkit server
	 * @param paths the file path, splitted by slash
	 * @return the web handler or {@code null} to send a HTTP 404 (not found)
	 */
	WebpageHandler requestHandler(WebRequestContext context, Server server, String[] paths);

}
