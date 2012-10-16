/**
 * Copyright 2010 Grid Dynamics Consulting Services, Inc.
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
package org.gridkit.coherence.integration.spring.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.gridkit.coherence.integration.spring.BackingMapLookupStrategy;
import org.gridkit.coherence.integration.spring.ClusteredCacheDefinition;
import org.gridkit.coherence.integration.spring.cache.InvalidationStrategy;
import org.gridkit.coherence.integration.spring.cache.LocalCacheDefinition;
import org.gridkit.coherence.integration.spring.cache.NearCacheDecorator;
import org.gridkit.coherence.integration.spring.cache.ReadWriteBackingMapDefinition;
import org.gridkit.coherence.integration.spring.impl.ByNameBackingMapLookupStrategy;
import org.gridkit.coherence.integration.spring.impl.CacheServiceBean;
import org.gridkit.coherence.integration.spring.impl.ClusteredCacheServiceBean;
import org.gridkit.coherence.integration.spring.impl.ClusteredServiceBean;
import org.gridkit.coherence.integration.spring.service.DistributedCacheServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.InvocationServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.LeaseGranularity;
import org.gridkit.coherence.integration.spring.service.MemberListenerCollection;
import org.gridkit.coherence.integration.spring.service.OptimisticCacheServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.PartitionListenerCollection;
import org.gridkit.coherence.integration.spring.service.ProxyServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.RemoteCacheServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.RemoteInvocationServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.ReplicatedCacheServiceConfiguration;
import org.gridkit.coherence.integration.spring.service.ServiceListenerCollection;
import org.gridkit.coherence.integration.spring.service.SocketAddressConfig;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tangosol.net.cache.BinaryMemoryCalculator;

/**
 *	@author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CoherenceConfigNamespaceHandler extends NamespaceHandlerSupport {
	
	private static final String TAG_COHERENCE_CONFIG = "coherence-config";
	private static final String TAG_NAMED_CACHE = "named-cache";
	private static final String TAG_SERVICE_INSTANCE = "service-instance";
	private static final String TAG_DISTRIBUTED_SERVICE_SCHEME = "distributed-service-scheme";
	private static final String TAG_REPLICATED_SERVICE_SCHEME = "replicated-service-scheme";
	private static final String TAG_OPTIMISTIC_SERVICE_SCHEME = "optimistic-service-scheme";
	private static final String TAG_INVOCATION_SERVICE_SCHEME = "invocation-service-scheme";
	private static final String TAG_NAMED_CACHE_SCHEME = "named-cache-scheme";
	private static final String TAG_LOCAL_CACHE_SCHEME = "local-cache-scheme";
	private static final String TAG_NEAR_CACHE_SCHEME = "near-cache-scheme";
	private static final String TAG_READ_WRITE_BACKING_MAP_SCHEME = "read-write-backing-map-scheme";
	private static final String TAG_PROXY_SERVICE_SCHEME = "proxy-service-scheme";
	private static final String TAG_REMOTE_INVOCATION_SERVICE_SCHEME = "remote-invocation-service-scheme";
	private static final String TAG_REMOTE_CACHE_SCHEME = "remote-cache-service-scheme";
	
	private static final String TAG_SOCKET_ADDRESS = "socket-address";

	private static final String BEAN_DFEAULT_BACKING_MAP_LOOKUP_STRATEGY = "default-backing-map-lookup-strategy";

	@Override
	public void init() {
		
		registerBeanDefinitionParser(TAG_COHERENCE_CONFIG, new ConfigParser());
		
		registerBeanDefinitionParser(TAG_NAMED_CACHE, new NamedCacheBeanParser());
		registerBeanDefinitionParser(TAG_SERVICE_INSTANCE, new ServiceInstanceBeanParser());
		
		{// services
			
			// distributed
			{
				ServiceBeanTemplate distribtedService = new ServiceBeanTemplate();
				distribtedService.className = ClusteredCacheServiceBean.class.getName();
				registerServiceBeanProperties(distribtedService);		
				registerDistributedServiceConfigProperties(distribtedService.configTemplate);
				
				registerBeanDefinitionParser(TAG_DISTRIBUTED_SERVICE_SCHEME, distribtedService);
			}
			// replicated
			{
				ServiceBeanTemplate replicatedService = new ServiceBeanTemplate();
				replicatedService.className = ClusteredCacheServiceBean.class.getName();
				registerServiceBeanProperties(replicatedService);
				registerReplicatedServiceConfigProperties(replicatedService.configTemplate);
				
				registerBeanDefinitionParser(TAG_REPLICATED_SERVICE_SCHEME, replicatedService);
			}
			// optimistic
			{
				ServiceBeanTemplate optimisticService = new ServiceBeanTemplate();
				optimisticService.className = ClusteredCacheServiceBean.class.getName();
				registerServiceBeanProperties(optimisticService);
				registerOptimisticServiceConfigProperties(optimisticService.configTemplate);
				
				registerBeanDefinitionParser(TAG_OPTIMISTIC_SERVICE_SCHEME, optimisticService);
			}
			// invocation
			{
				ServiceBeanTemplate invocationService = new ServiceBeanTemplate();
				invocationService.className = ClusteredServiceBean.class.getName();
				registerServiceBeanProperties(invocationService);
				registerInvocationServiceConfigProperties(invocationService.configTemplate);
				
				registerBeanDefinitionParser(TAG_INVOCATION_SERVICE_SCHEME, invocationService);
			}
			// proxy (for Coherence*Extend)
			{
				ServiceBeanTemplate proxyService = new ServiceBeanTemplate();
				proxyService.className = ClusteredServiceBean.class.getName();
				registerServiceBeanProperties(proxyService);
				registerProxyServiceConfigProperties(proxyService.configTemplate);
				
				registerBeanDefinitionParser(TAG_PROXY_SERVICE_SCHEME, proxyService);
			}
			// remote cache (for Coherence*Extend) - although its a cache, it is still closer to the service in its behavior
			{
				ServiceBeanTemplate remoteCacheService = new ServiceBeanTemplate();
				remoteCacheService.className = CacheServiceBean.class.getName();
				registerServiceBeanProperties(remoteCacheService);
				registerRemoteCacheServiceConfigProperties(remoteCacheService.configTemplate);
				
				registerBeanDefinitionParser(TAG_REMOTE_CACHE_SCHEME, remoteCacheService);
			}
			// remote invocation (for Coherence*Extend)
			{
				ServiceBeanTemplate remoteInvocationService = new ServiceBeanTemplate();
				remoteInvocationService.className = ClusteredServiceBean.class.getName();
				registerServiceBeanProperties(remoteInvocationService);
				registerRemoteInvocationServiceConfigProperties(remoteInvocationService.configTemplate);
				
				registerBeanDefinitionParser(TAG_REMOTE_INVOCATION_SERVICE_SCHEME, remoteInvocationService);
			}
		}
		{// cache schemes
			{
				CustomBeanDefinitionTemplate cacheScheme = new CustomBeanDefinitionTemplate();
				registerCacheSchemeProperties(cacheScheme);
				registerBeanDefinitionParser(TAG_NAMED_CACHE_SCHEME, cacheScheme);
			}
			{
				CustomBeanDefinitionTemplate nearCache = new CustomBeanDefinitionTemplate();
				registerNearCacheSchemeProperties(nearCache);
				registerBeanDefinitionParser(TAG_NEAR_CACHE_SCHEME, nearCache);
			}
			{
				CustomBeanDefinitionTemplate localCache = new CustomBeanDefinitionTemplate();
				registerLocalCacheSchemeProperties(localCache);
				registerBeanDefinitionParser(TAG_LOCAL_CACHE_SCHEME, localCache);
			}
			{
				CustomBeanDefinitionTemplate readWriteMap = new CustomBeanDefinitionTemplate();
				registerReadWriteBackingMapSchemeProperties(readWriteMap);
				registerBeanDefinitionParser(TAG_READ_WRITE_BACKING_MAP_SCHEME, readWriteMap);
			}
		}
		{// configuration fragments
			{
				CustomBeanDefinitionTemplate socketConfig = new CustomBeanDefinitionTemplate();
				
				socketConfig.className = SocketAddressConfig.class.getName();
				socketConfig.addProperty("address",	"host", new StringPropertyParser());
				socketConfig.addProperty("port", 	"port", new StringPropertyParser());
				
				registerBeanDefinitionParser(TAG_SOCKET_ADDRESS, socketConfig);
			}
		}
	}

	private void registerServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		template.addProperty("serializer", "serializer", new SerializerBeanParser());
	}
	
	private void registerServiceBeanProperties(CustomBeanDefinitionTemplate template) {
		template.addProperty("service-name", "serviceName", new StringPropertyParser());
		template.addProperty("autostart", "autostart", new StringPropertyParser());		
		template.addProperty("member-listener", "memberListener", new ListenerCollectionParser(MemberListenerCollection.class.getName()));
		template.addProperty("service-listener", "serviceListener", new ListenerCollectionParser(ServiceListenerCollection.class.getName()));
		
		if (template.className.equals(ClusteredCacheServiceBean.class.getName())) {
			template.addDefault("backingMapLookupStrategy", new RuntimeBeanReference(BEAN_DFEAULT_BACKING_MAP_LOOKUP_STRATEGY));
		}
	}
	
	private void registerDistributedServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerServiceConfigProperties(template);
		
		template.className = DistributedCacheServiceConfiguration.class.getName();
		
		template.addProperty("partion-count", 		"partitionCount", new StringPropertyParser());
		template.addProperty("key-associator", 		"keyAssociator", new BeanPropertyParser());
		template.addProperty("key-partitioning", 	"keyPartitioning", new BeanPropertyParser());
		template.addProperty("partition-listener", 	"partitionListener", new ListenerCollectionParser(PartitionListenerCollection.class.getName()));
		template.addProperty("backup-count", 		"backupCount", new StringPropertyParser());
		template.addProperty("backup-count-after-writebehind", "backup-count-after-writebehind", new StringPropertyParser());
		template.addProperty("thread-count", 		"threadCount", new StringPropertyParser());
		template.addProperty("lease-granularity", 	"leaseGranularity", new EnumPropertyParser(LeaseGranularity.values()));
		template.addProperty("transfer-threshold", 	"transferThreshold", new SizePropertyParser());
		template.addProperty("local-storage", 		"localStorage", new StringPropertyParser());
		template.addProperty("task-hung-threshlod", "taskHungThreshpld", new TimeoutPropertyParser());
		template.addProperty("task-timeout", 		"taskTimeout", new TimeoutPropertyParser());
		template.addProperty("request-timeout", 	"requestTimeout", new TimeoutPropertyParser());
	}
	
	private void registerReplicatedServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerServiceConfigProperties(template);

		template.className = ReplicatedCacheServiceConfiguration.class.getName();
		
		template.addProperty("standard-lease-milliseconds", 	"standardLeaseMilliseconds", new StringPropertyParser());
		template.addProperty("lease-granularity", 	"leaseGranularity", new EnumPropertyParser(LeaseGranularity.values()));
		template.addProperty("mobile-issues", 		"mobileIssues", new StringPropertyParser());		
	}

	private void registerOptimisticServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerServiceConfigProperties(template);
		template.className = OptimisticCacheServiceConfiguration.class.getName();
	}
	
	private void registerInvocationServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerServiceConfigProperties(template);
	
		template.className = InvocationServiceConfiguration.class.getName();

		template.addProperty("thread-count", 		"threadCount", new StringPropertyParser());
		template.addProperty("task-hung-threshlod", "taskHungThreshpld", new TimeoutPropertyParser());
		template.addProperty("task-timeout", 		"taskTimeout", new TimeoutPropertyParser());
		template.addProperty("request-timeout", 	"requestTimeout", new TimeoutPropertyParser());
	}
	
	private void registerProxyServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		template.className = ProxyServiceConfiguration.class.getName();
		
		template.addProperty("task-hung-threshlod", "taskHungThresholdMillis", new TimeoutPropertyParser());
		template.addProperty("task-timeout", 		"taskTimeoutMillis", new TimeoutPropertyParser());
		template.addProperty("request-timeout", 	"requestTimeout", new TimeoutPropertyParser());
		template.addProperty("thread-count", 		"threadCount", new StringPropertyParser());
		
		template.addProperty("acceptor-config/connection-limit","connectionLimit", new StringPropertyParser());
		template.addProperty("acceptor-config/serializer", 		"acceptorSerializer", new SerializerBeanParser());
		template.addProperty("acceptor-config/filter", 			"acceptorConnectionFilter", new BeanPropertyParser());
		
		template.addProperty("acceptor-config/outgoing-message-handler/heartbeat-interval",	"acceptorHeartbeatInterval", new TimeoutPropertyParser());
		template.addProperty("acceptor-config/outgoing-message-handler/heartbeat-timeout",	"acceptorHeartbeatTimeout", new TimeoutPropertyParser());
		template.addProperty("acceptor-config/outgoing-message-handler/request-timeout",	"acceptorRequestTimeout", new TimeoutPropertyParser());
		
		template.addProperty("acceptor-config/tcp-acceptor/local-address/address",	"acceptorLocalHost", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/local-address/port",		"acceptorLocalPort", new StringPropertyParser());
		
		template.addProperty("acceptor-config/tcp-acceptor/address-provider",		"addressProvider", new BeanPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/socket-provider",		"acceptorSocketProviderConfig", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/reuse-address",			"acceptorReuseAddress", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/keep-alive-enabled",		"acceptorKeepAliveEnabled", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/tcp-delay-enabled",		"acceptorTcpDelayEnabled", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/receive-buffer-size",	"acceptorReceiveBufferSizeBytes", new SizePropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/send-buffer-size",		"acceptorSendBufferSizeBytes", new SizePropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/listen-backlog",			"acceptorListenBacklog", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/linger-timeout",			"acceptorLingerTimeoutMillis", new TimeoutPropertyParser());
		
		template.addProperty("acceptor-config/tcp-acceptor/authorized-hosts/host-address",	"authorizedHostAddress", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/authorized-hosts/host-range",	"authorizedHostRange", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/authorized-hosts/host-filter",	"authorizedHostFilter", new BeanPropertyParser());
		
		template.addProperty("acceptor-config/tcp-acceptor/suspect-protocol-enabled",	"acceptorSuspectProtocolEnabled", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/suspect-buffer-size",		"acceptorSuspectBufferSizeBytes", new SizePropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/suspect-buffer-length",		"acceptorSuspectBufferLength", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/nominal-buffer-size",		"acceptorNominalBufferSizeBytes", new SizePropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/nominal-buffer-length",		"acceptorNominalBufferLength", new StringPropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/limit-buffer-size",			"acceptorLimitBufferSizeBytes", new SizePropertyParser());
		template.addProperty("acceptor-config/tcp-acceptor/limit-buffer-length",		"acceptorLimitBufferLength", new StringPropertyParser());
		
		template.addProperty("proxy-config/cache-service-proxy/enabled",				"cacheProxyEnabled", new StringPropertyParser());
		template.addProperty("proxy-config/cache-service-proxy/lock-enabled",			"cacheProxyLockEnabled", new StringPropertyParser());
		template.addProperty("proxy-config/cache-service-proxy/read-only",				"cacheProxyReadOnly", new StringPropertyParser());
		template.addProperty("proxy-config/cache-service-proxy/class-name",				"cacheProxyClassName", new StringPropertyParser());
		template.addProperty("proxy-config/cache-service-proxy/cache-lookup-strategy",	"cacheLookupStrategy", new BeanPropertyParser());
		
		template.addProperty("proxy-config/invocation-service-proxy/enabled",	"invocationProxyEnabled", new StringPropertyParser());
		template.addProperty("proxy-config/invocation-service-proxy/class-name","invocationProxyClassName", new StringPropertyParser());
	}
	
	private void registerRemoteServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		template.addProperty("initiator-config/outgoing-message-handler/heartbeat-interval", "initiatorHeartbeatInterval", new TimeoutPropertyParser());
		template.addProperty("initiator-config/outgoing-message-handler/heartbeat-timeout", "initiatorHeartbeatTimeout", new TimeoutPropertyParser());
		template.addProperty("initiator-config/outgoing-message-handler/request-timeout", "initiatorRequestTimeout", new TimeoutPropertyParser());
		
		template.addProperty("initiator-config/serializer", "initiatorSerializer", new SerializerBeanParser());
		template.addProperty("initiator-config/filter", 	"initiatorConnectionFilter", new BeanPropertyParser());
		
		template.addProperty("initiator-config/tcp-initiator/local-address/address",	"initiatorLocalHost", new StringPropertyParser());
		template.addProperty("initiator-config/tcp-initiator/local-address/port",		"initiatorLocalPort", new StringPropertyParser());
		
		template.addProperty("initiator-config/tcp-initiator/remote-addresses", "remoteAddresses", new CollectionPropertyParser(TAG_SOCKET_ADDRESS));
		
		template.addProperty("initiator-config/tcp-initiator/remote-addresses/address-provider",	"addressProvider", new BeanPropertyParser());
		
		template.addProperty("initiator-config/tcp-initiator/socket-provider",		"initiatorSocketProviderConfig", new StringPropertyParser());
		
		template.addProperty("initiator-config/tcp-initiator/reuse-address",		"initiatorReuseAddress", new StringPropertyParser());
		template.addProperty("initiator-config/tcp-initiator/keep-alive-enabled",	"initiatorKeepAliveEnabled", new StringPropertyParser());
		template.addProperty("initiator-config/tcp-initiator/tcp-delay-enabled",	"initiatorTcpDelayEnabled", new StringPropertyParser());
		template.addProperty("initiator-config/tcp-initiator/receive-buffer-size",	"initiatorReceiveBufferSize", new SizePropertyParser());
		template.addProperty("initiator-config/tcp-initiator/send-buffer-size",		"initiatorSendBufferSize", new SizePropertyParser());
		template.addProperty("initiator-config/tcp-initiator/linger-timeout",		"initiatorLingerTimeout", new TimeoutPropertyParser());
		template.addProperty("initiator-config/tcp-initiator/connect-timeout",		"initiatorConnectTimeout", new TimeoutPropertyParser());
	}
	
	private void registerRemoteCacheServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerRemoteServiceConfigProperties(template);
		template.className = RemoteCacheServiceConfiguration.class.getName();
	}
	
	private void registerRemoteInvocationServiceConfigProperties(CustomBeanDefinitionTemplate template) {
		registerRemoteServiceConfigProperties(template);
		template.className = RemoteInvocationServiceConfiguration.class.getName();
	}
	
	private void registerCacheSchemeProperties(CustomBeanDefinitionTemplate template) {
		template.className = ClusteredCacheDefinition.class.getName();
		
		template.addProperty("front-tier", "frontTier", new FrontTierBeanParser());
		template.addProperty("back-tier", "backTier", new BackTierBeanParser());
		template.addProperty("service", "service", new ServiceBeanParser());
	}
	
	private void registerNearCacheSchemeProperties(CustomBeanDefinitionTemplate template) {
		template.className = NearCacheDecorator.class.getName();

		template.addProperty("front-scheme", "frontMap", new CacheMapBeanParser());
		template.addProperty("invalidation-strategy", "invalidationStrategy", new EnumPropertyParser(InvalidationStrategy.values()));
		
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClassName(LocalCacheDefinition.class.getName());
		bd.setLazyInit(true);
		
		template.addDefault("frontMap", bd);
	}
	
	private void registerLocalCacheSchemeProperties(CustomBeanDefinitionTemplate template) {
		template.className = LocalCacheDefinition.class.getName();
		
		template.addProperty("eviction-policy",	"---", new EvictionPolicyPropertySetterParser());
		template.addProperty("high-units", 		"highUnits", new SizePropertyParser());
		template.addProperty("low-units", 		"lowUnits", new SizePropertyParser());
		template.addProperty("unit-calculator", "unitCalculator", new UnitCalculatorBeanParser("unitFactor"));
		template.addProperty("expiry-delay", 	"expiryDelayMillis", new TimeoutPropertyParser());
		template.addProperty("flush-delay", 	"flushDelayMillis", new TimeoutPropertyParser());
	}

	private void registerReadWriteBackingMapSchemeProperties(CustomBeanDefinitionTemplate template) {
		template.className = ReadWriteBackingMapDefinition.class.getName();
		
		template.addProperty("cachestore",				"cachestore", new BeanPropertyParser());
		template.addProperty("cachestore-timeout", 		"cachestoreTimeout", new TimeoutPropertyParser());
		template.addProperty("internal-cache-scheme", 	"internalMap", new CacheMapBeanParser());
		template.addProperty("miss-cache-scheme", 		"missMap", new CacheMapBeanParser());
		template.addProperty("write-delay", 			"writeDelay", new TimeoutPropertyParser());
		template.addProperty("write-batch-factor", 		"write-batch-factor", new StringPropertyParser());
		template.addProperty("write-requeue-threshold", "write-requeue-threshold", new StringPropertyParser());
		template.addProperty("refresh-ahead-factor", 	"refresh-ahead-factor", new StringPropertyParser());
		
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClassName(LocalCacheDefinition.class.getName());
		bd.setLazyInit(true);
		
		template.addDefault("internalMap", bd);		
	}
	
	private void initGlobalBeanDeclaration(ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEAN_DFEAULT_BACKING_MAP_LOOKUP_STRATEGY)) {
			
			GenericBeanDefinition bd = new GenericBeanDefinition();
			bd.setBeanClassName(ByNameBackingMapLookupStrategy.class.getName());
			
			parserContext.getRegistry().registerBeanDefinition(BEAN_DFEAULT_BACKING_MAP_LOOKUP_STRATEGY, bd);
			parserContext.getRegistry().registerAlias(BEAN_DFEAULT_BACKING_MAP_LOOKUP_STRATEGY, BackingMapLookupStrategy.class.getName());
		}
	}
	
	private class ConfigParser implements BeanDefinitionParser {

		@Override
		public BeanDefinition parse(Element root, ParserContext parserContext) {
			
			NodeList nl = root.getChildNodes();
			for(int i = 0; i != nl.getLength(); ++i) {
				Node n = nl.item(i);
				if (n instanceof Element) {
					Element element = (Element) n;
					if ("bean".equals(parserContext.getDelegate().getLocalName(element))) {
						parserContext.getDelegate().parseBeanDefinitionElement(element);
					}
					else {
						parserContext.getDelegate().parseCustomElement(element);
					}
				}
			}			
			return null;
		}		
	}
	
	private class CustomBeanDefinitionTemplate extends AbstractBeanDefinitionParser {
		
		public String className;
		public String factoryBean;
		public String factoryMethod;
				
		private Map<String, PropertyInfo> props = new HashMap<String, PropertyInfo>();
		private Map<String, BeanDefinition> defaultBeans = new HashMap<String, BeanDefinition>();
		private Map<String, Object> defaultValues = new HashMap<String, Object>();

		public void addProperty(String element, String propName, PropertyParser parser) {
			PropertyInfo info = new PropertyInfo();
			info.path = element;
			info.propName = propName;
			info.parser = parser;
			
			props.put(element, info);
		}

		public void addDefault(String propName, BeanDefinition bd) {
			defaultBeans.put(propName, bd);
		}

		public void addDefault(String propName, Object value) {
			defaultValues.put(propName, value);
		}
		
		@Override
		protected AbstractBeanDefinition parseInternal(Element element,	ParserContext parserContext) {
			
			initGlobalBeanDeclaration(parserContext);
						
			GenericBeanDefinition bd = new GenericBeanDefinition();
			if (className != null) {
				bd.setBeanClassName(className);
			}
			if (factoryBean != null) {
				bd.setFactoryBeanName(factoryBean);
			}
			if (factoryMethod != null) {
				bd.setFactoryMethodName(factoryMethod);
			}
			
			initBeanLazyInit(bd, element, parserContext);
			
			// New way of configuration parsing
			for (String path: props.keySet()) {
				PropertyInfo info = props.get(path);
				Element actualElement = findActualElement(element, path);
				if (actualElement == null) continue; 
				if (path.indexOf('@') > 0) {
					// attribute value
					String attributeName = path.substring(path.indexOf('@') + 1);
					String attributeValue = actualElement.getAttribute(attributeName);
					if (StringUtils.hasText(attributeValue)) {
						AttributeParser parser = (AttributeParser) info.parser;
						parser.initProperty(attributeValue, bd, info.propName, parserContext);
					}
				} else {
					// child element
					ElementParser parser = (ElementParser) info.parser;
					parser.initProperty(actualElement, bd, info.propName, parserContext);
				}
			}
			
			/* 
			// Old way of configuration parsing
			NodeList nl = element.getChildNodes();
			for (int i = 0; i != nl.getLength(); ++i) {
				Node n = nl.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					String prop = parserContext.getDelegate().getLocalName(n);
					PropertyInfo info = props.get(prop);
					if (info == null) {
						reportUnknownTag(element, parserContext, n);
					} else {
						ElementParser parser = (ElementParser) info.parser;
						parser.initProperty((Element)n, bd, info.propName, parserContext);
					}
				}
			}
			*/
			
			for (String prop: defaultBeans.keySet()) {
				if (!bd.getPropertyValues().contains(prop)) {
					GenericBeanDefinition defValue = new GenericBeanDefinition(defaultBeans.get(prop));
					String id = parserContext.getReaderContext().generateBeanName(defValue);
					BeanDefinitionHolder holder = new BeanDefinitionHolder(defValue, id);
					bd.getPropertyValues().add(prop, holder);
				}
			}

			for (String prop: defaultValues.keySet()) {
				if (!bd.getPropertyValues().contains(prop)) {
					bd.getPropertyValues().add(prop, defaultValues.get(prop));
				}
			}
			
			return bd;
		}

		protected void initBeanLazyInit(BeanDefinition bd, Element element, ParserContext parserContext) {
			String lazy = element.getAttribute("lazy-init");
			if ("true".equalsIgnoreCase(lazy.toLowerCase())) {
				bd.setLazyInit(true);
			}
			else if ("false".equalsIgnoreCase(lazy.toLowerCase())) {
				bd.setLazyInit(false);
			}
			else {
				initBeanLazyInitDefault(bd, parserContext);
			}			
		}

		protected void initBeanLazyInitDefault(BeanDefinition bd, ParserContext parserContext) {
			String lazy = parserContext.getDelegate().getDefaults().getLazyInit();
			if ("true".equalsIgnoreCase(lazy.toLowerCase())) {
				bd.setLazyInit(true);
			}
			else if ("false".equalsIgnoreCase(lazy.toLowerCase())) {
				bd.setLazyInit(false);
			}
			else {
				// default
				bd.setLazyInit(true);
			}
		}
		
		private Element findActualElement(Element parent, String path) {
			Element current = parent;
			String p = (path.indexOf('@') > 0) ? path.substring(0, path.indexOf('@')) : path;
			int slash = 0;
			do {
				slash = p.indexOf('/');
				String childName = (slash > 0) ? p.substring(0, slash) : p;
				if (current.getElementsByTagName(childName).getLength() == 1) {
					current = (Element) current.getElementsByTagName(childName).item(0);
				} else {
					// configuration fragment omitted, configuration will use its defaults
					return null;
				}
				p = p.substring(slash + 1);
			} while (slash > 0);
			return current;
		}
		
		/*
		private void reportUnknownTag(Element element, ParserContext parserContext, Node n) {
			parserContext.getReaderContext().fatal(
					"Unknown tag '" + parserContext.getDelegate().getLocalName(n)
							+ "' while parsing <" + parserContext.getDelegate().getLocalName(element) + ">", element);
		}
		*/
		
	}
	
	private class ServiceBeanTemplate extends CustomBeanDefinitionTemplate {
		
		public CustomBeanDefinitionTemplate configTemplate = new CustomBeanDefinitionTemplate();
		
		public ServiceBeanTemplate() {
		}
		
		@Override
		protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
			AbstractBeanDefinition serviceBean = super.parseInternal(element, parserContext);						
			BeanDefinition configBean = configTemplate.parseInternal(element, parserContext);
			
			String id = resolveBeanId(element, serviceBean, parserContext);
			String confId = id + "#CONFIG";
			
			parserContext.getRegistry().registerBeanDefinition(confId, configBean);			
			BeanDefinitionHolder holder = new BeanDefinitionHolder(configBean, confId);
			
			serviceBean.getPropertyValues().add("configuration", holder);
			
			if (serviceBean.isLazyInit()) {
				parserContext.getReaderContext().warning("Coherence service is defined as lazy, this is discouraged. Bean name \"" + id + "\" ", element);
			}
			
			return serviceBean;
		}

		private String resolveBeanId(Element element, BeanDefinition serviceBean, ParserContext parserContext) {
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (!StringUtils.hasText(id)) {
				id = parserContext.getReaderContext().generateBeanName(serviceBean);
			}
			return id;
		}

		@Override
		protected void initBeanLazyInitDefault(BeanDefinition bd, ParserContext parserContext) {
			bd.setLazyInit(false);
		}
	}
	
	private class NamedCacheBeanParser extends AbstractBeanDefinitionParser {
		@Override
		protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
			GenericBeanDefinition bd = new GenericBeanDefinition();
			String cacheName = element.getAttribute("cache-name");
			bd.setFactoryBeanName(cacheName);
			bd.setFactoryMethodName("getCache");
			bd.setLazyInit(true);
			return bd;
		}
	}

	private class ServiceInstanceBeanParser extends AbstractBeanDefinitionParser {
		@Override
		protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
			// TODO lazy init semantic
			GenericBeanDefinition bd = new GenericBeanDefinition();
			String serviceName = element.getAttribute("scheme-name");
			bd.setFactoryBeanName(serviceName);
			bd.setFactoryMethodName("getCoherenceService");
			bd.setLazyInit(true);
			return bd;
		}
	}

	private static class PropertyInfo {
		@SuppressWarnings("unused")
		public String path;
		public String propName;
		public PropertyParser parser; 
	}
	
	private interface PropertyParser { // just a mixin interface to provide consistency
	}

	private interface ElementParser extends PropertyParser {
		public void initProperty(Element xml, GenericBeanDefinition bd, String propName, ParserContext context);
	}

	private interface AttributeParser extends PropertyParser {
		public void initProperty(String value, GenericBeanDefinition bd, String propName, ParserContext context);
	}

	private static class StringPropertyParser implements ElementParser, AttributeParser {

		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			initProperty(xml.getTextContent(), bd, propName, context);
		}
		
		@Override
		public void initProperty(String value, GenericBeanDefinition bd,
				String propName, ParserContext context) {
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, value);
		}
	}

	private static class TimeoutPropertyParser implements ElementParser, AttributeParser {
		
		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			initProperty(xml.getTextContent(), bd, propName, context);
		}
		
		@Override
		public void initProperty(String value, GenericBeanDefinition bd,
				String propName, ParserContext context) {
			String text = value.toLowerCase();
			TimeUnit tu = TimeUnit.MILLISECONDS;
			if (text.endsWith("ms")) {
				text = text.substring(0, text.length() - 2);
				tu = TimeUnit.MILLISECONDS;
			}
			else if (text.endsWith("s")) {
				text = text.substring(0, text.length() - 1);
				tu = TimeUnit.SECONDS;
			}
			else if (text.endsWith("m")) {
				text = text.substring(0, text.length() - 1);
				tu = TimeUnit.MINUTES;
			}
			else if (text.endsWith("h")) {
				text = text.substring(0, text.length() - 1);
			}
			double d = Double.parseDouble(text);
			d *= 1000;
			long timeout = tu.toMillis((long)d) / 1000;
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, timeout);
		}
		
	}

	private static class SizePropertyParser implements ElementParser, AttributeParser {
		
		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			initProperty(xml.getTextContent(), bd, propName, context);
		}
		
		@Override
		public void initProperty(String value, GenericBeanDefinition bd,
				String propName, ParserContext context) {
			String text = value.toLowerCase();
			int multiplier = 1;
			if (text.endsWith("b")) {
				text = text.substring(0, text.length() - 1);
			}
			if (text.endsWith("k")) {
				text = text.substring(0, text.length() - 1);
				multiplier = 1 << 10;
			}
			else if (text.endsWith("m")) {
				multiplier = 1 << 20;
			}
			else if (text.endsWith("g")) {
				multiplier = 1 << 30;
			}
			else if (text.endsWith("t")) {
				multiplier = 1 << 40;
			}
			double d = Double.parseDouble(text) * multiplier;
			long val = (long) d;
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, val);
		}
		
	}
	
	private static class BeanPropertyParser implements ElementParser {
		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd,	String propName, ParserContext context) {
			NodeList nl = xml.getChildNodes();
			for(int i = 0; i != nl.getLength(); ++i) {
				Node n = nl.item(i);
				if (n instanceof Element) {
					initPropertyByElement((Element) n, bd, propName, context);
				}
			}
		}

		protected Object parseBeanReference(Element xml, GenericBeanDefinition bd,String propName, ParserContext context) {
			Object bean = context.getDelegate().parsePropertySubElement(xml, bd);
			return bean;
		}
		
		protected void initPropertyByElement(Element xml, GenericBeanDefinition bd,String propName, ParserContext context) {
			Object bean = parseBeanReference(xml, bd, propName, context);
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, bean);
		}
	}
	
	private static class EnumPropertyParser implements ElementParser {
		
		private Object[] values;
		
		public EnumPropertyParser(Object[] values) {
			this.values = values;
		}
		
		
		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd,	String propName, ParserContext context) {
			NodeList nl = xml.getChildNodes();
			for(int i = 0; i != nl.getLength(); ++i) {
				Node n = nl.item(i);
				if (n instanceof Element) {
					String tag = context.getDelegate().getLocalName(n);
					tag = tag.toLowerCase();
					for(Object v : values) {
						if (v.toString().toLowerCase().equals(tag)) {
							MutablePropertyValues mpv = bd.getPropertyValues();
							mpv.add(propName, v);
							return;
						}
					}
				}
			}
			
			context.getReaderContext().fatal("Cannot parse value for tag '" + context.getDelegate().getLocalName(xml) + "'", xml);
		}
	}
	
	private static class SerializerBeanParser extends BeanPropertyParser {
		// TODO

		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd,	String propName, ParserContext context) {
			super.initProperty(xml, bd, propName, context);
		}
	}

	private static class FrontTierBeanParser extends BeanPropertyParser {
		// TODO
	}

	private static class BackTierBeanParser extends CacheMapBeanParser {
		@Override
		protected void initPropertyByElement(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			Object bean = parseBeanReference(xml, bd, propName, context);
			RuntimeBeanNameReference ref;
			if (bean instanceof RuntimeBeanReference) {
				ref = new RuntimeBeanNameReference(((RuntimeBeanReference)bean).getBeanName());				
			}
			else {
				// holder have to be globally accessible
				BeanDefinitionHolder holder = (BeanDefinitionHolder) bean;	
				BeanDefinition obd = context.getRegistry().containsBeanDefinition(holder.getBeanName()) ? context.getRegistry().getBeanDefinition(holder.getBeanName()) : null;
				if (obd == null) {
					context.getRegistry().registerBeanDefinition(holder.getBeanName(), holder.getBeanDefinition());
				}
				else if (obd != holder.getBeanDefinition()) {
					context.getReaderContext().fatal("Backing map name clush " + holder, xml);
				}
				
				ref = new RuntimeBeanNameReference(((BeanDefinitionHolder)bean).getBeanName());
			}
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, ref);
		}
	}

	private static class ServiceBeanParser extends BeanPropertyParser {

		@Override
		protected void initPropertyByElement(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			if ("local".equals(context.getDelegate().getLocalName(xml))) {
				// ignore
			}
			else {
				super.initPropertyByElement(xml, bd, propName, context);
			}
		}
	}

	private static class CacheMapBeanParser extends BeanPropertyParser {
		
		@Override
		protected Object parseBeanReference(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			if ("java-map".equals(context.getDelegate().getLocalName(xml).toLowerCase())) {
				GenericBeanDefinition map = new GenericBeanDefinition();
				map.setBeanClassName(ConcurrentHashMap.class.getName());
				String mapName = context.getReaderContext().generateBeanName(map);
				BeanDefinitionHolder holder = new BeanDefinitionHolder(map, mapName);
				return holder;
			}
			else {
				return super.parseBeanReference(xml, bd, propName, context);
			}
		}
	}

	private static class EvictionPolicyPropertySetterParser extends BeanPropertyParser {
		
		@Override
		protected void initPropertyByElement(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			String tagName = context.getDelegate().getLocalName(xml);
			if ("java-map".equals(tagName)) {
				GenericBeanDefinition map = new GenericBeanDefinition();
				map.setBeanClassName(ConcurrentHashMap.class.getName());
				String mapName = context.getReaderContext().generateBeanName(map);
				BeanDefinitionHolder holder = new BeanDefinitionHolder(map, mapName);
				MutablePropertyValues mpv = bd.getPropertyValues();
				mpv.add(propName, holder);								
			}
			else {
				super.initPropertyByElement(xml, bd, "evictionPolicy", context);
			}
		}
	}
	
	private static class UnitCalculatorBeanParser extends BeanPropertyParser {
		
		private String factorPropName;

		public UnitCalculatorBeanParser(String factorPropName) {
			this.factorPropName = factorPropName;
		}

		@Override
		protected void initPropertyByElement(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			String tagName = context.getDelegate().getLocalName(xml).toLowerCase();
			if ("binary".equals(tagName)) {
				GenericBeanDefinition calculator = new GenericBeanDefinition();
				calculator.setBeanClassName(BinaryMemoryCalculator.class.getName());
				String calculatorName = context.getReaderContext().generateBeanName(calculator);
				BeanDefinitionHolder holder = new BeanDefinitionHolder(calculator, calculatorName);
				MutablePropertyValues mpv = bd.getPropertyValues();
				mpv.add(propName, holder);																
			}
			else if ("factor".equals(tagName)) {
				int factor = Integer.parseInt(xml.getTextContent());
				// TODO 
				MutablePropertyValues mpv = bd.getPropertyValues();
				mpv.add(factorPropName, factor);																				
			}
			else {
				super.initPropertyByElement(xml, bd, propName, context);
			}
		}
	}
	
	private static class ListenerCollectionParser implements ElementParser {
		
		private String listenerCollectionName;
		
		public ListenerCollectionParser(String listenerCollectionName) {
			this.listenerCollectionName = listenerCollectionName;
		}
		
		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd,	String propName, ParserContext context) {
			if (bd.getPropertyValues().contains(propName)) {
				// already set
				// this is an ugly hack, but I hope it will just work and no one will ever read this line
				return;
			}
			NodeList nl = ((Element)xml.getParentNode()).getElementsByTagName(xml.getTagName());
			if (nl.getLength() == 1) {
				Object bean = context.getDelegate().parsePropertySubElement(xml, bd);
				MutablePropertyValues mpv = bd.getPropertyValues();
				mpv.add(propName, bean);
			}
			else {
				GenericBeanDefinition ll = new GenericBeanDefinition();
				ll.setBeanClassName(listenerCollectionName);
				
				ManagedList<Object> list = new ManagedList<Object>(nl.getLength());
				for(int i = 0; i != nl.getLength(); ++i) {
					list.add(context.getDelegate().parsePropertySubElement(((Element)nl.item(i)), bd));
				}
				ll.getConstructorArgumentValues().addGenericArgumentValue(list);
				String llName = context.getReaderContext().generateBeanName(ll);				
				BeanDefinitionHolder holder = new BeanDefinitionHolder(ll, llName);
				MutablePropertyValues mpv = bd.getPropertyValues();
				mpv.add(propName, holder);				
			}
		}
	}
	
	private static class CollectionPropertyParser implements ElementParser {
		
		private final String elementName;
		
		public CollectionPropertyParser(String elementName) {
			this.elementName = elementName;
		}

		@Override
		public void initProperty(Element xml, GenericBeanDefinition bd, String propName, ParserContext context) {
			NodeList nl = xml.getElementsByTagName(elementName);
			
			GenericBeanDefinition ll = new GenericBeanDefinition();
			ll.setBeanClass(java.util.ArrayList.class);
			
			ManagedList<Object> list = new ManagedList<Object>(nl.getLength());
			for(int i = 0; i != nl.getLength(); ++i) {
				list.add(context.getDelegate().parsePropertySubElement(((Element)nl.item(i)), bd));
			}
			
			ll.getConstructorArgumentValues().addGenericArgumentValue(list);
			String llName = context.getReaderContext().generateBeanName(ll);
			BeanDefinitionHolder holder = new BeanDefinitionHolder(ll, llName);
			
			MutablePropertyValues mpv = bd.getPropertyValues();
			mpv.add(propName, holder);				
		}
		
	}
	
}