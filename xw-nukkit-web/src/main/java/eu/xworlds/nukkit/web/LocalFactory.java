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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import eu.xworlds.nukkit.web.tasks.WebRequestContext;
import eu.xworlds.nukkit.web.tasks.WebpageHandler;
import eu.xworlds.nukkit.web.tasks.WebpageHandlerFactory;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.util.CharsetUtil;

/**
 * A local factrory to create the standard pages.
 * 
 * @author mepeisen
 */
class LocalFactory implements WebpageHandlerFactory {

	@Override
	public WebpageHandler requestHandler(WebRequestContext context, Server server, String[] paths) {
		if (paths.length == 3) {
			switch (paths[2]) {
			case "info.json":
				return new InfoHandler();
			}
		}
		return null;
	}

	/**
	 * info.json: Standard nukkit server info
	 */
	public class InfoHandler implements WebpageHandler {

		@Override
		public FullHttpResponse handleRequest(WebRequestContext ctx, Server server) {
			final JsonObject result = new JsonObject();
			
			final JsonObject options = new JsonObject();
			options.addProperty("allowFlight", server.getAllowFlight());
			options.addProperty("difficulty", server.getDifficulty());
			options.addProperty("gamemode", server.getGamemode());
			options.addProperty("language", server.getLanguage().getName());
			options.addProperty("name", server.getName());
			result.add("options", options);
			
			final JsonArray levels = new JsonArray();
			for (final Level level : server.getLevels().values())
			{
				final JsonObject levelinfo = new JsonObject();
				levelinfo.addProperty("id", level.getId());
				levelinfo.addProperty("name", level.getName());
				levelinfo.addProperty("seed", level.getSeed());
				levelinfo.addProperty("players", level.getPlayers().size());
				levels.add(levelinfo);
			}
			result.add("levels", levels);
			
			result.addProperty("defaultLevel", server.getDefaultLevel().getId());
			result.addProperty("maxPlayers", server.getMaxPlayers());
			result.addProperty("onlinePlayers", server.getOnlinePlayers().size());
			
			final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					Unpooled.copiedBuffer(new Gson().toJson(result), CharsetUtil.UTF_8));
			response.headers().set(Names.CONTENT_TYPE, "application/json");
			return response;
		}

	}

}
