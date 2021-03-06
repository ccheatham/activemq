/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.pool;

import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.Connection;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.RegionBroker;
import org.apache.activemq.test.TestSupport;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Revision: 1.1 $
 */
public class PooledConnectionFactoryWithTemporaryDestinationsTest extends TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(PooledConnectionFactoryWithTemporaryDestinationsTest.class);

    private BrokerService broker;
    private ActiveMQConnectionFactory factory;
    private PooledConnectionFactory pooledFactory;

    protected void setUp() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        TransportConnector connector = broker.addConnector("tcp://localhost:0");
        broker.start();
        factory = new ActiveMQConnectionFactory("mock:" + connector.getConnectUri() + "?closeAsync=false");
        pooledFactory = new PooledConnectionFactory(factory);
    }

    protected void tearDown() throws Exception {
        broker.stop();
    }

    public void testTemporaryQueueLeakAfterConnectionClose() throws Exception {
        Connection pooledConnection = null;
        Session session = null;
        Queue tempQueue = null;
        for (int i = 0; i < 2; i++) {
            pooledConnection = pooledFactory.createConnection();
            session = pooledConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            tempQueue = session.createTemporaryQueue();
            LOG.info("Created queue named: " + tempQueue.getQueueName());
            pooledConnection.close();
        }

        assertEquals(0, countBrokerTemporaryQueues());
    }

    public void testTemporaryTopicLeakAfterConnectionClose() throws Exception {
        Connection pooledConnection = null;
        Session session = null;
        Topic tempTopic = null;
        for (int i = 0; i < 2; i++) {
            pooledConnection = pooledFactory.createConnection();
            session = pooledConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            tempTopic = session.createTemporaryTopic();
            LOG.info("Created topic named: " + tempTopic.getTopicName());
            pooledConnection.close();
        }

        assertEquals(0, countBrokerTemporaryTopics());
    }

    private int countBrokerTemporaryQueues() throws Exception {
        return ((RegionBroker) broker.getRegionBroker()).getTempQueueRegion().getDestinationMap().size();
    }

    private int countBrokerTemporaryTopics() throws Exception {
        return ((RegionBroker) broker.getRegionBroker()).getTempTopicRegion().getDestinationMap().size();
    }
}
