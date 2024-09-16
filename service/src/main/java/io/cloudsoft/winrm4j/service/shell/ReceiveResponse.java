
package io.cloudsoft.winrm4j.service.shell;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReceiveResponse", namespace="http://schemas.microsoft.com/wbem/wsman/1/windows/shell", propOrder = {
    "stream",
    "commandState"
})
public class ReceiveResponse {

    @XmlElement(name = "Stream", required = true)
    protected List<StreamType> stream;
    @XmlElement(name = "CommandState")
    protected CommandStateType commandState;
    @XmlAttribute(name = "SequenceID", namespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger sequenceID;

    /**
     * Gets the value of the stream property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the stream property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStream().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StreamType }
     * 
     * 
     */
    public List<StreamType> getStream() {
        if (stream == null) {
            stream = new ArrayList<StreamType>();
        }
        return this.stream;
    }

    /**
     * Gets the value of the commandState property.
     * 
     * @return
     *     possible object is
     *     {@link CommandStateType }
     *     
     */
    public CommandStateType getCommandState() {
        return commandState;
    }

    /**
     * Sets the value of the commandState property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommandStateType }
     *     
     */
    public void setCommandState(CommandStateType value) {
        this.commandState = value;
    }

    /**
     * Gets the value of the sequenceID property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSequenceID() {
        return sequenceID;
    }

    /**
     * Sets the value of the sequenceID property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSequenceID(BigInteger value) {
        this.sequenceID = value;
    }

}
