/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.messaging.remotenode.network;

import java.io.IOException;

import org.apache.geronimo.messaging.CommunicationException;
import org.apache.geronimo.messaging.NodeInfo;
import org.apache.geronimo.messaging.io.IOContext;
import org.apache.geronimo.messaging.remotenode.MessagingTransportFactory;
import org.apache.geronimo.messaging.remotenode.RemoteNode;
import org.apache.geronimo.messaging.remotenode.RemoteNodeConnection;

/**
 * 
 * @version $Revision: 1.3 $ $Date: 2004/06/24 23:39:03 $
 */
public class RemoteNodeJoiner
    extends AbstractRemoteNode
    implements RemoteNode
{

    private final MessagingTransportFactory connFactory;
    
    public RemoteNodeJoiner(NodeInfo aNodeInfo, IOContext anIOContext,
        MessagingTransportFactory aFactory) {
        super(aNodeInfo, anIOContext);
        if ( null == aFactory ) {
            throw new IllegalArgumentException("Factory is required.");
        }
        connFactory = aFactory;
    }

    public RemoteNodeConnection newConnection()
        throws IOException, CommunicationException {
        return connFactory.factoryRemoteNodeConnection(nodeInfo, ioContext);
    }
    
}
