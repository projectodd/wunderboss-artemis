/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.messaging.artemis;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS;
import org.apache.activemq.artemis.spi.core.naming.BindingRegistry;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.jms.DestinationUtil;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;
import org.projectodd.wunderboss.messaging.jms.JMSMessagingSkeleton;
import org.slf4j.Logger;

import javax.jms.ConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ArtemisMessaging extends JMSMessagingSkeleton {

    public ArtemisMessaging(String name, Options<CreateOption> options) {
        this.name = name;
        this.options = options;
    }

    @Override
    public synchronized void start() throws Exception {
        if (!started) {
            this.server = new EmbeddedJMS();

            ClassLoader cl = this.getClass().getClassLoader();
            if (cl.getResource("broker.xml") == null) {
                this.server.setConfigResourcePath("default-broker.xml");
            }

            this.server.start();
            this.started = true;

            TransportConfiguration config = new TransportConfiguration(InVMConnectorFactory.class.getName());
            BindingRegistry registry = jmsServerManager().getRegistry();

            registry.bind(JNDI_CF_NAME,
                          ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config));
            registry.bind(JNDI_XA_CF_NAME,
                          ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.XA_CF, config));
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            closeCloseables();
            this.server.stop();
            this.server = null;
            this.started = false;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return started;
    }

    public JMSServerManager jmsServerManager() {
        if (this.started) {
            return this.server.getJMSServerManager();
        }

        return null;
    }

    @Override
    public String name() {
        return this.name;
    }

    protected ConnectionFactory createRemoteConnectionFactory(final Options<CreateContextOption> options) {
        TransportConfiguration config =
                new TransportConfiguration(NettyConnectorFactory.class.getName(),
                        new HashMap() {{
                            put("host", options.getString(CreateContextOption.HOST));
                            put("port", options.getInt(CreateContextOption.PORT));
                        }});
        ActiveMQConnectionFactory cf =
                ActiveMQJMSClient.createConnectionFactoryWithoutHA(options.has(CreateContextOption.XA) ?
                                                                           JMSFactoryType.XA_CF :
                                                                           JMSFactoryType.CF,
                                                                   config);

        cf.setReconnectAttempts(options.getInt(CreateContextOption.RECONNECT_ATTEMPTS));
        cf.setRetryInterval(options.getLong(CreateContextOption.RECONNECT_RETRY_INTERVAL));
        cf.setRetryIntervalMultiplier(options.getDouble(CreateContextOption.RECONNECT_RETRY_INTERVAL_MULTIPLIER));
        cf.setMaxRetryInterval(options.getLong(CreateContextOption.RECONNECT_MAX_RETRY_INTERVAL));

        return cf;
    }

    protected javax.jms.Topic createTopic(String name) throws Exception {
        this.server
                .getJMSServerManager()
                .createTopic(false, name, DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC));

        return lookupTopic(name);
    }

    protected javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception {
        this.server
                .getJMSServerManager()
                .createQueue(false, name, selector, durable,
                             DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE));

        return lookupQueue(name);
    }

    protected javax.jms.Topic lookupTopic(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.getJMSServerManager().getBindingsOnTopic(name)));
        }
        jndiNames.add(name);
        jndiNames.add(DestinationUtil.jmsName(name, JMSDestination.Type.TOPIC));
        jndiNames.add(DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC));

        return (javax.jms.Topic)lookupJNDI(jndiNames);
    }

    protected javax.jms.Queue lookupQueue(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.getJMSServerManager().getBindingsOnQueue(name)));
        }
        jndiNames.add(name);
        jndiNames.add(DestinationUtil.jmsName(name, JMSDestination.Type.QUEUE));
        jndiNames.add(DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE));

        return (javax.jms.Queue)lookupJNDI(jndiNames);
    }

    protected void destroyQueue(String name) throws Exception {
        this.jmsServerManager().destroyQueue(name, true);
    }

    protected void destroyTopic(String name) throws Exception {
        this.jmsServerManager().destroyTopic(name, true);
    }

    protected Object lookupJNDI(String jndiName) {
        return server.getRegistry().lookup(jndiName);
    }

    private final String name;
    private final Options<CreateOption> options;
    private boolean started = false;
    private EmbeddedJMS server;

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.messaging.artemis");
}
