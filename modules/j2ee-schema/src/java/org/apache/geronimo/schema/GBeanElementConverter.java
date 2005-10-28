/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.geronimo.schema;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;

/**
 * @version $Rev:  $ $Date:  $
 */
public class GBeanElementConverter implements ElementConverter {

    private static final String GERONIMO_SERVICE_NAMESPACE = "http://geronimo.apache.org/xml/ns/deployment-1.0";


    public void convertElement(XmlCursor cursor, XmlCursor end) {
        end.toCursor(cursor);
        end.toEndToken();
        while (cursor.hasNextToken() && cursor.isLeftOf(end)) {
            if (cursor.isStart()) {
                if (GERONIMO_SERVICE_NAMESPACE.equals(cursor.getName().getNamespaceURI())) {
                    //already has correct schema, exit
                    return;
                }
                String localPart = cursor.getName().getLocalPart();
                if (localPart.equals("xml-attribute") || localPart.equals("xml-reference")) {
                    cursor.toEndToken();
                } else {
                    cursor.setName(new QName(GERONIMO_SERVICE_NAMESPACE, localPart));
                }
            }
            //this should not break because the xml-* elements are never top level.
            cursor.toNextToken();
        }
    }
}
