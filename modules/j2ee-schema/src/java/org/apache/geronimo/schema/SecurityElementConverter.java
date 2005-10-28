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
public class SecurityElementConverter implements ElementConverter {

    public static final String GERONIMO_SECURITY_NAMESPACE = "http://geronimo.apache.org/xml/ns/security-1.1";
    private static final QName PRINCIPAL_QNAME = new QName(GERONIMO_SECURITY_NAMESPACE, "principal");
    private static final QName REALM_NAME_QNAME = new QName("realm-name");

    public void convertElement(XmlCursor cursor, XmlCursor end) {
        cursor.push();
        end.toCursor(cursor);
        end.toEndToken();
        while (cursor.hasNextToken() && cursor.isLeftOf(end)) {
            if (cursor.isStart()) {
                if (GERONIMO_SECURITY_NAMESPACE.equals(cursor.getName().getNamespaceURI())) {
                    break;
                }
                cursor.setName(new QName(GERONIMO_SECURITY_NAMESPACE, cursor.getName().getLocalPart()));

            }
            cursor.toNextToken();
        }
        cursor.pop();
        while (cursor.hasNextToken() && cursor.isLeftOf(end)) {
            if (cursor.isStart()) {
                String localPart = cursor.getName().getLocalPart();
                if (localPart.equals("realm")) {
                    XmlCursor source = cursor.newCursor();
                    cursor.push();
                    cursor.toEndToken();
                    cursor.toNextToken();
                    try {
                        if (source.toChild(PRINCIPAL_QNAME)) {
                            do {
                                source.copyXmlContents(cursor);
                            } while (source.toNextSibling(PRINCIPAL_QNAME));
                        }

                    } finally {
                        source.dispose();
                    }
                    cursor.pop();
                    cursor.removeXml();
                } else if (localPart.equals("default-principal")) {
                    cursor.removeAttribute(REALM_NAME_QNAME);
                }
            }
            cursor.toNextToken();
        }

    }
}
