
package io.cloudsoft.winrm4j.service.shell;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Receive", propOrder = {
    "desiredStream"
})
public class Receive {

    @XmlElement(name = "DesiredStream", required = true)
    protected DesiredStreamType desiredStream;

    /**
     * Gets the value of the desiredStream property.
     * 
     * @return
     *     possible object is
     *     {@link DesiredStreamType }
     *     
     */
    public DesiredStreamType getDesiredStream() {
        return desiredStream;
    }

    /**
     * Sets the value of the desiredStream property.
     * 
     * @param value
     *     allowed object is
     *     {@link DesiredStreamType }
     *     
     */
    public void setDesiredStream(DesiredStreamType value) {
        this.desiredStream = value;
    }

}
