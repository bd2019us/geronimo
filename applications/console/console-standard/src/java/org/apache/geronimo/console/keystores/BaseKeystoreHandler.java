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
package org.apache.geronimo.console.keystores;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.MultiPageAbstractHandler;
import org.apache.geronimo.console.MultiPageModel;
import org.apache.geronimo.management.geronimo.KeystoreInstance;
import org.apache.geronimo.management.geronimo.KeystoreIsLocked;
import org.apache.geronimo.util.CertificateUtil;

/**
 * The base class for all handlers for this portlet
 *
 * @version $Rev$ $Date$
 */
public abstract class BaseKeystoreHandler extends MultiPageAbstractHandler {
    private final static Log log = LogFactory.getLog(BaseKeystoreHandler.class);
    protected static final String KEYSTORE_DATA_PREFIX="org.apache.geronimo.keystore.";
    protected static final String LIST_MODE = "list";
    protected static final String UNLOCK_KEYSTORE_FOR_EDITING = "unlockEdit";
    protected static final String UNLOCK_KEYSTORE_FOR_USAGE = "unlockKeystore";
    protected static final String UNLOCK_KEY = "unlockKey";
    protected static final String LOCK_KEYSTORE_FOR_EDITING = "lockEdit";
    protected static final String LOCK_KEYSTORE_FOR_USAGE = "lockKeystore";
    protected static final String CREATE_KEYSTORE = "createKeystore";
    protected static final String VIEW_KEYSTORE = "viewKeystore";
    protected static final String UPLOAD_CERTIFICATE = "uploadCertificate";
    protected static final String CONFIRM_CERTIFICATE = "confirmCertificate";
    protected static final String CONFIGURE_KEY = "configureKey";
    protected static final String CONFIRM_KEY = "confirmKey";
    protected static final String CERTIFICATE_DETAILS = "certificateDetails";
    protected static final String GENERATE_CSR = "generateCSR";
    protected static final String IMPORT_CA_REPLY = "importCAReply";
    protected static final String DELETE_ENTRY = "deleteEntry";
    

    protected BaseKeystoreHandler(String mode, String viewName) {
        super(mode, viewName);
    }

    public final static class KeystoreModel implements MultiPageModel {
        public KeystoreModel(PortletRequest request) {
        }

        public void save(ActionResponse response, PortletSession session) {
        }
    }

    public final static class KeystoreData implements Serializable {
        private transient KeystoreInstance instance;
        private char[] password;
        private String[] certificates;
        private String[] keys;
        private Map fingerprints;
        private Map keyPasswords;

        public KeystoreInstance getInstance() {
            return instance;
        }

        public void setInstance(KeystoreInstance instance) {
            this.instance = instance;
        }

        public void setPassword(char[] password) {
            this.password = password;
            if(password == null) { // If locking, clear all saved data
                certificates = null;
                keys = null;
                fingerprints = null;
                keyPasswords = null;
            }
        }

        public boolean isLocked() {
            return password == null;
        }

        public String[] getCertificates() {
            return certificates;
        }

        public void setCertificates(String[] certificates) {
            this.certificates = certificates;
        }

        public String[] getKeys() {
            return keys;
        }

        public void setKeys(String[] keys) {
            this.keys = keys;
        }

        public Map getFingerprints() {
            if(fingerprints == null) {
                fingerprints = new HashMap();
                for (int i = 0; i < certificates.length; i++) {
                    String alias = certificates[i];
                    try {
                        fingerprints.put(alias, CertificateUtil.generateFingerprint(instance.getCertificate(alias, password), "MD5"));
                    } catch (Exception e) {
                        log.error("Unable to generate certificate fingerprint", e);
                    }
                }
                for (int i = 0; i < keys.length; i++) {
                    String alias = keys[i];
                    try {
                        fingerprints.put(alias, CertificateUtil.generateFingerprint(instance.getCertificate(alias, password), "MD5"));
                    } catch (Exception e) {
                        log.error("Unable to generate certificate fingerprint", e);
                    }
                }
            }
            return fingerprints;
        }

