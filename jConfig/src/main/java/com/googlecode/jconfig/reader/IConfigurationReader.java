/*
 * @(#)IConfigurationReader.java          Data: 22/gen/2011
 *
 *
 * Copyright 2011 Gabriele Fedeli
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
 *
 */

package com.googlecode.jconfig.reader;


import java.io.InputStream;
import java.util.Map;

import com.googlecode.jconfig.ConfigurationException;
import com.googlecode.jconfig.model.IConfiguration;

/**
 * <p>
 *   Implemented by classes able to parse and load a configuration file.
 * <p>
 *
 * @author: Gabriele Fedeli (gabriele.fedeli@gmail.com)
 */
public interface IConfigurationReader {

	/**
	 * <p>
	 *   Parse a configuration file and returns a map of
	 *   {@link IConfiguration}.
	 * </p>
	 * 
	 * @param inputStream the configuration file stream
	 * @return a map of {@link IConfiguration}
	 * @throws ConfigurationException
	 */
	public Map<String, IConfiguration> readConfiguration(InputStream inputStream) throws ConfigurationException;
}