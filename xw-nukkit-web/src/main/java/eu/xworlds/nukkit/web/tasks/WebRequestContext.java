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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * The request context of the http request.
 * 
 * @author mepeisen
 */
public interface WebRequestContext {
	
	/**
	 * Returns the http headers coming along with this request
	 * @return http request headers
	 */
	HttpHeaders getRequestHeaders();
	
	/**
	 * Returns the trailing http headers coming along with this request (see protocol specification)
	 * @return trailing http headers
	 */
	HttpHeaders getRequestTrailingHeaders();
	
	/**
	 * Returns the http protocol version
	 * @return
	 */
	HttpVersion getProtocolVersion();
	
	/**
	 * Returns the http method
	 * @return http method
	 */
	HttpMethod getMethod();
	
	/**
	 * Returns the request uri
	 * @return
	 */
	String getRequestUri();
	
	/**
	 * Returns the query string
	 * @return
	 */
	QueryStringDecoder getQueryString();
	
	/**
	 * returns the request content (maybe form content).
	 * To decode as string use f.e. {@code toString(CharsetUtil.UTF_8)}.
	 * @return request content as byte buffer.
	 */
	ByteBuf getRequestContent();

}
