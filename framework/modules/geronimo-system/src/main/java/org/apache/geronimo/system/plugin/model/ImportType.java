//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.3-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.04.08 at 05:10:24 PM PDT 
//


package org.apache.geronimo.system.plugin.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;


/**
 * <p>Java class for importType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="importType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="classes"/>
 *     &lt;enumeration value="services"/>
 *     &lt;enumeration value="all"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum ImportType {

    @XmlEnumValue("classes")
    CLASSES("classes"),
    @XmlEnumValue("services")
    SERVICES("services"),
    @XmlEnumValue("all")
    ALL("all");
    private final String value;

    ImportType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ImportType fromValue(String v) {
        for (ImportType c: ImportType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}