        public boolean importTrustCert(String fileName, String alias) throws FileNotFoundException, CertificateException {
            // Uploading certificate using a disk file fails on Windows.  Certificate text is used instead.
            //InputStream is = new FileInputStream(fileName);
            InputStream is = new ByteArrayInputStream(fileName.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection certs = cf.generateCertificates(is);
            X509Certificate cert = (X509Certificate) certs.iterator().next();
            boolean result = instance.importTrustCertificate(cert, alias, password);
            if(result) {
                String[] update = new String[certificates.length+1];
                System.arraycopy(certificates, 0, update, 0, certificates.length);
                update[certificates.length] = alias;
                certificates = update;
                if(fingerprints == null) {
                    getFingerprints();
                }
                try {
                    fingerprints.put(alias, CertificateUtil.generateFingerprint(instance.getCertificate(alias, password), "MD5"));
                } catch (Exception e) {
                    log.error("Unable to generate certificate fingerprint", e);
                }
            }
            return result;
        }

        public boolean createKeyPair(String alias, String keyPassword, String keyAlgorithm, int keySize,
                                     String signatureAlgorithm, int validity, String commonName, String orgUnit,
                                     String organization, String locality, String state, String country) {
            boolean result = instance.generateKeyPair(alias, password, keyPassword.toCharArray(), keyAlgorithm, keySize,
                                     signatureAlgorithm, validity, commonName, orgUnit, organization, locality, state, country);
            if(result) {
                String[] update = new String[keys.length+1];
                System.arraycopy(keys, 0, update, 0, keys.length);
                update[keys.length] = alias;
                keys = update;
                if(fingerprints == null) {
                    getFingerprints();
                }
                try {
                    fingerprints.put(alias, CertificateUtil.generateFingerprint(instance.getCertificate(alias, password), "MD5"));
                } catch (Exception e) {
                    log.error("Unable to generate certificate fingerprint", e);
                }
            }
            return result;
        }

        public Certificate getCertificate(String alias) {
            return instance.getCertificate(alias, password);
        }

        public void unlockPrivateKey(String alias, char[] keyPassword) throws KeystoreIsLocked {
            if(keyPasswords == null) {
                keyPasswords = new HashMap();
            }
            
            if(instance.unlockPrivateKey(alias, keyPassword)) {
                keyPasswords.put(alias, keyPassword);
            }
        }

        public void deleteEntry(String alias) {
            for(int i = 0; i < keys.length; ++i) {
                if(keys[i].equals(alias)) {
                    String[] temp = new String[keys.length-1];
                    for(int j = 0; j < i; ++j) {
                        temp[j] = keys[j];
                    }
                    for(int j = i+1; j < keys.length; ++j) {
                        temp[j-1] = keys[j];
                    }
                    keys = temp;
                    break;
                }
            }

            for(int i = 0; i < certificates.length; ++i) {
                if(certificates[i].equals(alias)) {
                    String[] temp = new String[certificates.length-1];
                    for(int j = 0; j < i; ++j) {
                        temp[j] = certificates[j];
                    }
                    for(int j = i+1; j < certificates.length; ++j) {
                        temp[j-1] = certificates[j];
                    }
                    certificates = temp;
                    break;
                }
            }
            instance.deleteEntry(alias);
            if(keyPasswords != null)
                keyPasswords.remove(alias);
            if(fingerprints != null)
                fingerprints.remove(alias);
        }

		public void importPKCS7Certificate(String alias, String pkcs7cert) {
			try {
				instance.importPKCS7Certificate(alias, pkcs7cert);
				fingerprints.put(alias, CertificateUtil.generateFingerprint(instance.getCertificate(alias, password), "MD5"));
			} catch (Exception e) {
				log.error("Error importing CA reply", e);
			}
		}
    }
}
