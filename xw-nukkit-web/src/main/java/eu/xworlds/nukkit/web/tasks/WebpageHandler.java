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
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Interface to be implemented by plugins.
 * 
 * Provides a web page handler to handle http requests.
 * 
 * @author mepeisen
 */
public interface WebpageHandler {

	/**
	 * Handles an incoming web request. Content-Length and keep alive handling
	 * is managed by the webserver implementation.
	 * 
	 * @param ctx
	 *            web request context
	 * @param server
	 *            the nukkit server instance
	 * @return the full http response
	 * @see "http://netty.io/4.0/xref/io/netty/example/http/snoop/HttpSnoopServerHandler.html#145"
	 * @throws RuntimeException
	 *             can be thrown to show up an internal error.
	 */
	FullHttpResponse handleRequest(WebRequestContext ctx, Server server);

}
