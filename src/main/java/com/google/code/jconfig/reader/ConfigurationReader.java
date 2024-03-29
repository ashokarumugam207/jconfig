/*
 * @(#)ConfigurationReader.java          Data: 22/gen/2011
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

package com.google.code.jconfig.reader;


import java.io.File;
import java.util.Stack;

import javax.xml.parsers.*;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.code.jconfig.exception.ConfigurationParsingException;
import com.google.code.jconfig.exception.PluginInstantiationException;
import com.google.code.jconfig.factory.ConfigurationPluginFactory;
import com.google.code.jconfig.factory.ConfigurationReaderFactory;
import com.google.code.jconfig.model.ConfigurationInfo;
import com.google.code.jconfig.reader.hierarchical.HierarchicalReader;
import com.google.code.jconfig.reader.hierarchical.IHierarchicalReader;
import com.google.code.jconfig.reader.plugins.IConfigurationPlugin;

/**
 * <p>
 *   Default implementation of {@link IConfigurationReader} using a SAX parser.
 * <p>
 *
 * @author: Gabriele Fedeli (gabriele.fedeli@gmail.com)
 */
public class ConfigurationReader implements IConfigurationReader {

	private StringBuilder currentConfigPath;
	private SAXParser parser;
	private ConfigurationReaderHandler readerHandler;
	private ConfigurationInfo configurationInfo;
	
	private static final Logger logger = Logger.getLogger(ConfigurationReader.class);
	
	private static enum ELEMENT_TAGS {
		CONFIGURATIONS,
			IMPORT,
			CONFIGURATION,
	}
	
	private static enum ATTRIBUTES {
		plugin, file
	}
	
	/**
	 * 
	 * @throws ConfigurationParsingException
	 */
	public ConfigurationReader() throws ConfigurationParsingException {
		logger.debug("Initializing configuration reader");
		try {
			currentConfigPath = new StringBuilder();
			parser = SAXParserFactory.newInstance().newSAXParser();
			readerHandler = new ConfigurationReaderHandler();
		} catch (ParserConfigurationException e) {
			throw new ConfigurationParsingException(e.getMessage());
		} catch (SAXException e) {
			throw new ConfigurationParsingException(e.getMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.code.jconfig.reader.IConfigurationReader#readConfiguration(java.lang.String)
	 */
	public ConfigurationInfo readConfiguration(String absolutePath) throws ConfigurationParsingException {
		try {
			logger.debug("Reading configuration: " + absolutePath);
			configurationInfo = new ConfigurationInfo();
			File configurationFile = new File(absolutePath);
			currentConfigPath.append(configurationFile.getParent())
			                 .append(File.separator);
			
			configurationInfo.addConfigurationFilePath(absolutePath);
			parser.parse(configurationFile, readerHandler);
		} catch (Exception e) {
			
			ConfigurationParsingException configurationException = new ConfigurationParsingException(e.getMessage());
			if(e instanceof ConfigurationParsingException) {
				configurationInfo.getConfFileList().addAll( ((ConfigurationParsingException)e).getFileParsedList() ); 
			}
			
			configurationException.getFileParsedList().addAll(configurationInfo.getConfFileList());
			configurationInfo.clear();
			configurationInfo = null;

			throw configurationException;
		}
		
		return configurationInfo;
	}
	
	/**
	 * <p>
	 *   The internal handler for SAX parser.
	 * </p>
	 */
	private class ConfigurationReaderHandler extends DefaultHandler {
		
		private IConfigurationPlugin<?> currentPlugin;
		private Stack<IHierarchicalReader> configurationPluginStack;
		
		@Override
		public void startDocument() throws SAXException {
			configurationPluginStack = new Stack<IHierarchicalReader>();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			String tagName = qName.toUpperCase();
			
			if(tagName.equals(ELEMENT_TAGS.CONFIGURATIONS.name())) {
				// DO NOTHING
				logger.debug("Found <configurations> tag start.");
			} else if(tagName.equals(ELEMENT_TAGS.IMPORT.name())) {
				logger.debug("Found <import> tag start.");
				String importedConfiguration = attributes.getValue(ATTRIBUTES.file.name());
				StringBuilder absolutePath = new StringBuilder(currentConfigPath);
				absolutePath.append(importedConfiguration);

				configurationInfo.add(ConfigurationReaderFactory.read(absolutePath.toString()));
				
			} else if(tagName.equals(ELEMENT_TAGS.CONFIGURATION.name())) {
				logger.debug("Found <configuration> tag start.");
				String pluginClass = attributes.getValue(ATTRIBUTES.plugin.name());
				try {
					currentPlugin = ConfigurationPluginFactory.getPlugin(pluginClass);
				} catch (PluginInstantiationException e) {
					clearResources();
					throw new ConfigurationParsingException(e.getMessage());
				}
				configurationPluginStack.push(createHierarchicalReader(qName, attributes));
			} else { 
				logger.debug("Found <" + qName + "> tag start.");
				configurationPluginStack.push(createHierarchicalReader(qName, attributes));
			}
		}
		
		@Override
		public void characters(char[] characters, int start, int end) throws SAXException {
			if( !configurationPluginStack.isEmpty() ) {
				String value = new String(characters, start, end);
				((HierarchicalReader)configurationPluginStack.peek()).setValue(value);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String tagName = qName.toUpperCase();
			
			if(tagName.equals(ELEMENT_TAGS.CONFIGURATIONS.name())) {
				// DO NOTHING
				logger.debug("Found <configurations> tag end.");
			} else if(tagName.equals(ELEMENT_TAGS.CONFIGURATION.name())) {
				logger.debug("Found <configuration> tag end.");
				IHierarchicalReader rootConfiguration = configurationPluginStack.pop();
				String idConfiguration = rootConfiguration.getAttributeValue("id");
				Object configuration = currentPlugin.readConfiguration(rootConfiguration);
				configurationInfo.addConfigurationDetail(idConfiguration, configuration);
			} else if(tagName.equals(ELEMENT_TAGS.IMPORT.name())) {
				// DO NOTHING
				logger.debug("Found <import> tag end.");
			} else {
				logger.debug("Found <" + qName + "> tag end.");
				IHierarchicalReader child = configurationPluginStack.pop();
				((HierarchicalReader)configurationPluginStack.peek()).addChild(child);
			}
		}
		
		@Override
		public void endDocument() throws SAXException {
			clearResources();
		}

		private void clearResources() {
			currentConfigPath.delete(0, currentConfigPath.length());
			currentPlugin = null;
			configurationPluginStack.clear();
		}

		private IHierarchicalReader createHierarchicalReader(String nodeName, Attributes attributes) {
			HierarchicalReader hierarchicalReader = new HierarchicalReader();
			hierarchicalReader.setNodeName(nodeName);
			for(int i = 0; i < attributes.getLength(); i++) {
				hierarchicalReader.addAttribute(attributes.getQName(i), attributes.getValue(i));
			}
			
			return hierarchicalReader;
		}
	}
}
