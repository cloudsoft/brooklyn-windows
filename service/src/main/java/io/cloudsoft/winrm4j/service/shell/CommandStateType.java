
package io.cloudsoft.winrm4j.service.shell;

import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommandStateType", propOrder = {
    "exitCode"
})
public class CommandStateType {

    @XmlElement(name = "ExitCode", namespace = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell")
    protected BigInteger exitCode;
    @XmlAttribute(name = "CommandId", required = true)
    protected String commandId;
    @XmlAttribute(name = "State")
    protected String state;

    /**
     * Gets the value of the exitCode property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getExitCode() {
        return exitCode;
    }

    /**
     * Sets the value of the exitCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setExitCode(BigInteger value) {
        this.exitCode = value;
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
     * Gets the value of the state property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setState(String value) {
        this.state = value;
    }

}
