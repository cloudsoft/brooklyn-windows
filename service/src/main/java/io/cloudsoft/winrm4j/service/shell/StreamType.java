
package io.cloudsoft.winrm4j.service.shell;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StreamType", propOrder = {
    "value"
})
public class StreamType {

    @XmlValue
    protected byte[] value;
    @XmlAttribute(name = "Name", required = true)
    protected String name;
    @XmlAttribute(name = "CommandId")
    protected String commandId;
    @XmlAttribute(name = "End")
    protected Boolean end;
    @XmlAttribute(name = "Unit")
    @XmlSchemaType(name = "anyURI")
    protected String unit;
    @XmlAttribute(name = "EndUnit")
    protected Boolean endUnit;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the commandId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * Sets the value of the commandId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCommandId(String value) {
        this.commandId = value;
    }

    /**
     * Gets the value of the end property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEnd() {
        return end;
    }

    /**
     * Sets the value of the end property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnd(Boolean value) {
        this.end = value;
    }

    /**
     * Gets the value of the unit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     * Gets the value of the endUnit property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEndUnit() {
        return endUnit;
    }

    /**
     * Sets the value of the endUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEndUnit(Boolean value) {
        this.endUnit = value;
    }

}
